package com.termux.app.browser;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class BrowserProjectActionButtonsControllerTest {

    private static final class OpenedAction {
        final String url;
        final BrowserViewMode viewMode;

        OpenedAction(String url, BrowserViewMode viewMode) {
            this.url = url;
            this.viewMode = viewMode;
        }
    }

    private static final class RecordingOpener
            implements BrowserProjectActionButtonsController.ProjectActionUrlOpener {
        final List<OpenedAction> openedActions = new ArrayList<>();

        @Override
        public void openProjectActionUrl(@NonNull String url, @NonNull BrowserViewMode viewMode) {
            openedActions.add(new OpenedAction(url, viewMode));
        }
    }

    private Context context() {
        return RuntimeEnvironment.getApplication();
    }

    private BrowserProjectActionButtonsController controllerFor(View overview, View tdpmConsole,
                                                               View newIssue, RecordingOpener opener) {
        return new BrowserProjectActionButtonsController(overview, tdpmConsole, newIssue, opener);
    }

    @Test
    public void eachButtonOpensItsActionUrlWithTheConfiguredViewMode() {
        Context context = context();
        ImageView overview = new ImageView(context);
        ImageView tdpmConsole = new ImageView(context);
        ImageView newIssue = new ImageView(context);
        RecordingOpener opener = new RecordingOpener();
        BrowserProjectActionButtonsController controller =
            controllerFor(overview, tdpmConsole, newIssue, opener);

        controller.setActionUrls(new BrowserProjectActionUrls(
            "https://overview.example/", "https://console.example/", "https://newissue.example/"));

        overview.performClick();
        tdpmConsole.performClick();
        newIssue.performClick();

        Assert.assertEquals(3, opener.openedActions.size());
        Assert.assertEquals("https://overview.example/", opener.openedActions.get(0).url);
        Assert.assertEquals(BrowserViewMode.DESKTOP, opener.openedActions.get(0).viewMode);
        Assert.assertEquals("https://console.example/", opener.openedActions.get(1).url);
        Assert.assertEquals(BrowserViewMode.MOBILE, opener.openedActions.get(1).viewMode);
        Assert.assertEquals("https://newissue.example/", opener.openedActions.get(2).url);
        Assert.assertEquals(BrowserViewMode.DESKTOP, opener.openedActions.get(2).viewMode);
    }

    @Test
    public void buttonsAreVisibleWhenTheirUrlIsPresent() {
        Context context = context();
        ImageView overview = new ImageView(context);
        ImageView tdpmConsole = new ImageView(context);
        ImageView newIssue = new ImageView(context);
        BrowserProjectActionButtonsController controller =
            controllerFor(overview, tdpmConsole, newIssue, new RecordingOpener());

        controller.setActionUrls(new BrowserProjectActionUrls(
            "https://overview.example/", "https://console.example/", "https://newissue.example/"));

        Assert.assertEquals(View.VISIBLE, overview.getVisibility());
        Assert.assertEquals(View.VISIBLE, tdpmConsole.getVisibility());
        Assert.assertEquals(View.VISIBLE, newIssue.getVisibility());
    }

    @Test
    public void buttonWithoutUrlIsHiddenAndDoesNotOpenAnything() {
        Context context = context();
        ImageView overview = new ImageView(context);
        ImageView tdpmConsole = new ImageView(context);
        ImageView newIssue = new ImageView(context);
        RecordingOpener opener = new RecordingOpener();
        BrowserProjectActionButtonsController controller =
            controllerFor(overview, tdpmConsole, newIssue, opener);

        controller.setActionUrls(new BrowserProjectActionUrls(
            "https://overview.example/", null, null));

        Assert.assertEquals(View.VISIBLE, overview.getVisibility());
        Assert.assertEquals(View.GONE, tdpmConsole.getVisibility());
        Assert.assertEquals(View.GONE, newIssue.getVisibility());

        tdpmConsole.performClick();
        newIssue.performClick();
        Assert.assertTrue(opener.openedActions.isEmpty());
    }

    @Test
    public void allButtonsHiddenForEmptyActionUrls() {
        Context context = context();
        ImageView overview = new ImageView(context);
        ImageView tdpmConsole = new ImageView(context);
        ImageView newIssue = new ImageView(context);
        BrowserProjectActionButtonsController controller =
            controllerFor(overview, tdpmConsole, newIssue, new RecordingOpener());

        controller.setActionUrls(BrowserProjectActionUrls.EMPTY);

        Assert.assertEquals(View.GONE, overview.getVisibility());
        Assert.assertEquals(View.GONE, tdpmConsole.getVisibility());
        Assert.assertEquals(View.GONE, newIssue.getVisibility());
    }
}
