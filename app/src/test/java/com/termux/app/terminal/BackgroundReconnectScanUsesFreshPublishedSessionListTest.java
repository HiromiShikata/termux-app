package com.termux.app.terminal;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.content.Context;
import android.os.Looper;

import com.termux.app.TermuxActivity;
import com.termux.app.TermuxService;
import com.termux.app.sessiondefinition.SessionDefinitionDocumentFetcher;
import com.termux.app.sessiondefinition.SessionDefinitionEntry;
import com.termux.app.sessiondefinition.SessionDefinitionLoadResult;
import com.termux.app.sessiondefinition.SessionDefinitionLoader;
import com.termux.app.sessiondefinition.SessionDefinitionParser;
import com.termux.app.sessiondefinition.SessionDefinitionRepository;
import com.termux.shared.shell.command.ExecutionCommand;
import com.termux.shared.termux.settings.preferences.TermuxAppSharedPreferences;
import com.termux.shared.termux.shell.TermuxShellManager;
import com.termux.shared.termux.shell.command.runner.terminal.TermuxSession;
import com.termux.terminal.JniSpawnCounter;
import com.termux.terminal.TerminalSession;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Reproduces https://github.com/HiromiShikata/termux-app/issues/2037 : the autonomous background
 * reconnect scan must force-refresh the published session list immediately before deciding,
 * instead of reusing whichever list was last fetched.
 */
@RunWith(RobolectricTestRunner.class)
public class BackgroundReconnectScanUsesFreshPublishedSessionListTest {

    private static final String INDEX_URL = "https://example.test/sessions/index.json";
    private static final String GROUP_URL = "https://example.test/sessions/group.json";

    private TermuxActivity activity;
    private TermuxService service;
    private TermuxShellManager shellManager;
    private TermuxAppSharedPreferences preferences;
    private Object sessionDefinitionRepository;

    private static final class Case {
        final String acceptanceCriterion;
        final List<String> alreadyHeldSessionNames;
        final List<String> freshlyFetchedSessionNames;
        final boolean forceRefreshFetchFails;
        final String candidateSessionName;
        final boolean expectedToBeRecreated;

        Case(String acceptanceCriterion, List<String> alreadyHeldSessionNames,
             List<String> freshlyFetchedSessionNames, boolean forceRefreshFetchFails,
             String candidateSessionName, boolean expectedToBeRecreated) {
            this.acceptanceCriterion = acceptanceCriterion;
            this.alreadyHeldSessionNames = alreadyHeldSessionNames;
            this.freshlyFetchedSessionNames = freshlyFetchedSessionNames;
            this.forceRefreshFetchFails = forceRefreshFetchFails;
            this.candidateSessionName = candidateSessionName;
            this.expectedToBeRecreated = expectedToBeRecreated;
        }
    }

    private static final class ConfigurableFreshListFetcher implements SessionDefinitionDocumentFetcher {
        private final List<String> freshSessionNames;
        private final boolean fail;

        ConfigurableFreshListFetcher(List<String> freshSessionNames, boolean fail) {
            this.freshSessionNames = freshSessionNames;
            this.fail = fail;
        }

        @Override
        public String fetch(String url) throws IOException {
            if (fail) {
                throw new IOException("simulated published session list fetch failure");
            }
            if (INDEX_URL.equals(url)) {
                return "{\"projects\":[\"group\"]}";
            }
            if (GROUP_URL.equals(url)) {
                return groupDocument(freshSessionNames);
            }
            throw new IOException("no document registered for " + url);
        }

        private static String groupDocument(List<String> sessionNames) {
            StringBuilder urls = new StringBuilder();
            for (int index = 0; index < sessionNames.size(); index++) {
                if (index > 0) {
                    urls.append(",");
                }
                urls.append('"').append(sessionNames.get(index)).append('"');
            }
            return "[{\"story\":\"entry\",\"urls\":[" + urls + "]}]";
        }
    }

