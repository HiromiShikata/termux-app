package com.termux.app.browser;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class BrowserWebViewContributesNoAssistStructureTest {

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

    private static String browserWebViewSource() throws IOException {
        return readModuleResource(
            "src/main/java/com/termux/app/browser/BrowserAssistStructureFreeWebView.java");
    }

    private static String createWebViewForTabBody() throws IOException {
        String source = readModuleResource(
            "src/main/java/com/termux/app/browser/TermuxBrowserController.java");
        int start = source.indexOf("private WebView createWebViewForTab(");
        Assert.assertTrue("The tab web view factory must exist for this test to mean anything", start >= 0);
        int end = source.indexOf("webView.setWebViewClient(", start);
        Assert.assertTrue("The end of the tab web view factory must be locatable", end > start);
        return source.substring(start, end);
    }

    @Test
    public void theTabWebViewOverridesTheAssistStructureHookThePlatformCallsOnIt() throws IOException {
        String source = browserWebViewSource();

        Assert.assertTrue("android.view.View dispatches the assist walk to onProvideVirtualStructure, so the"
                + " web view has to override that exact method to keep its document out of the walk: " + source,
            source.contains("public void onProvideVirtualStructure(ViewStructure structure)"));
        Assert.assertTrue("the class has to be a web view for the platform to reach the override: " + source,
            source.contains("extends WebView"));
    }

    @Test
    public void theOverrideDoesNotDelegateToTheImplementationThatWalksTheDocument() throws IOException {
        String source = browserWebViewSource();

        Assert.assertFalse("delegating to the default implementation is exactly the 2098 ms document walk this"
                + " exists to avoid, so no call to it may remain: " + source,
            source.contains("super.onProvideVirtualStructure("));
    }

    @Test
    public void theAutofillHookIsLeftAloneSoFormsInsideThePageStillFill() throws IOException {
        String source = browserWebViewSource();

        Assert.assertFalse("autofill reaches the page through onProvideAutofillVirtualStructure, which is a"
                + " different method on a different dispatch branch, and overriding it here would take"
                + " autofill away from the pages the owner uses: " + source,
            source.contains("onProvideAutofillVirtualStructure"));
    }

    @Test
    public void everyTabWebViewIsBuiltFromThatClassRatherThanAPlainWebView() throws IOException {
        String body = createWebViewForTabBody();

        Assert.assertTrue("the assist walk covers every child of the container without checking visibility, so"
                + " a tab built as a plain web view is walked even while it is off screen: " + body,
            body.contains("new BrowserAssistStructureFreeWebView("));
        Assert.assertFalse("a plain web view left in this factory would keep the document walk for that tab: "
                + body,
            body.contains("new WebView("));
    }
}
