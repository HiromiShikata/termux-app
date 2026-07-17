package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class SessionBellAutoCloseWiringTest {

    private static final String ACTIVITY_RELATIVE_PATH =
        "src/main/java/com/termux/app/TermuxActivity.java";

    private static final String NAVIGATION_BUTTONS_BINDER_RELATIVE_PATH =
        "src/main/java/com/termux/app/terminal/SessionNavigationButtonsBinder.java";

    private static final String SWITCH_PICKER_CONTROLLER_RELATIVE_PATH =
        "src/main/java/com/termux/app/terminal/SessionSwitchPickerController.java";

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
        Assert.assertTrue(signature + " not found", methodIndex >= 0);
        int methodEnd = source.indexOf("\n    }", methodIndex);
        Assert.assertTrue(methodEnd > methodIndex);
        return source.substring(methodIndex, methodEnd);
    }

    @Test
    public void bellHandlerClosesTheBottomSheetAfterNavigating() throws IOException {
        String jumpBody = methodBody(
            readSource(ACTIVITY_RELATIVE_PATH), "private void jumpToTopmostCallingSession() {");

        int switchIndex = jumpBody.indexOf("switchToSessionAtIndex(");
        int hideIndex = jumpBody.indexOf("mSessionListBottomSheetController.hide()");

        Assert.assertTrue("bell handler must navigate to the target session", switchIndex >= 0);
        Assert.assertTrue("bell handler must close the bottom sheet", hideIndex >= 0);
        Assert.assertTrue("bell handler must close the sheet after navigating", hideIndex > switchIndex);
    }

    @Test
    public void arrowBinderDoesNotTouchTheBottomSheet() throws IOException {
        String binderSource = readSource(NAVIGATION_BUTTONS_BINDER_RELATIVE_PATH);

        Assert.assertFalse(binderSource.contains("SessionListBottomSheetController"));
        Assert.assertFalse(binderSource.contains(".hide()"));
    }

    @Test
    public void arrowDirectionTargetDoesNotCloseTheBottomSheet() throws IOException {
        String onVolumeKeyDirectionBody = methodBody(
            readSource(SWITCH_PICKER_CONTROLLER_RELATIVE_PATH),
            "public void onVolumeKeyDirection(boolean forward) {");

        Assert.assertFalse(onVolumeKeyDirectionBody.contains("SessionListBottomSheetController"));
        Assert.assertFalse(onVolumeKeyDirectionBody.contains("getSessionListBottomSheetController"));
    }

    @Test
    public void arrowWiringSiteDoesNotCloseTheBottomSheet() throws IOException {
        String navigationButtonsViewBody = methodBody(
            readSource(ACTIVITY_RELATIVE_PATH), "private void setSessionNavigationButtonsView() {");

        Assert.assertTrue(
            "arrow buttons must stay bound through SessionNavigationButtonsBinder",
            navigationButtonsViewBody.contains("SessionNavigationButtonsBinder.bind("));
        Assert.assertFalse(
            "arrow wiring site must not close the bottom sheet",
            navigationButtonsViewBody.contains("mSessionListBottomSheetController.hide()"));
    }
}
