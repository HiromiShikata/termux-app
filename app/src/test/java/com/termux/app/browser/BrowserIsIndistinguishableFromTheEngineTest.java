package com.termux.app.browser;

import android.webkit.WebSettings;
import android.webkit.WebView;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RunWith(RobolectricTestRunner.class)
public class BrowserIsIndistinguishableFromTheEngineTest {

    private static final String ENGINE_USER_AGENT =
        "Mozilla/5.0 (Linux; Android 13; Pixel 6 Build/TQ3A.230805.001; wv) "
            + "AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/123.0.6312.80 Mobile Safari/537.36";

    private static Path moduleResource(String relativePath) {
        Path moduleRelative = Paths.get(relativePath);
        if (Files.exists(moduleRelative)) {
            return moduleRelative;
        }
        return Paths.get("app").resolve(relativePath);
    }

    private static String readModuleResource(String relativePath) throws IOException {
        return new String(Files.readAllBytes(moduleResource(relativePath)), StandardCharsets.UTF_8);
    }

    private static List<Path> browserSourceFiles() throws IOException {
        try (Stream<Path> paths = Files.walk(moduleResource("src/main/java/com/termux/app/browser"))) {
            return paths.filter(path -> path.toString().endsWith(".java")).collect(Collectors.toList());
        }
    }

    private static String readSource(Path path) {
        try {
            return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private WebView newWebView() {
        return new WebView(RuntimeEnvironment.getApplication());
    }

    @Test
    public void aMobileTabSendsTheUserAgentTheEngineItselfWouldSend() {
        WebView webView = newWebView();
        WebSettings settings = webView.getSettings();
        String engineOwnUserAgent = settings.getUserAgentString();

        BrowserWebViewConfigurator.apply(webView, BrowserViewMode.MOBILE, ENGINE_USER_AGENT);

        Assert.assertEquals("a page must not be able to tell this browser from the engine underneath it,"
                + " so an ordinary tab has to keep the engine's own user agent rather than a fabricated one",
            engineOwnUserAgent, settings.getUserAgentString());
    }

    @Test
    public void aDesktopTabAdvertisesTheVersionTheEngineReportsRatherThanAFixedOne() {
        WebView webView = newWebView();

        BrowserWebViewConfigurator.apply(webView, BrowserViewMode.DESKTOP, ENGINE_USER_AGENT);
        String desktopUserAgent = webView.getSettings().getUserAgentString();

        Assert.assertTrue("requesting the desktop layout may change the platform token the way an ordinary"
                + " browser does, but the browser major version has to stay the engine's own so it agrees"
                + " with the client hints the same engine keeps reporting: " + desktopUserAgent,
            desktopUserAgent.contains("Chrome/123."));
        Assert.assertFalse("a hardcoded version drifts away from the installed engine and contradicts it: "
                + desktopUserAgent,
            desktopUserAgent.contains("Chrome/150"));
    }

    @Test
    public void noScriptTheBrowserInjectsReplacesTheCredentialApiThatSignInPagesUse() throws IOException {
        for (Path path : browserSourceFiles()) {
            String source = readSource(path);
            Assert.assertFalse("replacing navigator.credentials.get makes the function fail the native-code"
                    + " check a sign-in page performs, so no injected script may assign to it: " + path,
                source.contains("navigator.credentials.get="));
            Assert.assertFalse("replacing navigator.credentials.create makes the function fail the"
                    + " native-code check a sign-in page performs, so no injected script may assign to it: "
                    + path,
                source.contains("navigator.credentials.create="));
        }
    }

    @Test
    public void noNameCarryingTheApplicationIsLeftOnAnyPage() throws IOException {
        for (Path path : browserSourceFiles()) {
            Assert.assertFalse("a global named after this application identifies the client as one specific"
                    + " application rather than as a browser, so no page-visible name may carry it: " + path,
                readSource(path).contains("__termux"));
        }
    }

    @Test
    public void theTabFactoryExposesNoJavaObjectToThePagesItLoads() throws IOException {
        String source = readModuleResource("src/main/java/com/termux/app/browser/TermuxBrowserController.java");
        int start = source.indexOf("private WebView createWebViewForTab(");
        Assert.assertTrue("the tab web view factory must exist for this test to mean anything", start >= 0);
        int end = source.indexOf("webView.setWebViewClient(", start);
        Assert.assertTrue("the end of the tab web view factory must be locatable", end > start);
        String factoryBody = source.substring(start, end);

        Assert.assertFalse("an object added with addJavascriptInterface appears on window and any page can"
                + " enumerate it, so a tab must not be given one: " + factoryBody,
            factoryBody.contains("addJavascriptInterface"));
    }
}