    @Before
    public void setUp() throws Exception {
        JniSpawnCounter.pretendTheDeviceNativeSubprocessLibraryIsPresent();
        JniSpawnCounter.reset();
        assertTrue("the test-source-set JNI stub must be the class on the runtime classpath, "
                + "otherwise session creation cannot be measured",
            JniSpawnCounter.stubIsActive());

        activity = Robolectric.buildActivity(TermuxActivity.class).get();
        Context appContext = RuntimeEnvironment.getApplication();

        service = Robolectric.buildService(TermuxService.class).get();
        shellManager = new TermuxShellManager(appContext);
        set(service, TermuxService.class, "mShellManager", shellManager);
        set(service, TermuxService.class, "mProperties",
            com.termux.shared.termux.settings.properties.TermuxAppSharedProperties.init(appContext));

        TermuxTerminalSessionActivityClient client = new TermuxTerminalSessionActivityClient(activity);
        set(activity, TermuxActivity.class, "mTermuxService", service);
        set(activity, TermuxActivity.class, "mTermuxTerminalSessionActivityClient", client);
        service.setTermuxTerminalSessionClient(activity.getTermuxTerminalSessionClient());

        preferences = TermuxAppSharedPreferences.build(appContext, true);
        set(activity, TermuxActivity.class, "mPreferences", preferences);
        preferences.setAutosshCommand("ssh {name}");
        preferences.setSessionDefinitionUrl(INDEX_URL);
        preferences.setRemoveGithubSessionsNotInList(true);
        preferences.setDisabledSessionNames(
            TermuxAppSharedPreferences.serializeDisabledSessionNames(Collections.<String>emptySet()));

        set(activity, TermuxActivity.class, "mProperties",
            com.termux.shared.termux.settings.properties.TermuxAppSharedProperties.init(appContext));
        set(activity, TermuxActivity.class, "mIsVisible", true);

        Field repositoryField = TermuxActivity.class.getDeclaredField("mSessionDefinitionRepository");
        repositoryField.setAccessible(true);
        sessionDefinitionRepository = repositoryField.get(activity);

        JniSpawnCounter.reset();
    }

    @After
    public void restoreTheDefaultAbsenceOfTheDeviceNativeSubprocessLibrary() {
        JniSpawnCounter.restoreTheAbsenceOfTheDeviceNativeSubprocessLibrary();
    }

    @Test
    public void theScanDecidesFromTheListItForceRefetchedRatherThanTheStaleInMemoryList() throws Exception {
        Case[] cases = {
            new Case(
                "SC-1: session present in the already-held list but absent from a successful "
                    + "force-refresh must NOT be recreated",
                Collections.singletonList("https://github.com/owner/stale-removed-session"),
                Collections.<String>emptyList(),
                false,
                "https://github.com/owner/stale-removed-session",
                false),
            new Case(
                "SC-2: session absent from the already-held list but present in a successful "
                    + "force-refresh must be recreated",
                Collections.<String>emptyList(),
                Collections.singletonList("https://github.com/owner/newly-published-session"),
                false,
                "https://github.com/owner/newly-published-session",
                true),
            new Case(
                "SC-3: when the force-refresh fetch fails, the scan must fall back to the "
                    + "already-held list and still decide (not suppress the reconnect)",
                Collections.singletonList("https://github.com/owner/fallback-session"),
                null,
                true,
                "https://github.com/owner/fallback-session",
                true),
        };

        for (Case useCase : cases) {
            shellManager.mTermuxSessions.clear();
            JniSpawnCounter.reset();

            seedAlreadyHeldPublishedSessionList(useCase.alreadyHeldSessionNames);
            installForceRefreshFetcher(useCase.freshlyFetchedSessionNames, useCase.forceRefreshFetchFails);

            TermuxSession deadSession = deadSession(useCase.candidateSessionName);
            shellManager.mTermuxSessions.add(deadSession);
            TerminalSession originalTerminalSession = deadSession.getTerminalSession();

            activity.reconnectDeadDisplayedSessions();
            flushMainLooperUntilReconnectSettles();

            TerminalSession currentTerminalSession = terminalSessionNamed(useCase.candidateSessionName);

            if (useCase.expectedToBeRecreated) {
                assertNotNull(useCase.acceptanceCriterion + ": the session must still be present\n"
                    + describeCase(useCase), currentTerminalSession);
                assertNotSame(useCase.acceptanceCriterion + ": the session must have been recreated"
                        + " (a different TerminalSession instance), but the original instance is"
                        + " still in place\n" + describeCase(useCase),
                    originalTerminalSession, currentTerminalSession);
            } else {
                if (currentTerminalSession == null) {
                    fail(useCase.acceptanceCriterion + ": the session must still be present"
                        + " (untouched), but it is gone entirely\n" + describeCase(useCase));
                }
                assertSame(useCase.acceptanceCriterion + ": the session must NOT have been"
                        + " recreated, but a different TerminalSession instance is now in place\n"
                        + describeCase(useCase),
                    originalTerminalSession, currentTerminalSession);
            }
        }
    }

