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
public class OpenTagBrowserControllerTest {

    private static final String SESSION_A_HANDLE = "handle-session-a";
    private static final String SESSION_B_HANDLE = "handle-session-b";
    private static final String URL_A = "https://example.com/project-a";
    private static final String URL_B = "https://example.com/project-b";

    private static final class RecordedOpen {
        final String sessionHandle;
        final String url;

        RecordedOpen(String sessionHandle, String url) {
            this.sessionHandle = sessionHandle;
            this.url = url;
        }
    }

    private static final class RecordingUrlOpener implements OpenTagBrowserController.UrlOpener {
        final List<RecordedOpen> opens = new ArrayList<>();

        @Override
        public void openUrlInTabForSession(@NonNull String sessionHandle, @NonNull String url) {
            opens.add(new RecordedOpen(sessionHandle, url));
        }
    }

    private TermuxAppSharedPreferences preferences;

    @Before
    public void setUp() {
        preferences = TermuxAppSharedPreferences.build(RuntimeEnvironment.getApplication(), true);
        Assert.assertNotNull(preferences);
        preferences.setOpenTagAutoOpenEnabled(true);
    }

    @Test
    public void opensDetectedUrlIntoTheSessionThatProducedTheOpenTag() {
        RecordingUrlOpener opener = new RecordingUrlOpener();
        OpenTagBrowserController controller = new OpenTagBrowserController(preferences, opener);

        controller.onSessionTextChanged(SESSION_A_HANDLE, "<open>" + URL_A + "</open>");

        Assert.assertEquals(1, opener.opens.size());
        Assert.assertEquals(SESSION_A_HANDLE, opener.opens.get(0).sessionHandle);
        Assert.assertEquals(URL_A, opener.opens.get(0).url);
    }

    @Test
    public void neverSwapsTwoSessionsUrlsWhenBothProduceOpenTags() {
        RecordingUrlOpener opener = new RecordingUrlOpener();
        OpenTagBrowserController controller = new OpenTagBrowserController(preferences, opener);

        controller.onSessionTextChanged(SESSION_A_HANDLE, "<open>" + URL_A + "</open>");
        controller.onSessionTextChanged(SESSION_B_HANDLE, "<open>" + URL_B + "</open>");

        Assert.assertEquals(2, opener.opens.size());
        Assert.assertEquals(SESSION_A_HANDLE, opener.opens.get(0).sessionHandle);
        Assert.assertEquals(URL_A, opener.opens.get(0).url);
        Assert.assertEquals(SESSION_B_HANDLE, opener.opens.get(1).sessionHandle);
        Assert.assertEquals(URL_B, opener.opens.get(1).url);
    }

    @Test
    public void doesNotReopenTheSameUrlForTheSameSession() {
        RecordingUrlOpener opener = new RecordingUrlOpener();
        OpenTagBrowserController controller = new OpenTagBrowserController(preferences, opener);

        controller.onSessionTextChanged(SESSION_A_HANDLE, "<open>" + URL_A + "</open>");
        controller.onSessionTextChanged(SESSION_A_HANDLE, "<open>" + URL_A + "</open>");

        Assert.assertEquals(1, opener.opens.size());
    }

    @Test
    public void reScanningTheSameOutputDoesNotReopenButANewUrlStillOpens() {
        RecordingUrlOpener opener = new RecordingUrlOpener();
        OpenTagBrowserController controller = new OpenTagBrowserController(preferences, opener);

        String transcriptWithFirstUrl = "<open>" + URL_A + "</open>";
        controller.onSessionTextChanged(SESSION_A_HANDLE, transcriptWithFirstUrl);
        Assert.assertEquals(1, opener.opens.size());

        for (int bottomSheetOpen = 0; bottomSheetOpen < 5; bottomSheetOpen++) {
            controller.onSessionTextChanged(SESSION_A_HANDLE, transcriptWithFirstUrl);
        }
        Assert.assertEquals(1, opener.opens.size());

        controller.onSessionTextChanged(SESSION_A_HANDLE,
            "<open>" + URL_A + "</open>\noutput\n<open>" + URL_B + "</open>");

        Assert.assertEquals(2, opener.opens.size());
        Assert.assertEquals(URL_B, opener.opens.get(1).url);
    }

    @Test
    public void doesNotReopenAnEarlierUrlWhenAReScanShowsItAfterALaterUrlScrolledOut() {
        RecordingUrlOpener opener = new RecordingUrlOpener();
        OpenTagBrowserController controller = new OpenTagBrowserController(preferences, opener);

        controller.onSessionTextChanged(SESSION_A_HANDLE, "header\n<open>" + URL_A + "</open>\n");
        controller.onSessionTextChanged(SESSION_A_HANDLE,
            "<open>" + URL_A + "</open>\nmid\n<open>" + URL_B + "</open>\n");
        Assert.assertEquals(2, opener.opens.size());

        controller.onSessionTextChanged(SESSION_A_HANDLE, "old\n<open>" + URL_A + "</open>\nold2\n");

        Assert.assertEquals(2, opener.opens.size());
    }

    @Test
    public void tracksPerSessionScannerStateSoEachSessionOpensItsOwnLatestUrl() {
        RecordingUrlOpener opener = new RecordingUrlOpener();
        OpenTagBrowserController controller = new OpenTagBrowserController(preferences, opener);

        controller.onSessionTextChanged(SESSION_A_HANDLE, "<open>" + URL_A + "</open>");
        controller.onSessionTextChanged(SESSION_B_HANDLE, "<open>" + URL_A + "</open>");

        Assert.assertEquals(2, opener.opens.size());
        Assert.assertEquals(SESSION_A_HANDLE, opener.opens.get(0).sessionHandle);
        Assert.assertEquals(SESSION_B_HANDLE, opener.opens.get(1).sessionHandle);
        Assert.assertEquals(URL_A, opener.opens.get(1).url);
    }

