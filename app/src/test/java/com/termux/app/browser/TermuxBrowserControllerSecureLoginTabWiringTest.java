package com.termux.app.browser;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class TermuxBrowserControllerSecureLoginTabWiringTest {

    private static final String CONTROLLER_RELATIVE_PATH =
        "src/main/java/com/termux/app/browser/TermuxBrowserController.java";

    private static final String DRAWER_LAYOUT_RELATIVE_PATH =
        "src/main/res/layout/activity_termux.xml";

    private String readModuleFile(String relativePath) throws IOException {
        Path moduleRelative = Paths.get(relativePath);
        if (Files.exists(moduleRelative)) {
            return new String(Files.readAllBytes(moduleRelative), StandardCharsets.UTF_8);
        }
        Path repoRelative = Paths.get("app").resolve(relativePath);
        return new String(Files.readAllBytes(repoRelative), StandardCharsets.UTF_8);
    }

    @Test
    public void secureLoginTabLauncherIsSourcedFromTheTrustedCurrentUrlOnly() throws IOException {
        String controller = readModuleFile(CONTROLLER_RELATIVE_PATH);

        int launcherIndex = controller.indexOf("createSecureLoginTabLauncher()");
        Assert.assertTrue("secure login tab launcher factory is missing", launcherIndex >= 0);
        int factoryIndex = controller.indexOf("private BrowserSecureLoginTabLauncher createSecureLoginTabLauncher()");
        Assert.assertTrue(factoryIndex >= 0);
        String factoryBody = controller.substring(factoryIndex, controller.indexOf("\n    }", factoryIndex));

        Assert.assertTrue("launcher must be sourced from the trusted current page URL",
            factoryBody.contains("this::currentTrustedPageUrl"));
        Assert.assertTrue("launcher must resolve the launch mechanism from the device",
            factoryBody.contains("BrowserSecureLoginTab.resolveMechanism(mActivity)"));
        Assert.assertTrue("secure tab sink must open in the in-app custom tab",
            factoryBody.contains("BrowserSecureLoginTab.openInCustomTab(mActivity, url)"));
        Assert.assertTrue("fallback must reuse the existing external Chrome mechanism",
            factoryBody.contains("ShareUtils.openUrlInChrome(mActivity, url)"));
    }

    @Test
    public void trustedCurrentPageUrlComesFromTheDisplayedWebViewNotAPageSuppliedValue() throws IOException {
        String controller = readModuleFile(CONTROLLER_RELATIVE_PATH);

        int index = controller.indexOf("private String currentTrustedPageUrl()");
        Assert.assertTrue(index >= 0);
        String body = controller.substring(index, controller.indexOf("\n    }", index));

        Assert.assertTrue("trusted url must come from the displayed WebView getUrl()",
            body.contains("displayedWebView.getUrl()"));
    }

    @Test
    public void passkeyAndLoginFormHintsBothOpenTheInAppSecureLoginTab() throws IOException {
        String controller = readModuleFile(CONTROLLER_RELATIVE_PATH);

        Assert.assertTrue("passkey detection must open the secure login tab hint",
            controller.contains("showSecureLoginTabHint(R.string.msg_browser_passkey_open_in_chrome"));
        Assert.assertTrue("login form detection must open the secure login tab hint",
            controller.contains("showSecureLoginTabHint(R.string.msg_browser_login_open_secure_tab"));
        Assert.assertTrue("the hint action must delegate to the secure login tab launcher",
            controller.contains("launcher.openTrustedCurrentUrlInSecureLoginTab()"));
    }

    @Test
    public void drawerExposesAnAnytimeSecureLoginTabAction() throws IOException {
        String controller = readModuleFile(CONTROLLER_RELATIVE_PATH);
        String layout = readModuleFile(DRAWER_LAYOUT_RELATIVE_PATH);

        Assert.assertTrue("drawer button must be wired to open the current page in a secure login tab",
            controller.contains("R.id.browser_secure_login_button")
                && controller.contains("openCurrentPageInSecureLoginTab()"));
        Assert.assertTrue("drawer layout must expose the secure login button",
            layout.contains("@+id/browser_secure_login_button"));
    }

    @Test
    public void secureLoginTabLaunchIsNotWiredIntoNavigationCallbacksSoItStaysOneShot() throws IOException {
        String controller = readModuleFile(CONTROLLER_RELATIVE_PATH);

        Assert.assertFalse("secure login tab launch must never fire from shouldOverrideUrlLoading",
            controller.contains("shouldOverrideUrlLoading")
                && controller.contains("openTrustedCurrentUrlInSecureLoginTab")
                && shouldOverrideBlockContainsLaunch(controller));
    }

    private boolean shouldOverrideBlockContainsLaunch(String controller) {
        int index = controller.indexOf("shouldOverrideUrlLoading");
        while (index >= 0) {
            int blockEnd = controller.indexOf("\n    }", index);
            if (blockEnd < 0) blockEnd = controller.length();
            String block = controller.substring(index, blockEnd);
            if (block.contains("openTrustedCurrentUrlInSecureLoginTab")) return true;
            index = controller.indexOf("shouldOverrideUrlLoading", index + 1);
        }
        return false;
    }
}
