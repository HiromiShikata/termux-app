package com.termux.app.browser;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class BrowserBookmarkStarToolbarWiringTest {

    private static final String LAYOUT_RELATIVE_PATH =
        "src/main/res/layout/activity_termux.xml";

    private static final String EDIT_URL_LAYOUT_RELATIVE_PATH =
        "src/main/res/layout/dialog_browser_edit_url.xml";

    private static final String CONTROLLER_RELATIVE_PATH =
        "src/main/java/com/termux/app/browser/TermuxBrowserController.java";

    private static final String URL_ACTIONS_RELATIVE_PATH =
        "src/main/java/com/termux/app/browser/BrowserUrlActions.java";

    private String readModuleResource(String relativePath) throws IOException {
        Path moduleRelative = Paths.get(relativePath);
        if (Files.exists(moduleRelative)) {
            return new String(Files.readAllBytes(moduleRelative), StandardCharsets.UTF_8);
        }
        Path repoRelative = Paths.get("app").resolve(relativePath);
        return new String(Files.readAllBytes(repoRelative), StandardCharsets.UTF_8);
    }

    @Test
    public void bookmarkStarButtonSitsRightAfterTheNewTabButton() throws IOException {
        String layout = readModuleResource(LAYOUT_RELATIVE_PATH);
        int newTabIndex = layout.indexOf("@+id/browser_new_tab_button");
        int starIndex = layout.indexOf("@+id/browser_bookmark_toggle_button");
        int openInChromeIndex = layout.indexOf("@+id/browser_open_in_chrome_button");
        Assert.assertTrue("New-tab button must exist", newTabIndex >= 0);
        Assert.assertTrue("Bookmark-star button must exist", starIndex >= 0);
        Assert.assertTrue("Bookmark-star button must appear after the new-tab button",
            starIndex > newTabIndex);
        Assert.assertTrue("Bookmark-star button must appear before the open-in-chrome button",
            openInChromeIndex > starIndex);
    }

    @Test
    public void bookmarkStarButtonDefaultsToOutlineStarWithContentDescription() throws IOException {
        String layout = readModuleResource(LAYOUT_RELATIVE_PATH);
        int starIndex = layout.indexOf("@+id/browser_bookmark_toggle_button");
        int elementEnd = layout.indexOf("/>", starIndex);
        String element = layout.substring(starIndex, elementEnd);
        Assert.assertTrue("Bookmark-star button must default to the outline star drawable",
            element.contains("android:src=\"@drawable/ic_browser_bookmark_star_outline\""));
        Assert.assertTrue("Bookmark-star button must declare a content description",
            element.contains("android:contentDescription="
                + "\"@string/action_browser_bookmark_current_page\""));
    }

    @Test
    public void controllerBindsBookmarkStarButtonToTheToggleAction() throws IOException {
        String source = readModuleResource(CONTROLLER_RELATIVE_PATH);
        Assert.assertTrue(source.contains("R.id.browser_bookmark_toggle_button"));
        Assert.assertTrue(source.contains("toggleCurrentPageBookmark()"));
        Assert.assertTrue("Toggle must go through the pure toggled() helper",
            source.contains(".toggled("));
    }

    @Test
    public void controllerRefreshesStarIconWhenPageHeaderUpdates() throws IOException {
        String source = readModuleResource(CONTROLLER_RELATIVE_PATH);
        int updatePageHeaderIndex = source.indexOf("private void updatePageHeader()");
        Assert.assertTrue(updatePageHeaderIndex >= 0);
        int updatePageHeaderEnd = source.indexOf("private", updatePageHeaderIndex
            + "private void updatePageHeader()".length());
        Assert.assertTrue(updatePageHeaderEnd > updatePageHeaderIndex);
        Assert.assertTrue("Page-header refresh must also refresh the bookmark star state",
            source.substring(updatePageHeaderIndex, updatePageHeaderEnd)
                .contains("updateBookmarkToggleState()"));
    }

    @Test
    public void starIconReflectsBookmarkedStateWithFilledAndOutlineDrawables() throws IOException {
        String source = readModuleResource(CONTROLLER_RELATIVE_PATH);
        int stateIndex = source.indexOf("private void updateBookmarkToggleState()");
        Assert.assertTrue(stateIndex >= 0);
        int stateEnd = source.indexOf("private", stateIndex
            + "private void updateBookmarkToggleState()".length());
        String body = source.substring(stateIndex, stateEnd);
        Assert.assertTrue("Bookmarked state must show the filled star",
            body.contains("R.drawable.ic_browser_bookmark_star_filled"));
        Assert.assertTrue("Un-bookmarked state must show the outline star",
            body.contains("R.drawable.ic_browser_bookmark_star_outline"));
        Assert.assertTrue("State must be derived from the current page URL bookmark membership",
            body.contains("loadBookmarks().contains("));
    }

    @Test
    public void editUrlDialogNoLongerOffersReopenClosedTabButton() throws IOException {
        String editUrlLayout = readModuleResource(EDIT_URL_LAYOUT_RELATIVE_PATH);
        Assert.assertFalse("Reopen-closed-tab button must be removed from the Edit URL dialog",
            editUrlLayout.contains("browser_edit_url_reopen_closed_tab"));
        String urlActions = readModuleResource(URL_ACTIONS_RELATIVE_PATH);
        Assert.assertFalse("Reopen-closed-tab handler must be removed from BrowserUrlActions",
            urlActions.contains("reopenLastClosedTab"));
        Assert.assertFalse("hasRecentlyClosedTab host hook must be removed from BrowserUrlActions",
            urlActions.contains("hasRecentlyClosedTab"));
    }

    @Test
    public void undoSnackbarPathIsRetained() throws IOException {
        String source = readModuleResource(CONTROLLER_RELATIVE_PATH);
        int closeTabIndex = source.indexOf("public void closeTab(");
        Assert.assertTrue(closeTabIndex >= 0);
        int closeTabEnd = source.indexOf("private void rememberClosedTab", closeTabIndex);
        Assert.assertTrue(closeTabEnd > closeTabIndex);
        Assert.assertTrue("Every closeTab must show the tab-closed undo snackbar",
            source.substring(closeTabIndex, closeTabEnd).contains("showTabClosedUndoSnackbar()"));
        Assert.assertTrue("Undo action must reopen the last closed tab",
            source.contains("reopenLastClosedTab()"));
    }
}