    @Test
    public void doesNotReopenAlreadyOpenedUrlAcrossActivityRecreationsThatReRegisterTheOpener() {
        OpenTagBrowserController controller = new OpenTagBrowserController(preferences, null);
        String stillVisibleTranscript = "<open>" + URL_A + "</open>";

        RecordingUrlOpener firstActivityOpener = new RecordingUrlOpener();
        controller.setUrlOpener(firstActivityOpener);
        controller.onSessionTextChanged(SESSION_A_HANDLE, stillVisibleTranscript);

        for (int activityRecreation = 0; activityRecreation < 5; activityRecreation++) {
            controller.setUrlOpener(null);
            RecordingUrlOpener recreatedActivityOpener = new RecordingUrlOpener();
            controller.setUrlOpener(recreatedActivityOpener);
            controller.onSessionTextChanged(SESSION_A_HANDLE, stillVisibleTranscript);
            Assert.assertTrue(recreatedActivityOpener.opens.isEmpty());
        }

        Assert.assertEquals(1, firstActivityOpener.opens.size());
        Assert.assertEquals(URL_A, firstActivityOpener.opens.get(0).url);
    }

    @Test
    public void doesNotScanWhileNoForegroundOpenerIsRegistered() {
        OpenTagBrowserController controller = new OpenTagBrowserController(preferences, null);

        controller.onSessionTextChanged(SESSION_A_HANDLE, "<open>" + URL_A + "</open>");

        RecordingUrlOpener opener = new RecordingUrlOpener();
        controller.setUrlOpener(opener);
        controller.onSessionTextChanged(SESSION_A_HANDLE, "<open>" + URL_A + "</open>");

        Assert.assertEquals(1, opener.opens.size());
        Assert.assertEquals(URL_A, opener.opens.get(0).url);
    }

    @Test
    public void opensABareUrlAloneOnItsOwnLineIntoTheProducingSession() {
        RecordingUrlOpener opener = new RecordingUrlOpener();
        OpenTagBrowserController controller = new OpenTagBrowserController(preferences, opener);

        controller.onSessionTextChanged(SESSION_A_HANDLE, "running task\n" + URL_A + "\ndone\n");

        Assert.assertEquals(1, opener.opens.size());
        Assert.assertEquals(SESSION_A_HANDLE, opener.opens.get(0).sessionHandle);
        Assert.assertEquals(URL_A, opener.opens.get(0).url);
    }

    @Test
    public void doesNotOpenABareUrlEmbeddedInASentence() {
        RecordingUrlOpener opener = new RecordingUrlOpener();
        OpenTagBrowserController controller = new OpenTagBrowserController(preferences, opener);

        controller.onSessionTextChanged(SESSION_A_HANDLE, "please open " + URL_A + " to continue\n");

        Assert.assertTrue(opener.opens.isEmpty());
    }

    @Test
    public void doesNotReopenTheSameBareUrlForTheSameSessionOnSubsequentScreenUpdates() {
        RecordingUrlOpener opener = new RecordingUrlOpener();
        OpenTagBrowserController controller = new OpenTagBrowserController(preferences, opener);

        controller.onSessionTextChanged(SESSION_A_HANDLE, URL_A + "\n");
        controller.onSessionTextChanged(SESSION_A_HANDLE, URL_A + "\nmore output\n");

        Assert.assertEquals(1, opener.opens.size());
        Assert.assertEquals(URL_A, opener.opens.get(0).url);
    }

    @Test
    public void routesEachSessionsBareUrlToItsOwnProducingSession() {
        RecordingUrlOpener opener = new RecordingUrlOpener();
        OpenTagBrowserController controller = new OpenTagBrowserController(preferences, opener);

        controller.onSessionTextChanged(SESSION_A_HANDLE, "log\n" + URL_A + "\n");
        controller.onSessionTextChanged(SESSION_B_HANDLE, "log\n" + URL_B + "\n");

        Assert.assertEquals(2, opener.opens.size());
        Assert.assertEquals(SESSION_A_HANDLE, opener.opens.get(0).sessionHandle);
        Assert.assertEquals(URL_A, opener.opens.get(0).url);
        Assert.assertEquals(SESSION_B_HANDLE, opener.opens.get(1).sessionHandle);
        Assert.assertEquals(URL_B, opener.opens.get(1).url);
    }

    @Test
    public void doesNotOpenABareUrlWhenAutoOpenDisabled() {
        preferences.setOpenTagAutoOpenEnabled(false);
        RecordingUrlOpener opener = new RecordingUrlOpener();
        OpenTagBrowserController controller = new OpenTagBrowserController(preferences, opener);

        controller.onSessionTextChanged(SESSION_A_HANDLE, "log\n" + URL_A + "\n");

        Assert.assertTrue(opener.opens.isEmpty());
    }

    @Test
    public void doesNotOpenWhenAutoOpenDisabled() {
        preferences.setOpenTagAutoOpenEnabled(false);
        RecordingUrlOpener opener = new RecordingUrlOpener();
        OpenTagBrowserController controller = new OpenTagBrowserController(preferences, opener);

        controller.onSessionTextChanged(SESSION_A_HANDLE, "<open>" + URL_A + "</open>");

        Assert.assertTrue(opener.opens.isEmpty());
    }
}
