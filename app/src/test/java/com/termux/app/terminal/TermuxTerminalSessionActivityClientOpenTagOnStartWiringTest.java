package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class TermuxTerminalSessionActivityClientOpenTagOnStartWiringTest {

    private static final String CLIENT_RELATIVE_PATH =
        "src/main/java/com/termux/app/terminal/TermuxTerminalSessionActivityClient.java";

    private String readClientSource() throws IOException {
        Path moduleRelative = Paths.get(CLIENT_RELATIVE_PATH);
        if (Files.exists(moduleRelative)) {
            return new String(Files.readAllBytes(moduleRelative), StandardCharsets.UTF_8);
        }
        Path repoRelative = Paths.get("app").resolve(CLIENT_RELATIVE_PATH);
        return new String(Files.readAllBytes(repoRelative), StandardCharsets.UTF_8);
    }

    @Test
    public void onStartProcessesOpenTagsForCurrentSessionAfterRestoringVisibility() throws IOException {
        String source = readClientSource();
        int onStartIndex = source.indexOf("public void onStart()");
        Assert.assertTrue(onStartIndex >= 0);
        int onResumeIndex = source.indexOf("public void onResume()", onStartIndex);
        Assert.assertTrue(onResumeIndex > onStartIndex);
        String onStartBody = source.substring(onStartIndex, onResumeIndex);
        Assert.assertTrue(onStartBody.contains("openTagsForSession(mActivity.getCurrentSession())"));
    }

    @Test
    public void onStartProcessesOpenTagsAfterScreenUpdate() throws IOException {
        String source = readClientSource();
        int onStartIndex = source.indexOf("public void onStart()");
        Assert.assertTrue(onStartIndex >= 0);
        int onResumeIndex = source.indexOf("public void onResume()", onStartIndex);
        Assert.assertTrue(onResumeIndex > onStartIndex);
        String onStartBody = source.substring(onStartIndex, onResumeIndex);
        int screenUpdateIndex = onStartBody.indexOf("onScreenUpdated()");
        int openTagIndex = onStartBody.indexOf("openTagsForSession(mActivity.getCurrentSession())");
        Assert.assertTrue(screenUpdateIndex >= 0);
        Assert.assertTrue(openTagIndex > screenUpdateIndex);
    }
}
