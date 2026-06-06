package com.termux.app;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import com.termux.shared.termux.TermuxConstants;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.nio.charset.StandardCharsets;

@RunWith(RobolectricTestRunner.class)
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
    public void patchLoginScriptForForkGuardsLdPreloadExportSoShimIsPreserved() {
        String prefix = TermuxConstants.TERMUX_PREFIX_DIR_PATH;
        String loginScript = "#!" + FORK_DATA_DIR + "/files/usr/bin/sh\n"
            + "if [ -f \"" + prefix + "/lib/libtermux-exec-ld-preload.so\" ]; then\n"
            + "\texport LD_PRELOAD=\"" + prefix + "/lib/libtermux-exec-ld-preload.so\"\n"
            + "\t$SHELL -c \"coreutils --coreutils-prog=true\" > /dev/null 2>&1 || unset LD_PRELOAD\n"
            + "elif [ -f \"" + prefix + "/lib/libtermux-exec.so\" ]; then\n"
            + "\texport LD_PRELOAD=\"" + prefix + "/lib/libtermux-exec.so\"\n"
            + "\t$SHELL -c \"coreutils --coreutils-prog=true\" > /dev/null 2>&1 || unset LD_PRELOAD\n"
            + "fi\n"
            + "if [ -n \"$TERM\" ]; then\n"
            + "\texec \"$SHELL\" -l \"$@\"\n"
            + "fi\n";

        byte[] patched = TermuxInstaller.patchLoginScriptForFork(loginScript.getBytes(StandardCharsets.UTF_8));
        String patchedContent = new String(patched, StandardCharsets.UTF_8);

        assertTrue(patchedContent.contains(TermuxInstaller.LOGIN_PRESERVE_LD_PRELOAD_TOKEN));
        assertTrue(patchedContent.contains("[ -n \"$LD_PRELOAD\" ] || export LD_PRELOAD=\""
            + prefix + "/lib/libtermux-exec-ld-preload.so\""));
        assertTrue(patchedContent.contains("[ -n \"$LD_PRELOAD\" ] || export LD_PRELOAD=\""
            + prefix + "/lib/libtermux-exec.so\""));
        assertFalse(patchedContent.contains("\n\texport LD_PRELOAD=\"" + prefix + "/lib/libtermux-exec-ld-preload.so\"\n"));
        assertFalse(patchedContent.contains("\n\texport LD_PRELOAD=\"" + prefix + "/lib/libtermux-exec.so\"\n"));
    }

    @Test
    public void patchLoginScriptForForkIsIdempotentForLdPreloadGuard() {
        String prefix = TermuxConstants.TERMUX_PREFIX_DIR_PATH;
        String loginScript = "#!" + FORK_DATA_DIR + "/files/usr/bin/sh\n"
            + "if [ -f \"" + prefix + "/lib/libtermux-exec-ld-preload.so\" ]; then\n"
            + "\texport LD_PRELOAD=\"" + prefix + "/lib/libtermux-exec-ld-preload.so\"\n"
            + "fi\n";

        byte[] firstPass = TermuxInstaller.patchLoginScriptForFork(loginScript.getBytes(StandardCharsets.UTF_8));
        byte[] secondPass = TermuxInstaller.patchLoginScriptForFork(firstPass);

        assertArrayEquals(firstPass, secondPass);
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

    @Test
    public void isDpkgMaintainerScriptEntryAcceptsInfoMaintainerSuffixes() {
        assertTrue(TermuxInstaller.isDpkgMaintainerScriptEntry("var/lib/dpkg/info/coreutils.postinst"));
        assertTrue(TermuxInstaller.isDpkgMaintainerScriptEntry("var/lib/dpkg/info/coreutils.prerm"));
        assertTrue(TermuxInstaller.isDpkgMaintainerScriptEntry("var/lib/dpkg/info/coreutils.preinst"));
        assertTrue(TermuxInstaller.isDpkgMaintainerScriptEntry("var/lib/dpkg/info/coreutils.postrm"));
    }

    @Test
    public void isDpkgMaintainerScriptEntryRejectsOtherDpkgInfoFiles() {
        assertFalse(TermuxInstaller.isDpkgMaintainerScriptEntry("var/lib/dpkg/info/coreutils.list"));
        assertFalse(TermuxInstaller.isDpkgMaintainerScriptEntry("var/lib/dpkg/info/coreutils.md5sums"));
    }

    @Test
    public void isDpkgMaintainerScriptEntryRejectsPathsOutsideDpkgInfo() {
        assertFalse(TermuxInstaller.isDpkgMaintainerScriptEntry("etc/profile"));
        assertFalse(TermuxInstaller.isDpkgMaintainerScriptEntry("var/lib/dpkg/info"));
        assertFalse(TermuxInstaller.isDpkgMaintainerScriptEntry("var/lib/other/info/coreutils.postinst"));
    }

    @Test
    public void patchUpdateAlternativesInvocationsInjectsAltdirAndAdmindirForInstallInvocation() {
        String script = "if [ -x \"" + FORK_DATA_DIR + "/files/usr/bin/update-alternatives\" ]; then\n"
            + "    # pager\n"
            + "    update-alternatives \\\n"
            + "      --install \"" + FORK_DATA_DIR + "/files/usr/bin/pager\" \"pager\" \"" + FORK_DATA_DIR + "/files/usr/libexec/coreutils/cat\" 1 \\\n"
            + "      --slave \"" + FORK_DATA_DIR + "/files/usr/share/man/man1/pager.1.gz\" \"pager.1.gz\" \"" + FORK_DATA_DIR + "/files/usr/share/man/man1/cat.1.gz\"\n"
            + "fi\n";

        byte[] patched = TermuxInstaller.patchUpdateAlternativesInvocations(script.getBytes(StandardCharsets.UTF_8));
        String patchedContent = new String(patched, StandardCharsets.UTF_8);

        assertTrue(patchedContent.contains("--altdir \"" + TermuxConstants.TERMUX_PREFIX_DIR_PATH + "/etc/alternatives\""));
        assertTrue(patchedContent.contains("--admindir \"" + TermuxConstants.TERMUX_PREFIX_DIR_PATH + "/var/lib/dpkg/alternatives\""));
        assertTrue(patchedContent.contains(TermuxInstaller.UPDATE_ALTERNATIVES_FORK_FLAGS_TOKEN));
        assertTrue(patchedContent.contains("\\\n      --install"));
    }

    @Test
    public void patchUpdateAlternativesInvocationsInjectsAltdirAndAdmindirForRemoveInvocation() {
        String script = "if [ -x \"" + FORK_DATA_DIR + "/files/usr/bin/update-alternatives\" ]; then\n"
            + "    update-alternatives --remove \"pager\" \"" + FORK_DATA_DIR + "/files/usr/libexec/coreutils/cat\"\n"
            + "fi\n";

        byte[] patched = TermuxInstaller.patchUpdateAlternativesInvocations(script.getBytes(StandardCharsets.UTF_8));
        String patchedContent = new String(patched, StandardCharsets.UTF_8);

        assertTrue(patchedContent.contains("--altdir \"" + TermuxConstants.TERMUX_PREFIX_DIR_PATH + "/etc/alternatives\""));
        assertTrue(patchedContent.contains("--admindir \"" + TermuxConstants.TERMUX_PREFIX_DIR_PATH + "/var/lib/dpkg/alternatives\""));
        assertTrue(patchedContent.contains(TermuxInstaller.UPDATE_ALTERNATIVES_FORK_FLAGS_TOKEN));
        assertTrue(patchedContent.contains("--remove \"pager\""));
    }

    @Test
    public void patchUpdateAlternativesInvocationsReturnsSameBytesWhenNeitherInvocationPresent() {
        byte[] script = "#!/bin/sh\n# Automatically added by termux_step_update_alternatives\necho hello\n".getBytes(StandardCharsets.UTF_8);

        assertSame(script, TermuxInstaller.patchUpdateAlternativesInvocations(script));
    }

    @Test
    public void patchUpdateAlternativesInvocationsKeepsInstallFlagOnContinuedLineNotBehindComment() {
        String script = "if [ -x \"" + FORK_DATA_DIR + "/files/usr/bin/update-alternatives\" ]; then\n"
            + "    update-alternatives \\\n"
            + "      --install \"" + FORK_DATA_DIR + "/files/usr/bin/pager\" \"pager\" \"" + FORK_DATA_DIR + "/files/usr/libexec/coreutils/cat\" 1\n"
            + "fi\n";

        String patchedContent = new String(
            TermuxInstaller.patchUpdateAlternativesInvocations(script.getBytes(StandardCharsets.UTF_8)),
            StandardCharsets.UTF_8);

        for (String line : patchedContent.split("\n", -1)) {
            int hashIndex = line.indexOf('#');
            int backslashAtEnd = line.endsWith("\\") ? line.length() - 1 : -1;
            assertFalse(
                "Line continues with backslash after a shell comment, breaking multi-line invocation: " + line,
                hashIndex >= 0 && backslashAtEnd > hashIndex);
        }
    }

    @Test
    public void patchUpdateAlternativesInvocationsKeepsRemoveFlagOutsideOfComment() {
        String script = "    update-alternatives --remove \"pager\" \"" + FORK_DATA_DIR + "/files/usr/libexec/coreutils/cat\"\n";

        String patchedContent = new String(
            TermuxInstaller.patchUpdateAlternativesInvocations(script.getBytes(StandardCharsets.UTF_8)),
            StandardCharsets.UTF_8);

        for (String line : patchedContent.split("\n", -1)) {
            int hashIndex = line.indexOf('#');
            int removeIndex = line.indexOf("--remove");
            assertFalse(
                "Line places --remove after a shell comment, disabling the flag: " + line,
                hashIndex >= 0 && removeIndex > hashIndex);
        }
    }

    @org.junit.Rule
    public org.junit.rules.TemporaryFolder forkEaccesShimTempFolder = new org.junit.rules.TemporaryFolder();

    @Test
    public void stageForkEaccesShimCopiesSourceLibraryToTargetWhenTargetMissing() throws java.io.IOException {
        java.io.File sourceDir = forkEaccesShimTempFolder.newFolder("nativeLibs");
        java.io.File sourceLib = new java.io.File(sourceDir, TermuxInstaller.FORK_EACCES_SHIM_LIBRARY_NAME);
        byte[] payload = new byte[]{0x7F, 'E', 'L', 'F', 0x42, 0x43};
        try (java.io.FileOutputStream out = new java.io.FileOutputStream(sourceLib)) {
            out.write(payload);
        }
        java.io.File targetDir = forkEaccesShimTempFolder.newFolder("prefixLib");

        TermuxInstaller.stageForkEaccesShim(sourceLib, targetDir);

        java.io.File targetLib = new java.io.File(targetDir, TermuxInstaller.FORK_EACCES_SHIM_LIBRARY_NAME);
        assertTrue(targetLib.isFile());
        byte[] copied = new byte[payload.length];
        try (java.io.FileInputStream in = new java.io.FileInputStream(targetLib)) {
            int read = in.read(copied);
            assertEquals(payload.length, read);
        }
        assertArrayEquals(payload, copied);
    }

    @Test
    public void stageForkEaccesShimCreatesTargetDirectoryWhenAbsent() throws java.io.IOException {
        java.io.File sourceDir = forkEaccesShimTempFolder.newFolder("nativeLibs");
        java.io.File sourceLib = new java.io.File(sourceDir, TermuxInstaller.FORK_EACCES_SHIM_LIBRARY_NAME);
        try (java.io.FileOutputStream out = new java.io.FileOutputStream(sourceLib)) {
            out.write(new byte[]{1, 2, 3});
        }
        java.io.File rootDir = forkEaccesShimTempFolder.newFolder("root");
        java.io.File targetDir = new java.io.File(rootDir, "lib/usr/lib");

        TermuxInstaller.stageForkEaccesShim(sourceLib, targetDir);

        assertTrue(targetDir.isDirectory());
        assertTrue(new java.io.File(targetDir, TermuxInstaller.FORK_EACCES_SHIM_LIBRARY_NAME).isFile());
    }

    @Test
    public void stageForkEaccesShimSkipsCopyWhenTargetUpToDate() throws java.io.IOException {
        java.io.File sourceDir = forkEaccesShimTempFolder.newFolder("nativeLibs");
        java.io.File sourceLib = new java.io.File(sourceDir, TermuxInstaller.FORK_EACCES_SHIM_LIBRARY_NAME);
        byte[] payload = new byte[]{9, 9, 9};
        try (java.io.FileOutputStream out = new java.io.FileOutputStream(sourceLib)) {
            out.write(payload);
        }
        java.io.File targetDir = forkEaccesShimTempFolder.newFolder("prefixLib");
        java.io.File targetLib = new java.io.File(targetDir, TermuxInstaller.FORK_EACCES_SHIM_LIBRARY_NAME);
        try (java.io.FileOutputStream out = new java.io.FileOutputStream(targetLib)) {
            out.write(payload);
        }
        sourceLib.setLastModified(1_000_000L);
        targetLib.setLastModified(2_000_000L);

        TermuxInstaller.stageForkEaccesShim(sourceLib, targetDir);

        assertEquals(2_000_000L, targetLib.lastModified());
    }

    @Test
    public void stageForkEaccesShimDoesNothingWhenSourceLibraryMissing() throws java.io.IOException {
        java.io.File sourceDir = forkEaccesShimTempFolder.newFolder("nativeLibs");
        java.io.File missingSource = new java.io.File(sourceDir, TermuxInstaller.FORK_EACCES_SHIM_LIBRARY_NAME);
        java.io.File targetDir = forkEaccesShimTempFolder.newFolder("prefixLib");

        TermuxInstaller.stageForkEaccesShim(missingSource, targetDir);

        assertFalse(new java.io.File(targetDir, TermuxInstaller.FORK_EACCES_SHIM_LIBRARY_NAME).exists());
    }
}
