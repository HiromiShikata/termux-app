package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ReloadReconnectsEveryDisplayedSessionTest {

    private static final String CLIENT_RELATIVE_PATH =
        "src/main/java/com/termux/app/terminal/TermuxTerminalSessionActivityClient.java";

    private String readClientSource() throws IOException {
        Path moduleRelative = Paths.get(CLIENT_RELATIVE_PATH);
        if (Files.exists(moduleRelative)) {
            return new String(Files.readAllBytes(moduleRelative), StandardCharsets.UTF_8);
        }
        Path repoRelative = Paths.get("app").resolve(CLIENT_RELATIVE_PATH);
        return new String(Files.readAllBytes(repoRelative), StandardCharsets.UTF_8);
    }

    private String methodBody(String source, String signature) {
        int methodIndex = source.indexOf(signature);
        Assert.assertTrue("method not found: " + signature, methodIndex >= 0);
        int methodEnd = source.indexOf("\n    }", methodIndex);
        Assert.assertTrue(methodEnd > methodIndex);
        return source.substring(methodIndex, methodEnd);
    }

    @Test
    public void reloadReconnectsTheDisplayedSetRatherThanOnlyTheTerminalSession() throws IOException {
        String source = readClientSource();
        String body = methodBody(source,
            "public void reconnectDeadDefinitionBackedSessionsThenForceRescanStatusline() {");

        Assert.assertTrue("the reload path must reconnect every displayed (non-hidden) dead session, "
                + "because the Load Sessions button closes the session list before reloading and the narrow "
                + "visible set then holds only the session shown in the terminal",
            body.contains("reconnectDeadDisplayedSessionsInBackground(displayedSessionNames())"));
        Assert.assertTrue("the reload path must not fall back to the narrow visible set",
            !body.contains("reconnectDeadDefinitionBackedSessionsInBackground()"));
    }

    @Test
    public void reloadKeepsForcingTheStatuslineRescanForEveryDisplayedSession() throws IOException {
        String source = readClientSource();
        String body = methodBody(source,
            "public void reconnectDeadDefinitionBackedSessionsThenForceRescanStatusline() {");

        Assert.assertTrue("the reload path must keep force-refreshing the statusline of every displayed row",
            body.contains("repopulateStatuslineTimesForDisplayedSessions(true)"));
        Assert.assertTrue("the reload path must keep tracking the reconnected session names for the "
                + "post-reconnect statusline rescan retry",
            body.contains("reconnectedSessionNames"));
    }

    @Test
    public void theDisplayedReconnectPathKeepsTheStaggeredSchedule() throws IOException {
        String source = readClientSource();
        String body = methodBody(source,
            "private List<String> reconnectDeadDefinitionBackedSessionsInBackground(@NonNull Set<String> reconnectableSessionNames) {");

        Assert.assertTrue("reconnecting a large displayed set must stay paced so the app is not frozen "
                + "by one reconnect per session at once",
            body.contains("mSessionReconnectPacer.enqueueSession(deadSession)"));
    }
}
