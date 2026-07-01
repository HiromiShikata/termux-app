package com.termux.app.browser;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class TermuxBrowserControllerBottomSheetNoAutoCloseWiringTest {

    private static final String BROWSER_CONTROLLER_RELATIVE_PATH =
        "src/main/java/com/termux/app/browser/TermuxBrowserController.java";

    private static final String BOTTOM_SHEET_CONTROLLER_RELATIVE_PATH =
        "src/main/java/com/termux/app/terminal/SessionListBottomSheetController.java";

    private static final String ACTIVITY_RELATIVE_PATH =
        "src/main/java/com/termux/app/TermuxActivity.java";

    private String readSource(String moduleRelativePath) throws IOException {
        Path moduleRelative = Paths.get(moduleRelativePath);
        if (Files.exists(moduleRelative)) {
            return new String(Files.readAllBytes(moduleRelative), StandardCharsets.UTF_8);
        }
        Path repoRelative = Paths.get("app").resolve(moduleRelativePath);
        return new String(Files.readAllBytes(repoRelative), StandardCharsets.UTF_8);
    }

    private String methodBody(String source, String signature) {
        int methodIndex = source.indexOf(signature);
        Assert.assertTrue(methodIndex >= 0);
        int methodEnd = source.indexOf("\n    }", methodIndex);
        Assert.assertTrue(methodEnd > methodIndex);
        return source.substring(methodIndex, methodEnd);
    }

    @Test
    public void openTabDoesNotHideTheSessionListBottomSheet() throws IOException {
        String openTabBody =
            methodBody(readSource(BROWSER_CONTROLLER_RELATIVE_PATH), "public void openTab(@NonNull BrowserTab tab) {");

        Assert.assertFalse(openTabBody.contains("hideSessionListBottomSheet"));
        Assert.assertFalse(openTabBody.contains("SessionListBottomSheetController"));
    }

    @Test
    public void browserControllerNoLongerHasTheBottomSheetHideHelper() throws IOException {
        String source = readSource(BROWSER_CONTROLLER_RELATIVE_PATH);

        Assert.assertFalse(source.contains("hideSessionListBottomSheet"));
    }

    @Test
    public void sessionRowTapStillHidesTheSessionListBottomSheet() throws IOException {
        String dismissBody = methodBody(
            readSource(BOTTOM_SHEET_CONTROLLER_RELATIVE_PATH), "private void dismissAfterSessionSelected() {");

        Assert.assertTrue(dismissBody.contains("hide()"));
    }

    @Test
    public void scrimTapStillHidesTheSessionListBottomSheet() throws IOException {
        String scrimBody = methodBody(
            readSource(BOTTOM_SHEET_CONTROLLER_RELATIVE_PATH), "private void bindScrimTapToDismiss() {");

        Assert.assertTrue(scrimBody.contains("hide()"));
    }

    @Test
    public void actionButtonsStillHideTheSessionListBottomSheet() throws IOException {
        String actionButtonsBody = methodBody(
            readSource(BOTTOM_SHEET_CONTROLLER_RELATIVE_PATH), "private void bindActionButtons() {");

        Assert.assertTrue(actionButtonsBody.contains("hide()"));
    }

    @Test
    public void backKeyStillHidesTheSessionListBottomSheetWhenOpen() throws IOException {
        String activitySource = readSource(ACTIVITY_RELATIVE_PATH);
        int onBackPressedIndex = activitySource.indexOf("public boolean onBackPressed() {");
        Assert.assertTrue(onBackPressedIndex >= 0);
        int onBackPressedEnd = activitySource.indexOf("\n            }", onBackPressedIndex);
        Assert.assertTrue(onBackPressedEnd > onBackPressedIndex);
        String onBackPressedBody = activitySource.substring(onBackPressedIndex, onBackPressedEnd);

        Assert.assertTrue(onBackPressedBody.contains("mSessionListBottomSheetController.isOpen()"));
        Assert.assertTrue(onBackPressedBody.contains("mSessionListBottomSheetController.hide()"));
    }
}
