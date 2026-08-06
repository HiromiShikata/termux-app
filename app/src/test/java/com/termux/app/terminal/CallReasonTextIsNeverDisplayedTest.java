package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class CallReasonTextIsNeverDisplayedTest {

    private Path moduleResource(String relativePath) {
        Path moduleRelative = Paths.get(relativePath);
        if (Files.exists(moduleRelative)) {
            return moduleRelative;
        }
        return Paths.get("app").resolve(relativePath);
    }

    private String readModuleResource(String relativePath) throws IOException {
        return new String(Files.readAllBytes(moduleResource(relativePath)), StandardCharsets.UTF_8);
    }

    @Test
    public void theSessionInfoAreaLayoutHasNoAreaThatDisplaysACallReason() throws IOException {
        String layout = readModuleResource("src/main/res/layout/activity_termux.xml");

        Assert.assertFalse(layout.contains("session_pending_call_to_user_bar"));
        Assert.assertFalse(layout.contains("session_pending_call_to_user_scroll"));
        Assert.assertFalse(layout.contains("session_pending_call_to_user_text"));
        Assert.assertFalse(layout.contains("session_pending_call_to_user_scroll_button"));
    }

    @Test
    public void theCallReasonFooterSourcesAreDeleted() {
        Assert.assertFalse(Files.exists(moduleResource(
            "src/main/java/com/termux/app/terminal/PendingCallToUserFooterBinder.java")));
        Assert.assertFalse(Files.exists(moduleResource(
            "src/main/java/com/termux/app/terminal/PendingCallToUserFooterDecision.java")));
    }

    @Test
    public void theSessionInfoAreaBinderRendersNoCallReason() throws IOException {
        String binder = readModuleResource(
            "src/main/java/com/termux/app/terminal/SessionInfoBottomBarsBinder.java");

        Assert.assertFalse(binder.contains("PendingCallToUserFooter"));
        Assert.assertFalse(binder.contains("session_pending_call_to_user"));
        Assert.assertFalse(binder.contains("Reason"));
    }

    @Test
    public void theBottomSheetSessionRowRendersNoCallReason() throws IOException {
        String sessionListController = readModuleResource(
            "src/main/java/com/termux/app/terminal/TermuxSessionsListViewController.java");

        Assert.assertFalse(sessionListController.contains("getLastExplicitCallReason"));
        Assert.assertFalse(sessionListController.contains("explicitCallReason"));
        Assert.assertFalse(sessionListController.contains("EXPLICIT_CALL_REASON"));
    }

    @Test
    public void theSessionInfoBlockHasNoCallReasonLine() {
        for (SessionInfoLine line : SessionInfoLine.values()) {
            Assert.assertNotEquals("EXPLICIT_CALL_REASON", line.name());
        }
    }

    @Test
    public void aCalledSessionStillTurnsRedSoTheDotAloneReportsTheCall() {
        SessionNewActivityStore store = new SessionNewActivityStore();

        store.recordExplicitCall("worker", 1_000L, "deploy failed");

        Assert.assertEquals(SessionNewActivityTier.RED, store.tierFor("worker"));
        Assert.assertEquals(1, store.pendingCallToUserSessionCount());
    }

    @Test
    public void aCalledSessionStopsBeingRedOnceTheUserReplies() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordExplicitCall("worker", 1_000L, "deploy failed");

        store.recordUserInput("worker", 2_000L);

        Assert.assertEquals(SessionNewActivityTier.NONE, store.tierFor("worker"));
        Assert.assertEquals(0, store.pendingCallToUserSessionCount());
    }
}