    private static String describeCase(Case useCase) {
        return "candidate=" + useCase.candidateSessionName
            + " alreadyHeld=" + useCase.alreadyHeldSessionNames
            + " freshlyFetched=" + useCase.freshlyFetchedSessionNames
            + " forceRefreshFetchFails=" + useCase.forceRefreshFetchFails;
    }

    private void seedAlreadyHeldPublishedSessionList(List<String> sessionNamesInList) throws Exception {
        SessionDefinitionEntry entry = new SessionDefinitionEntry("group", "entry",
            new ArrayList<>(sessionNamesInList));
        SessionDefinitionLoadResult result = new SessionDefinitionLoadResult(
            Collections.singletonList(entry), 1, Collections.<String>emptyList());
        set(sessionDefinitionRepository, SessionDefinitionRepository.class, "result", result);
        set(sessionDefinitionRepository, SessionDefinitionRepository.class, "loaded", true);
    }

    private void installForceRefreshFetcher(List<String> freshSessionNames, boolean fail) throws Exception {
        ConfigurableFreshListFetcher fetcher = new ConfigurableFreshListFetcher(freshSessionNames, fail);
        SessionDefinitionLoader loader = new SessionDefinitionLoader(fetcher, new SessionDefinitionParser());
        set(sessionDefinitionRepository, SessionDefinitionRepository.class, "loader", loader);
    }

    private TerminalSession terminalSessionNamed(String sessionName) {
        for (TermuxSession termuxSession : shellManager.mTermuxSessions) {
            TerminalSession terminalSession = termuxSession.getTerminalSession();
            if (terminalSession != null && sessionName.equals(terminalSession.mSessionName)) {
                return terminalSession;
            }
        }
        return null;
    }

    private void flushMainLooperUntilReconnectSettles() throws InterruptedException {
        Duration perAttempt = Duration.ofMillis(50);
        for (int attempt = 0; attempt < 100; attempt++) {
            Shadows.shadowOf(Looper.getMainLooper()).idleFor(perAttempt);
            Thread.sleep(5);
        }
    }

    private TermuxSession deadSession(String name) throws Exception {
        TerminalSession terminalSession = new TerminalSession(null, null, null, null, null, null);
        terminalSession.mSessionName = name;
        Field shellPid = TerminalSession.class.getDeclaredField("mShellPid");
        shellPid.setAccessible(true);
        shellPid.setInt(terminalSession, -1);
        Constructor<TermuxSession> constructor = TermuxSession.class.getDeclaredConstructor(
            TerminalSession.class, ExecutionCommand.class, TermuxSession.TermuxSessionClient.class, boolean.class);
        constructor.setAccessible(true);
        TermuxSession termuxSession = constructor.newInstance(terminalSession, new ExecutionCommand(), null, false);
        assertNotNull(termuxSession.getTerminalSession());
        return termuxSession;
    }

    private void set(Object target, Class<?> declaringClass, String fieldName, Object value) throws Exception {
        Field field = declaringClass.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
