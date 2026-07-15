package com.termux.app.browser;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class BrowserViewportInjectorTest {

    private static final String CORE_CLIENT_PATH =
        "src/main/java/com/termux/app/browser/BrowserCoreWebViewClient.java";

    private static final String SESSION_CONTROLLER_PATH =
        "src/main/java/com/termux/app/browser/TermuxBrowserController.java";

    @Test
    public void desktopModeSelectsTheDesktopViewportScript() {
        Assert.assertEquals(
            BrowserDesktopViewport.INJECTION_SCRIPT,
            BrowserViewportInjector.scriptFor(BrowserViewMode.DESKTOP, false));
    }

    @Test
    public void desktopModeSelectsTheDesktopViewportScriptEvenWhenMobileInjectionRequested() {
        Assert.assertEquals(
            BrowserDesktopViewport.INJECTION_SCRIPT,
            BrowserViewportInjector.scriptFor(BrowserViewMode.DESKTOP, true));
    }

    @Test
    public void mobileModeInjectsNothingWhenMobileViewportInjectionIsDisabled() {
        Assert.assertNull(BrowserViewportInjector.scriptFor(BrowserViewMode.MOBILE, false));
    }

    @Test
    public void mobileModeSelectsTheMobileViewportScriptWhenInjectionIsEnabled() {
        Assert.assertEquals(
            BrowserMobileViewport.INJECTION_SCRIPT,
            BrowserViewportInjector.scriptFor(BrowserViewMode.MOBILE, true));
    }

    @Test
    public void postLoadInjectionSkipsOnlyTheScriptAlreadyRegisteredAtDocumentStart()
            throws IOException {
        String source = readModuleSource(CORE_CLIENT_PATH);
        int injectIndex = source.indexOf("private void injectViewport");
        Assert.assertTrue(injectIndex >= 0);
        String body = source.substring(injectIndex, source.indexOf("\n    }", injectIndex));
        Assert.assertTrue("post-load injection must consult the document-start script",
            body.contains("getDocumentStartViewportScript()"));
        Assert.assertTrue("desktop post-load injection must skip only the desktop script when it"
                + " was already registered at document-start",
            body.contains("BrowserDesktopViewport.INJECTION_SCRIPT.equals(documentStartScript)"));
        Assert.assertTrue("mobile post-load injection must skip only the mobile script when it"
                + " was already registered at document-start",
            body.contains("BrowserMobileViewport.INJECTION_SCRIPT.equals(documentStartScript)"));
    }

    @Test
    public void sessionControllerRegistersDesktopViewportAtDocumentStart() throws IOException {
        String source = readModuleSource(SESSION_CONTROLLER_PATH);
        Assert.assertTrue(source.contains("BrowserViewportInjector.applyDocumentStart(webView"));
        Assert.assertTrue(source.contains("getDocumentStartViewportScript()"));
    }

    private String readModuleSource(String relativePath) throws IOException {
        Path moduleRelative = Paths.get(relativePath);
        if (Files.exists(moduleRelative)) {
            return new String(Files.readAllBytes(moduleRelative), StandardCharsets.UTF_8);
        }
        Path repoRelative = Paths.get("app").resolve(relativePath);
        return new String(Files.readAllBytes(repoRelative), StandardCharsets.UTF_8);
    }
}
