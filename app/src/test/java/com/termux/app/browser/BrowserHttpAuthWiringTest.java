package com.termux.app.browser;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class BrowserHttpAuthWiringTest {

    private static final String TERMUX_BROWSER_CONTROLLER_PATH =
        "src/main/java/com/termux/app/browser/TermuxBrowserController.java";

    private static final String DESKTOP_VIEWPORT_CLIENT_PATH =
        "src/main/java/com/termux/app/browser/BrowserDesktopViewportWebViewClient.java";

    private static final String DIALOG_PATH =
        "src/main/java/com/termux/app/browser/BrowserHttpAuthDialog.java";

    private String readModuleSource(String relativePath) throws IOException {
        Path moduleRelative = Paths.get(relativePath);
        if (Files.exists(moduleRelative)) {
            return new String(Files.readAllBytes(moduleRelative), StandardCharsets.UTF_8);
        }
        Path repoRelative = Paths.get("app").resolve(relativePath);
        return new String(Files.readAllBytes(repoRelative), StandardCharsets.UTF_8);
    }

    @Test
    public void termuxBrowserControllerOverridesHttpAuthRequest() throws IOException {
        String source = readModuleSource(TERMUX_BROWSER_CONTROLLER_PATH);
        Assert.assertTrue(source.contains("public void onReceivedHttpAuthRequest"));
        Assert.assertTrue(source.contains(
            "BrowserHttpAuthDialog.show(view.getContext(), handler, host, realm)"));
    }

    @Test
    public void desktopViewportClientOverridesHttpAuthRequest() throws IOException {
        String source = readModuleSource(DESKTOP_VIEWPORT_CLIENT_PATH);
        Assert.assertTrue(source.contains("public void onReceivedHttpAuthRequest"));
        Assert.assertTrue(source.contains(
            "BrowserHttpAuthDialog.show(view.getContext(), handler, host, realm)"));
    }

    @Test
    public void dialogProceedsWithEnteredCredentialsOnSignIn() throws IOException {
        String source = readModuleSource(DIALOG_PATH);
        Assert.assertTrue(source.contains("guard.proceed(request)"));
        Assert.assertTrue(source.contains("mHandler.proceed(request.getUsername(), request.getPassword())"));
    }

    @Test
    public void dialogCancelsHandlerOnEveryDismissalPath() throws IOException {
        String source = readModuleSource(DIALOG_PATH);
        Assert.assertTrue(source.contains("dialog.setCanceledOnTouchOutside(true)"));
        Assert.assertTrue(source.contains("dialog.setOnCancelListener(d -> guard.cancel())"));
        Assert.assertTrue(source.contains("cancelButton.setOnClickListener(view -> dialog.cancel())"));
        Assert.assertTrue(source.contains("mHandler.cancel()"));
    }

    @Test
    public void dialogResolvesHandlerExactlyOnce() throws IOException {
        String source = readModuleSource(DIALOG_PATH);
        Assert.assertTrue(source.contains("if (mResolved) return;"));
        Assert.assertTrue(source.contains("mResolved = true;"));
    }
}
