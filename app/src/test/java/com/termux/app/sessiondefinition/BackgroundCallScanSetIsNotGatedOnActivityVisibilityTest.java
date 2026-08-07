package com.termux.app.sessiondefinition;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class BackgroundCallScanSetIsNotGatedOnActivityVisibilityTest {

    private static final String CLIENT_RELATIVE_PATH =
        "src/main/java/com/termux/app/terminal/TermuxTerminalSessionActivityClient.java";

    private static final String SELECTOR_RELATIVE_PATH =
        "src/main/java/com/termux/app/sessiondefinition/DisplayedSessionSelector.java";

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
    public void theClientBuildsItsScanSetWithoutGatingOnActivityVisibility() throws IOException {
        String body = methodBody(readSource(CLIENT_RELATIVE_PATH),
            "private Set<String> displayedSessionNames() {");

        Assert.assertTrue("the set the one-minute cycle scans must not be gated on the activity being "
                + "visible; the cycle already keeps running while the app is backgrounded, so a gated "
                + "set makes every firing return at its empty-set guard and detect nothing",
            body.contains("selectDisplayedSessionNamesRegardlessOfActivityVisibility("));
        Assert.assertTrue("the activity's current visibility must no longer decide the contents of that set",
            !body.contains("mActivity.isVisible()"));
    }

    @Test
    public void theSelectorOffersASelectionThatDoesNotConsultActivityVisibility() throws IOException {
        String source = readSource(SELECTOR_RELATIVE_PATH);

        Assert.assertTrue("the selector must offer a selection the background scan can use",
            source.contains("selectDisplayedSessionNamesRegardlessOfActivityVisibility("));

        String body = methodBody(source,
            "public Set<String> selectDisplayedSessionNamesRegardlessOfActivityVisibility(");
        Assert.assertTrue("that selection must not consult activity visibility at all",
            !body.contains("activityVisible"));
        Assert.assertTrue("it must still exclude the sessions the owner hid",
            body.contains("hiddenSessionNames"));
        Assert.assertTrue("it must still exclude sessions under a collapsed project",
            body.contains("expandedProjectSessionNames"));
    }

    @Test
    public void theVisibilityGatedSelectionIsKeptForCallersThatRenderOnScreenRows() throws IOException {
        String source = readSource(SELECTOR_RELATIVE_PATH);

        Assert.assertTrue("the existing visibility-gated selection must keep returning nothing while the "
                + "activity is not visible, because callers that render on-screen rows rely on it",
            source.contains("if (!activityVisible)"));
        Assert.assertTrue("that gated selection must keep its existing name so its callers are untouched",
            source.contains("public Set<String> selectDisplayedSessionNames(boolean activityVisible,"));
    }
}
