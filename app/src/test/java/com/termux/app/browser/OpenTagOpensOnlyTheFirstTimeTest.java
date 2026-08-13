package com.termux.app.browser;

import androidx.annotation.NonNull;

import com.termux.shared.termux.settings.preferences.TermuxAppSharedPreferences;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class OpenTagOpensOnlyTheFirstTimeTest {

    private static final String SESSION_NAME = "session-the-owner-reads";

    private static final String FIRST_HANDLE = "handle-before-the-session-was-replaced";

    private static final String SECOND_HANDLE = "handle-after-the-session-was-replaced";

    private static final String BACKLOG_URL = "https://example.com/opened-a-long-time-ago";

    private static final String FRESH_URL = "https://example.com/printed-just-now";

    private static final boolean OUTPUT_THE_OWNER_HAS_NOT_SEEN = true;

    private static final boolean OUTPUT_THE_OWNER_HAS_SEEN = false;

    private static final class RecordingUrlOpener implements OpenTagBrowserController.UrlOpener {
        final List<String> urls = new ArrayList<>();

        @Override
        public void openUrlInTabForSession(@NonNull String sessionHandle, @NonNull String url) {
            urls.add(url);
        }
    }

    private TermuxAppSharedPreferences preferences;

    private RecordingUrlOpener opener;

    private OpenTagBrowserController controller;

    @Before
    public void setUp() {
        preferences = TermuxAppSharedPreferences.build(RuntimeEnvironment.getApplication(), true);
        Assert.assertNotNull(preferences);
        preferences.setOpenTagAutoOpenEnabled(true);
        opener = new RecordingUrlOpener();
        controller = new OpenTagBrowserController(preferences, opener);
    }

    @Test
    public void aTagStillOnScreenIsNotOpenedAgainAfterTheSessionIsReplaced() {
        String transcript = "<open>" + FRESH_URL + "</open>";

        controller.onSessionTextChanged(SESSION_NAME, FIRST_HANDLE, transcript,
            OUTPUT_THE_OWNER_HAS_NOT_SEEN);
        controller.onSessionTextChanged(SESSION_NAME, SECOND_HANDLE, transcript,
            OUTPUT_THE_OWNER_HAS_NOT_SEEN);

        Assert.assertEquals("a session whose shell is replaced keeps its name and its transcript, so"
                + " the tag still on screen must not be treated as a tag the owner has never been"
                + " shown", 1, opener.urls.size());
    }

    @Test
    public void aBacklogTheOwnerHasAlreadySeenIsNotOpenedWhenTheAppStartsAgain() {
        controller.onSessionTextChanged(SESSION_NAME, FIRST_HANDLE,
            "<open>" + BACKLOG_URL + "</open>", OUTPUT_THE_OWNER_HAS_SEEN);

        Assert.assertTrue("after the app process restarts nothing is remembered about what was"
                + " already opened, so a transcript the owner has already looked at must be taken as"
                + " already opened rather than opened all over again", opener.urls.isEmpty());
    }

    @Test
    public void aTagPrintedAfterTheOwnerLastLookedStillOpensOnceFollowingARestart() {
        controller.onSessionTextChanged(SESSION_NAME, FIRST_HANDLE,
            "<open>" + BACKLOG_URL + "</open>", OUTPUT_THE_OWNER_HAS_SEEN);

        String transcriptWithBothTags = "<open>" + BACKLOG_URL + "</open>\nlater output\n<open>"
            + FRESH_URL + "</open>";
        controller.onSessionTextChanged(SESSION_NAME, FIRST_HANDLE, transcriptWithBothTags,
            OUTPUT_THE_OWNER_HAS_NOT_SEEN);
        controller.onSessionTextChanged(SESSION_NAME, FIRST_HANDLE, transcriptWithBothTags,
            OUTPUT_THE_OWNER_HAS_NOT_SEEN);

        Assert.assertEquals("only the tag printed after the owner last looked is new to him", 1,
            opener.urls.size());
        Assert.assertEquals(FRESH_URL, opener.urls.get(0));
    }

    @Test
    public void theOpenedTabStillGoesToTheSessionInstanceThatIsShowingTheTag() {
        RecordingSessionOpener sessionOpener = new RecordingSessionOpener();
        OpenTagBrowserController routingController =
            new OpenTagBrowserController(preferences, sessionOpener);

        routingController.onSessionTextChanged(SESSION_NAME, SECOND_HANDLE,
            "<open>" + FRESH_URL + "</open>", OUTPUT_THE_OWNER_HAS_NOT_SEEN);

        Assert.assertEquals("the tab belongs to the live session instance, so the handle still routes"
                + " it even though what was already opened is remembered by session name",
            SECOND_HANDLE, sessionOpener.sessionHandles.get(0));
    }

    private static final class RecordingSessionOpener implements OpenTagBrowserController.UrlOpener {
        final List<String> sessionHandles = new ArrayList<>();

        @Override
        public void openUrlInTabForSession(@NonNull String sessionHandle, @NonNull String url) {
            sessionHandles.add(sessionHandle);
        }
    }
}
