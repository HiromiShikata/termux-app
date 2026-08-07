package com.termux.app.browser;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class BookmarkDeleteIsReachableFromEveryBookmarkListTest {

    private static final String CONTROLLER_RELATIVE_PATH =
        "src/main/java/com/termux/app/browser/TermuxBrowserController.java";

    private static final String BOOKMARK_ROW_LAYOUT_RELATIVE_PATH =
        "src/main/res/layout/item_browser_bookmark_list_entry.xml";

    private String readSource(String relativePath) throws IOException {
        Path moduleRelative = Paths.get(relativePath);
        if (Files.exists(moduleRelative)) {
            return new String(Files.readAllBytes(moduleRelative), StandardCharsets.UTF_8);
        }
        Path repoRelative = Paths.get("app").resolve(relativePath);
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
    public void everyBookmarkRowCarriesAVisibleDeleteControl() throws IOException {
        String rowLayout = readSource(BOOKMARK_ROW_LAYOUT_RELATIVE_PATH);

        Assert.assertTrue("a bookmark row must carry a delete control the owner can see and tap; without "
                + "one the only way to remove a bookmark is a long press that nothing on screen announces",
            rowLayout.contains("@+id/browser_bookmark_list_entry_delete_button"));
        Assert.assertTrue("the delete control must carry a content description so it is reachable by "
                + "accessibility services as well as by sight",
            rowLayout.contains("android:contentDescription"));
    }

    @Test
    public void theBookmarksDialogWiresThatDeleteControlToTheDeletePrompt() throws IOException {
        String source = readSource(CONTROLLER_RELATIVE_PATH);
        String body = methodBody(source, "private void showBookmarksList() {");

        Assert.assertTrue("the bookmarks dialog must wire each row's delete control to the delete prompt",
            body.contains("browser_bookmark_list_entry_delete_button"));
        Assert.assertTrue("tapping that control must run the same delete prompt the long press runs",
            body.contains("promptDeleteBookmark("));
    }

    @Test
    public void theNewTabListCanAlsoDeleteABookmarkItShows() throws IOException {
        String source = readSource(CONTROLLER_RELATIVE_PATH);
        String body = methodBody(source, "public void promptNewTab() {");

        Assert.assertTrue("the new-tab dialog lists bookmarks too, so it must offer a way to delete one; "
                + "with only an item click listener a bookmark shown there cannot be removed at all",
            body.contains("setOnItemLongClickListener"));
        Assert.assertTrue("that path must run the same delete prompt the bookmarks dialog runs",
            body.contains("promptDeleteBookmarkForUrl("));
    }

    @Test
    public void deletingByUrlOnlyPromptsForAUrlThatIsActuallyBookmarked() throws IOException {
        String source = readSource(CONTROLLER_RELATIVE_PATH);
        String body = methodBody(source, "private void promptDeleteBookmarkForUrl(@NonNull String url) {");

        Assert.assertTrue("a history entry that is not bookmarked must not raise a delete prompt",
            body.contains("return"));
        Assert.assertTrue("the prompt must be raised for the bookmark that matches the url",
            body.contains("promptDeleteBookmark("));
    }

    @Test
    public void theLongPressDeletePathInTheBookmarksDialogIsKept() throws IOException {
        String source = readSource(CONTROLLER_RELATIVE_PATH);
        String body = methodBody(source, "private void showBookmarksList() {");

        Assert.assertTrue("the existing long-press delete must keep working for anyone already using it",
            body.contains("setOnItemLongClickListener"));
    }
}
