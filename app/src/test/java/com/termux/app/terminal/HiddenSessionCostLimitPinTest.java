package com.termux.app.terminal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.termux.app.sessiondefinition.SessionDefinitionCapCountPlanner;
import com.termux.app.terminal.session.AlwaysPresentSessionPlanner;
import com.termux.terminal.TerminalSession;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@RunWith(RobolectricTestRunner.class)
public class HiddenSessionCostLimitPinTest {

    private static final int NO_SHELL_PROCESS = -1;
    private static final String HIDDEN_SESSION_NAME = "hidden-agent";
    private static final String VISIBLE_SESSION_NAME = "visible-agent";

    @Test
    public void theHideReleaseLeavesTheHiddenSessionWithNoTerminalEmulatorAndNoShellProcess() throws Exception {
        TerminalSession hiddenSession = new TerminalSession(null, null, null, null, null, null);
        hiddenSession.mSessionName = HIDDEN_SESSION_NAME;
        setShellProcessId(hiddenSession, NO_SHELL_PROCESS);

        hiddenSession.releaseRuntimeResources();

        assertNull("a session the owner hid must hold no terminal emulator, so drawing a row for it may never "
            + "bring one back", hiddenSession.getEmulator());
        assertFalse("a session the owner hid must hold no shell process, so drawing a row for it may never "
            + "bring one back", hiddenSession.isRunning());
    }

    @Test
    public void theAlwaysPresentRestoreStillPlansNothingForAHiddenSessionName() {
        List<String> alwaysPresentSessionNames = Arrays.asList(HIDDEN_SESSION_NAME, VISIBLE_SESSION_NAME);
        Set<String> hiddenSessionNames = namesInOrder(HIDDEN_SESSION_NAME);

        List<String> missingSessionNames = new AlwaysPresentSessionPlanner().planMissingSessionNames(
            alwaysPresentSessionNames, Collections.emptyList(), hiddenSessionNames);

        assertEquals("the always-present restore must plan exactly the one name the owner did not hide, so a "
            + "row for a hidden always-present session never turns into a recreated session",
            Collections.singletonList(VISIBLE_SESSION_NAME), missingSessionNames);
        assertFalse("the always-present restore must plan nothing for a name the owner hid",
            missingSessionNames.contains(HIDDEN_SESSION_NAME));
    }

    @Test
    public void aHiddenSessionStillConsumesNoSessionCapSlot() {
        List<SessionDefinitionCapCountPlanner.CountedSession> countedSessions = Arrays.asList(
            new SessionDefinitionCapCountPlanner.CountedSession(HIDDEN_SESSION_NAME, true),
            new SessionDefinitionCapCountPlanner.CountedSession(VISIBLE_SESSION_NAME, true));

        int countedTowardCap = new SessionDefinitionCapCountPlanner().countSessionsTowardCap(
            countedSessions, namesInOrder(HIDDEN_SESSION_NAME));

        assertEquals("a hidden session must take no slot under the session cap, so the visible sessions keep "
            + "every slot the owner configured", 1, countedTowardCap);
        assertTrue("the cap count must stay below the count of all sessions while one of them is hidden",
            countedTowardCap < countedSessions.size());
    }

    private static Set<String> namesInOrder(String... names) {
        return new LinkedHashSet<>(Arrays.asList(names));
    }

    private static void setShellProcessId(TerminalSession session, int shellProcessId) throws Exception {
        Field shellPid = TerminalSession.class.getDeclaredField("mShellPid");
        shellPid.setAccessible(true);
        shellPid.setInt(session, shellProcessId);
    }
}
