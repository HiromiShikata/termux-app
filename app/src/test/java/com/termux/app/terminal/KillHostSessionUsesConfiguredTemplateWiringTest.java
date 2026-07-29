package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class KillHostSessionUsesConfiguredTemplateWiringTest {

    private static final String CLIENT_RELATIVE_PATH =
        "src/main/java/com/termux/app/terminal/TermuxTerminalSessionActivityClient.java";

    private String readSource(String relativePath) throws IOException {
        Path moduleRelative = Paths.get(relativePath);
        if (Files.exists(moduleRelative)) {
            return new String(Files.readAllBytes(moduleRelative), StandardCharsets.UTF_8);
        }
        Path repoRelative = Paths.get("app").resolve(relativePath);
        return new String(Files.readAllBytes(repoRelative), StandardCharsets.UTF_8);
    }

    private String killHostSessionMethodBody() throws IOException {
        String source = readSource(CLIENT_RELATIVE_PATH);
        int methodIndex = source.indexOf("public void killHostSession(final TerminalSession sessionToKill) {");
        Assert.assertTrue("killHostSession(TerminalSession) must still exist as a stable public entry point",
            methodIndex >= 0);
        int methodEnd = source.indexOf("\n    }", methodIndex);
        Assert.assertTrue(methodEnd > methodIndex);
        return source.substring(methodIndex, methodEnd);
    }

    @Test
    public void killHostSessionMustNotWriteTheKillCommandIntoTheAttachedSessionAnyMore() throws IOException {
        String methodBody = killHostSessionMethodBody();
        Assert.assertFalse("the kill command must be routed out-of-band instead of being written into the "
                + "pty of the currently attached session",
            methodBody.contains("sessionToKill.write("));
    }

    @Test
    public void killHostSessionMustReadTheConfigurableKillSessionCommandTemplatePreference() throws IOException {
        String methodBody = killHostSessionMethodBody();
        Assert.assertTrue("killHostSession must consult the configurable kill_session_command template, "
                + "the same way addNewSessionApplyingAutosshConfig consults the autossh_command template",
            methodBody.contains("getKillSessionCommand()"));
    }

    @Test
    public void killHostSessionMustExecuteTheComposedCommandAsANewShellSessionLikeTheConnectTemplateDoes()
            throws IOException {
        String methodBody = killHostSessionMethodBody();
        Assert.assertTrue("the composed kill command must be executed as a new short-lived session via "
                + "TermuxService.createTermuxSession(...), mirroring addNewAutosshSession",
            methodBody.contains("createTermuxSession("));
        Assert.assertTrue("the new session must be started as \"sh -c <command>\", mirroring addNewAutosshSession",
            methodBody.contains("\"-c\""));
    }
}
