package com.termux.app.browser;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class BrowserTabClosedSnackbarSwipeDismissTest {

    private static final String LAYOUT_RELATIVE_PATH =
        "src/main/res/layout/activity_termux.xml";

    private static final String CONTROLLER_RELATIVE_PATH =
        "src/main/java/com/termux/app/browser/TermuxBrowserController.java";

    private String readModuleResource(String relativePath) throws IOException {
        Path moduleRelative = Paths.get(relativePath);
        if (Files.exists(moduleRelative)) {
            return new String(Files.readAllBytes(moduleRelative), StandardCharsets.UTF_8);
        }
        Path repoRelative = Paths.get("app").resolve(relativePath);
        return new String(Files.readAllBytes(repoRelative), StandardCharsets.UTF_8);
    }

    @Test
    public void browserContentIsWrappedInACoordinatorLayout() throws IOException {
        String layout = readModuleResource(LAYOUT_RELATIVE_PATH);
        int containerIndex = layout.indexOf("@+id/browser_content_container");
        int coordinatorIndex = layout.indexOf("@+id/browser_content_coordinator");
        int tabBarIndex = layout.indexOf("@+id/browser_tab_bar");
        Assert.assertTrue("Browser content container must exist", containerIndex >= 0);
        Assert.assertTrue("Browser content coordinator must exist", coordinatorIndex >= 0);
        Assert.assertTrue("Coordinator must be declared as a CoordinatorLayout",
            layout.contains("androidx.coordinatorlayout.widget.CoordinatorLayout"));
        Assert.assertTrue("Coordinator must sit inside the browser content container",
            coordinatorIndex > containerIndex);
        Assert.assertTrue("Browser tab bar must sit inside the coordinator",
            tabBarIndex > coordinatorIndex);
    }

    @Test
    public void browserContentContainerKeepsItsWeightForTheSplitResize() throws IOException {
        String layout = readModuleResource(LAYOUT_RELATIVE_PATH);
        int containerIndex = layout.indexOf("@+id/browser_content_container");
        int coordinatorIndex = layout.indexOf("@+id/browser_content_coordinator");
        String containerElement = layout.substring(containerIndex, coordinatorIndex);
        Assert.assertTrue("Browser content container must retain its layout weight so the "
            + "browser/terminal split resize keeps working",
            containerElement.contains("android:layout_weight=\"2\""));
    }

    @Test
    public void snackbarIsCreatedAgainstAViewInsideTheCoordinatorLayout() throws IOException {
        String source = readModuleResource(CONTROLLER_RELATIVE_PATH);
        int snackbarIndex = source.indexOf("private void showTabClosedUndoSnackbar()");
        Assert.assertTrue(snackbarIndex >= 0);
        int snackbarEnd = source.indexOf("public void reopenLastClosedTab()", snackbarIndex);
        Assert.assertTrue(snackbarEnd > snackbarIndex);
        String body = source.substring(snackbarIndex, snackbarEnd);
        Assert.assertTrue("Snackbar must be created against the coordinator so swipe-to-dismiss "
            + "behavior is attached",
            body.contains("R.id.browser_content_coordinator"));
        Assert.assertFalse("Snackbar must not be created against android.R.id.content as its "
            + "primary root, because that view is above the CoordinatorLayout and would "
            + "disable swipe-to-dismiss",
            body.indexOf("Snackbar.make(") >= 0
                && body.substring(body.indexOf("Snackbar.make("))
                    .startsWith("Snackbar.make(\n            mActivity.findViewById(android.R.id.content)"));
    }

    @Test
    public void snackbarRetainsAnchorDurationAndUndoAction() throws IOException {
        String source = readModuleResource(CONTROLLER_RELATIVE_PATH);
        int snackbarIndex = source.indexOf("private void showTabClosedUndoSnackbar()");
        int snackbarEnd = source.indexOf("public void reopenLastClosedTab()", snackbarIndex);
        String body = source.substring(snackbarIndex, snackbarEnd);
        Assert.assertTrue("Snackbar must keep anchoring above the browser tab bar",
            body.contains("setAnchorView(") && body.contains("R.id.browser_tab_bar"));
        Assert.assertTrue("Anchor must stay guarded against a missing or hidden tab bar",
            body.contains("!= null") && body.contains("isShown()"));
        Assert.assertTrue("Snackbar must keep its tab-closed duration constant",
            body.contains("TAB_CLOSED_UNDO_SNACKBAR_DURATION_MS"));
        Assert.assertTrue("Snackbar must keep the undo action that reopens the last closed tab",
            body.contains("reopenLastClosedTab()"));
    }
}
