package com.termux.app.browser;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ProjectBrowserOverlayForeignFrameResetWiringTest {

    private static final String CONTROLLER_RELATIVE_PATH =
        "src/main/java/com/termux/app/browser/ProjectBrowserOverlayController.java";

    private String readControllerSource() throws IOException {
        Path moduleRelative = Paths.get(CONTROLLER_RELATIVE_PATH);
        if (Files.exists(moduleRelative)) {
            return new String(Files.readAllBytes(moduleRelative), StandardCharsets.UTF_8);
        }
        Path repoRelative = Paths.get("app").resolve(CONTROLLER_RELATIVE_PATH);
        return new String(Files.readAllBytes(repoRelative), StandardCharsets.UTF_8);
    }

    @Test
    public void hideBlanksTheWebViewAndClearsLoadedUrlBookkeeping() throws IOException {
        String source = readControllerSource();
        int hideIndex = source.indexOf("public void hide()");
        Assert.assertTrue(hideIndex >= 0);
        int hideEnd = source.indexOf("\n    }", hideIndex);
        String hideBody = source.substring(hideIndex, hideEnd);
        Assert.assertTrue(hideBody.contains("resetWebViewToBlank()"));

        int resetIndex = source.indexOf("private void resetWebViewToBlank()");
        Assert.assertTrue(resetIndex >= 0);
        int resetEnd = source.indexOf("\n    }", resetIndex);
        String resetBody = source.substring(resetIndex, resetEnd);
        Assert.assertTrue(resetBody.contains("mCurrentUrl = null"));
        Assert.assertTrue(resetBody.contains("mLoadedUrl = null"));
        Assert.assertTrue(resetBody.contains("mWebView.loadUrl(\"about:blank\")"));
    }

    @Test
    public void openProjectUrlShowsCoverWhileTheNextPageLoads() throws IOException {
        String source = readControllerSource();
        int openIndex = source.indexOf("public void openProjectUrl(@NonNull String url)");
        Assert.assertTrue(openIndex >= 0);
        int openEnd = source.indexOf("\n    }", openIndex);
        String openBody = source.substring(openIndex, openEnd);
        Assert.assertTrue(openBody.contains("BrowserPageTransition.requiresCoverWhileLoading"));
        Assert.assertTrue(openBody.contains("mWebViewCover.setVisibility(View.VISIBLE)"));
    }

    @Test
    public void activityDestroyStillBlanksTheWebView() throws IOException {
        String source = readControllerSource();
        int destroyIndex = source.indexOf("public void onActivityDestroy()");
        Assert.assertTrue(destroyIndex >= 0);
        int destroyEnd = source.indexOf("\n    }", destroyIndex);
        String destroyBody = source.substring(destroyIndex, destroyEnd);
        Assert.assertTrue(destroyBody.contains("mWebView.loadUrl(\"about:blank\")"));
    }
}
