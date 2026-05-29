package com.termux.app;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import com.termux.shared.termux.TermuxConstants;

import org.junit.Test;

import java.nio.charset.StandardCharsets;

public class TermuxInstallerTest {

    private static final String FORK_DATA_DIR = TermuxConstants.TERMUX_INTERNAL_PRIVATE_APP_DATA_DIR_PATH;

    @Test
    public void patchPackagePathStringRewritesUpstreamPackagePathToForkPackagePath() {
        String content = "#!/data/data/com.termux/files/usr/bin/sh\n"
            + "for i in /data/data/com.termux/files/usr/etc/profile.d/*.sh; do .  $i; done\n";

        String patched = TermuxInstaller.patchPackagePathString(content);

        assertEquals("#!" + FORK_DATA_DIR + "/files/usr/bin/sh\n"
            + "for i in " + FORK_DATA_DIR + "/files/usr/etc/profile.d/*.sh; do .  $i; done\n", patched);
        assertFalse(patched.contains("/data/data/com.termux/"));
    }

    @Test
    public void patchPackagePathStringRewritesPathWithoutShebang() {
        String content = "if [ -x \"/data/data/com.termux/files/usr/libexec/termux/cnf\" ]; then :; fi\n";

        String patched = TermuxInstaller.patchPackagePathString(content);

        assertEquals("if [ -x \"" + FORK_DATA_DIR + "/files/usr/libexec/termux/cnf\" ]; then :; fi\n", patched);
    }

    @Test
    public void patchPackagePathStringLeavesForkPackagePathUnchanged() {
        String content = "#!" + FORK_DATA_DIR + "/files/usr/bin/sh\n";

        assertEquals(content, TermuxInstaller.patchPackagePathString(content));
    }

    @Test
    public void patchPackagePathStringLeavesContentWithoutDataDataPathUnchanged() {
        String content = "#!/system/bin/sh\necho hello\n";

        assertEquals(content, TermuxInstaller.patchPackagePathString(content));
    }

    @Test
    public void patchPackagePathsIfNeededReturnsSameInstanceWhenNothingToPatch() {
        byte[] fileBytes = "plain text with no termux path\n".getBytes(StandardCharsets.UTF_8);

        assertSame(fileBytes, TermuxInstaller.patchPackagePathsIfNeeded(fileBytes));
    }

    @Test
    public void patchPackagePathsIfNeededRewritesUpstreamPackagePath() {
        byte[] fileBytes = "#!/data/data/com.termux/files/usr/bin/sh\n".getBytes(StandardCharsets.UTF_8);

        byte[] patched = TermuxInstaller.patchPackagePathsIfNeeded(fileBytes);

        assertArrayEquals(("#!" + FORK_DATA_DIR + "/files/usr/bin/sh\n").getBytes(StandardCharsets.UTF_8), patched);
    }

    @Test
    public void isElfHeaderRecognizesElfMagic() {
        byte[] elf = new byte[]{0x7F, 'E', 'L', 'F', 0x02, 0x01};

        assertTrue(TermuxInstaller.isElfHeader(elf, elf.length));
    }

    @Test
    public void isElfHeaderRejectsShebangAndShortBuffers() {
        byte[] shebang = "#!/bin/sh".getBytes(StandardCharsets.UTF_8);

        assertFalse(TermuxInstaller.isElfHeader(shebang, shebang.length));
        assertFalse(TermuxInstaller.isElfHeader(new byte[]{0x7F, 'E', 'L'}, 3));
    }

    @Test
    public void patchLoginScriptForForkReplacesLoginShellExecWithInitFileForBash() {
        String loginScript = "#!/data/data/com.termux.hs.shortcut/files/usr/bin/sh\n"
            + "export SHELL=/data/data/com.termux.hs.shortcut/files/usr/bin/bash\n"
            + "if [ -n \"$TERM\" ]; then\n"
            + "\texec \"$SHELL\" -l \"$@\"\n"
            + "else\n"
            + "\texec \"$SHELL\" \"$@\"\n"
            + "fi\n";

        byte[] patched = TermuxInstaller.patchLoginScriptForFork(loginScript.getBytes(StandardCharsets.UTF_8));
        String patchedContent = new String(patched, StandardCharsets.UTF_8);

        assertTrue(patchedContent.contains("--init-file"));
        assertTrue(patchedContent.contains(TermuxConstants.TERMUX_PREFIX_DIR_PATH + "/etc/profile"));
        assertFalse(patchedContent.contains("if [ -n \"$TERM\" ]; then\n\texec \"$SHELL\" -l"));
        assertTrue(patchedContent.contains("${SHELL##*/}\" = bash"));
    }

    @Test
    public void patchLoginScriptForForkReturnsSameBytesWhenLoginShellExecLineAbsent() {
        byte[] noExecLine = "#!/bin/sh\nexport SHELL=/usr/bin/bash\n".getBytes(StandardCharsets.UTF_8);

        assertSame(noExecLine, TermuxInstaller.patchLoginScriptForFork(noExecLine));
    }

    @Test
    public void patchSecondStageScriptForForkReplacesDpkgVersionInvocationWithStatusFileLookup() {
        String secondStageScript = "if [ -d \"${TERMUX_PREFIX}/var/lib/dpkg/info\" ]; then\n"
            + "\tlocal dpkg_version\n"
            + "\n"
            + "\t" + TermuxInstaller.SECOND_STAGE_DPKG_VERSION_INVOCATION + "\n"
            + "\tif [[ ! \"$dpkg_version\" =~ ^[0-9].*$ ]]; then\n"
            + "\t\tlog_error \"Failed to find the 'dpkg' version\"\n"
            + "\t\treturn 1\n"
            + "\tfi\n"
            + "fi\n";

        byte[] patched = TermuxInstaller.patchSecondStageScriptForFork(secondStageScript.getBytes(StandardCharsets.UTF_8));
        String patchedContent = new String(patched, StandardCharsets.UTF_8);

        assertFalse(patchedContent.contains(TermuxInstaller.SECOND_STAGE_DPKG_VERSION_INVOCATION));
        assertTrue(patchedContent.contains("awk '/^Package: dpkg$/"));
        assertTrue(patchedContent.contains("${TERMUX_PREFIX}/var/lib/dpkg/status"));
        assertTrue(patchedContent.contains(TermuxInstaller.SECOND_STAGE_DPKG_VERSION_REPLACEMENT_TOKEN));
        assertTrue(patchedContent.contains("\tif [[ ! \"$dpkg_version\" =~ ^[0-9].*$ ]]; then"));
    }

    @Test
    public void patchSecondStageScriptForForkReturnsSameBytesWhenDpkgVersionInvocationAbsent() {
        byte[] noInvocation = "#!/data/data/com.termux/files/usr/bin/bash\necho hello\n".getBytes(StandardCharsets.UTF_8);

        assertSame(noInvocation, TermuxInstaller.patchSecondStageScriptForFork(noInvocation));
    }

    @Test
    public void patchSecondStageScriptForForkPreservesLeadingIndentation() {
        String secondStageScript = "\t\t\t" + TermuxInstaller.SECOND_STAGE_DPKG_VERSION_INVOCATION + "\n";

        byte[] patched = TermuxInstaller.patchSecondStageScriptForFork(secondStageScript.getBytes(StandardCharsets.UTF_8));
        String patchedContent = new String(patched, StandardCharsets.UTF_8);

        assertTrue(patchedContent.startsWith("\t\t\tdpkg_version="));
    }
}
