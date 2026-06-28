package com.termux.app.browser;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class BrowserProjectOverviewActionButtonsStyleTest {

    private static final String LAYOUT_RELATIVE_PATH =
        "src/main/res/layout/activity_termux.xml";

    private String readLayout() throws IOException {
        Path moduleRelative = Paths.get(LAYOUT_RELATIVE_PATH);
        if (Files.exists(moduleRelative)) {
            return new String(Files.readAllBytes(moduleRelative), StandardCharsets.UTF_8);
        }
        Path repoRelative = Paths.get("app").resolve(LAYOUT_RELATIVE_PATH);
        return new String(Files.readAllBytes(repoRelative), StandardCharsets.UTF_8);
    }

    private String readActionsGroup() throws IOException {
        String layout = readLayout();
        int start = layout.indexOf("@+id/browser_project_overview_actions");
        Assert.assertTrue(start >= 0);
        int end = layout.indexOf("</LinearLayout>", start);
        Assert.assertTrue(end > start);
        return layout.substring(start, end);
    }

    @Test
    public void openAllButtonIsACompactRoundFloatingActionButton() throws IOException {
        String group = readActionsGroup();
        int buttonIndex = group.indexOf("@+id/browser_open_all_tasks_button");
        Assert.assertTrue(buttonIndex >= 0);
        int elementStart =
            group.lastIndexOf("<com.google.android.material.floatingactionbutton", buttonIndex);
        int elementEnd = group.indexOf("/>", buttonIndex);
        Assert.assertTrue(elementStart >= 0);
        Assert.assertTrue(elementEnd > buttonIndex);
        String element = group.substring(elementStart, elementEnd);
        Assert.assertTrue(element.contains(
            "com.google.android.material.floatingactionbutton.FloatingActionButton"));
        Assert.assertFalse(element.contains("ExtendedFloatingActionButton"));
        Assert.assertTrue(element.contains("app:fabSize=\"mini\""));
        Assert.assertTrue(element.contains("app:srcCompat=\"@drawable/ic_browser_open_all_tasks\""));
        Assert.assertTrue(element.contains(
            "android:contentDescription=\"@string/action_browser_open_all_tasks\""));
        Assert.assertFalse(element.contains("android:text="));
    }

    @Test
    public void openFirstTenButtonIsACompactRoundFloatingActionButton() throws IOException {
        String group = readActionsGroup();
        int buttonIndex = group.indexOf("@+id/browser_open_first_ten_tasks_button");
        Assert.assertTrue(buttonIndex >= 0);
        int elementStart =
            group.lastIndexOf("<com.google.android.material.floatingactionbutton", buttonIndex);
        int elementEnd = group.indexOf("/>", buttonIndex);
        Assert.assertTrue(elementStart >= 0);
        Assert.assertTrue(elementEnd > buttonIndex);
        String element = group.substring(elementStart, elementEnd);
        Assert.assertTrue(element.contains(
            "com.google.android.material.floatingactionbutton.FloatingActionButton"));
        Assert.assertFalse(element.contains("ExtendedFloatingActionButton"));
        Assert.assertTrue(element.contains("app:fabSize=\"mini\""));
        Assert.assertTrue(element.contains(
            "app:srcCompat=\"@drawable/ic_browser_open_first_ten_tasks\""));
        Assert.assertTrue(element.contains(
            "android:contentDescription=\"@string/action_browser_open_first_ten_tasks\""));
        Assert.assertFalse(element.contains("android:text="));
    }

    @Test
    public void overviewActionButtonsDoNotUseExtendedFloatingActionButton() throws IOException {
        String group = readActionsGroup();
        Assert.assertFalse(group.contains("ExtendedFloatingActionButton"));
    }

    @Test
    public void actionIconDrawablesExist() {
        Assert.assertTrue(Files.exists(resolveDrawable("ic_browser_open_all_tasks.xml")));
        Assert.assertTrue(Files.exists(resolveDrawable("ic_browser_open_first_ten_tasks.xml")));
    }

    private Path resolveDrawable(String fileName) {
        Path moduleRelative = Paths.get("src/main/res/drawable").resolve(fileName);
        if (Files.exists(moduleRelative)) {
            return moduleRelative;
        }
        return Paths.get("app/src/main/res/drawable").resolve(fileName);
    }
}
