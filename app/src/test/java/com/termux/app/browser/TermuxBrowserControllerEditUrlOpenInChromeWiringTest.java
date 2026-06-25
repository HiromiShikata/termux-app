package com.termux.app.browser;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class TermuxBrowserControllerEditUrlOpenInChromeWiringTest {

    private static final String CONTROLLER_RELATIVE_PATH =
        "src/main/java/com/termux/app/browser/TermuxBrowserController.java";

    private static final String EDIT_URL_LAYOUT_RELATIVE_PATH =
        "src/main/res/layout/dialog_browser_edit_url.xml";

    private String readModuleFile(String relativePath) throws IOException {
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
    public void editUrlDialogLayoutExposesOpenInChromeButtonWithOpenInNewIcon() throws IOException {
        String layout = readModuleFile(EDIT_URL_LAYOUT_RELATIVE_PATH);

        Assert.assertTrue("Open in Chrome button is missing from the per-tab edit URL menu",
            layout.contains("@+id/browser_edit_url_open_in_chrome"));
        Assert.assertTrue("Open in Chrome button must use the standard open-in-new icon",
            layout.contains("@drawable/ic_open_in_new"));
        Assert.assertTrue("Open in Chrome button must use the existing open-in-chrome label",
            layout.contains("@string/action_browser_open_in_chrome"));
    }

    @Test
    public void editUrlDialogWiresOpenInChromeButtonToExternalOpenPath() throws IOException {
        String dialogBody = methodBody(
            readModuleFile(CONTROLLER_RELATIVE_PATH), "private void promptEditCurrentPageUrl() {");

        Assert.assertTrue("Open in Chrome button must be resolved from the dialog view",
            dialogBody.contains("R.id.browser_edit_url_open_in_chrome"));
        Assert.assertTrue("Open in Chrome button must be labeled with the open-in-chrome string",
            dialogBody.contains("R.string.action_browser_open_in_chrome"));
        Assert.assertTrue("Open in Chrome button must delegate to the edited-url external open handler",
            dialogBody.contains("openEditedUrlInChrome(urlInput.getText().toString())"));
    }

    @Test
    public void openEditedUrlInChromeUsesExistingExternalOpenMechanism() throws IOException {
        String handlerBody = methodBody(
            readModuleFile(CONTROLLER_RELATIVE_PATH), "private void openEditedUrlInChrome(");

        Assert.assertTrue("handler must reuse the shared external open-in-chrome mechanism",
            handlerBody.contains("ShareUtils.openUrlInChrome(mActivity, url)"));
        Assert.assertTrue("handler must guard against an empty or invalid URL",
            handlerBody.contains("BrowserEditedUrl.trimmedOrNull(editedUrl)"));
    }
}
