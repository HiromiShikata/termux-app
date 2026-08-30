package com.termux.app;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class TermuxActivityOwnerCallDialogUrlOpensInMatchingAppTest {

    private static final String ACTIVITY_RELATIVE_PATH =
        "src/main/java/com/termux/app/TermuxActivity.java";

    private String readModuleFile(String relativePath) throws IOException {
        Path moduleRelative = Paths.get(relativePath);
        if (Files.exists(moduleRelative)) {
            return new String(Files.readAllBytes(moduleRelative), StandardCharsets.UTF_8);
        }
        Path repoRelative = Paths.get("app").resolve(relativePath);
        return new String(Files.readAllBytes(repoRelative), StandardCharsets.UTF_8);
    }

    @Test
    public void ownerCallDialogUrlTapRoutesThroughNativeAppBeforeOpeningInBrowser() throws IOException {
        String source = readModuleFile(ACTIVITY_RELATIVE_PATH);
        int methodIndex = source.indexOf("public void onUrlTapped(@NonNull String url)");
        Assert.assertTrue("onUrlTapped not found in TermuxActivity", methodIndex >= 0);
        String methodRegion = source.substring(methodIndex, methodIndex + 400);
        Assert.assertTrue(
            "onUrlTapped in the owner call dialog must route through NativeAppLink.openInNativeAppOrElse before opening in the browser",
            methodRegion.contains("NativeAppLink.openInNativeAppOrElse("));
    }

    @Test
    public void ownerCallDialogUrlTapDoesNotBypassNativeAppResolution() throws IOException {
        String source = readModuleFile(ACTIVITY_RELATIVE_PATH);
        int methodIndex = source.indexOf("public void onUrlTapped(@NonNull String url)");
        Assert.assertTrue("onUrlTapped not found in TermuxActivity", methodIndex >= 0);
        String methodRegion = source.substring(methodIndex, methodIndex + 400);
        Assert.assertFalse(
            "onUrlTapped in the owner call dialog must not bypass native-app resolution by calling openUrlInNewTab directly without NativeAppLink",
            !methodRegion.contains("NativeAppLink.openInNativeAppOrElse(") && methodRegion.contains("openUrlInNewTab"));
    }
}
