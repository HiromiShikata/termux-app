package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

public class SessionNameBarContentTest {

    @Test
    public void shortensGithubUrlNameToPathAfterHost() {
        SessionNameBarContent content = SessionNameBarContent.of(
            "https://github.com/HiromiShikata/termux-app/issues/440", null);
        Assert.assertEquals("HiromiShikata/termux-app/issues/440", content.getName());
    }

    @Test
    public void keepsNonGithubUrlNameUnchanged() {
        SessionNameBarContent content = SessionNameBarContent.of("https://example.com/foo/bar", null);
        Assert.assertEquals("https://example.com/foo/bar", content.getName());
    }

    @Test
    public void hasNoTitleWhenTitleIsNull() {
        SessionNameBarContent content = SessionNameBarContent.of(
            "https://github.com/HiromiShikata/termux-app/issues/440", null);
        Assert.assertFalse(content.hasTitle());
        Assert.assertEquals("HiromiShikata/termux-app/issues/440", content.getText());
        Assert.assertEquals(-1, content.getTitleStart());
        Assert.assertEquals(-1, content.getTitleEnd());
    }

    @Test
    public void hasNoTitleWhenTitleIsBlank() {
        SessionNameBarContent content = SessionNameBarContent.of(
            "https://github.com/HiromiShikata/termux-app/issues/440", "   ");
        Assert.assertFalse(content.hasTitle());
    }

    @Test
    public void buildsNameAndTitleTextWithTitleBeneathName() {
        SessionNameBarContent content = SessionNameBarContent.of(
            "https://github.com/HiromiShikata/termux-app/issues/440", "Fix the header bar");
        Assert.assertTrue(content.hasTitle());
        Assert.assertEquals("HiromiShikata/termux-app/issues/440", content.getName());
        Assert.assertEquals("Fix the header bar", content.getTitle());
        Assert.assertEquals("HiromiShikata/termux-app/issues/440\nFix the header bar", content.getText());
    }

    @Test
    public void titleSpanCoversOnlyTheTitlePortion() {
        SessionNameBarContent content = SessionNameBarContent.of(
            "https://github.com/HiromiShikata/termux-app/issues/440", "Fix the header bar");
        String name = "HiromiShikata/termux-app/issues/440";
        Assert.assertEquals(name.length() + 1, content.getTitleStart());
        Assert.assertEquals(content.getText().length(), content.getTitleEnd());
        Assert.assertEquals("Fix the header bar",
            content.getText().substring(content.getTitleStart(), content.getTitleEnd()));
    }

    @Test
    public void trimsSurroundingWhitespaceFromTitle() {
        SessionNameBarContent content = SessionNameBarContent.of("session-name", "  trimmed title  ");
        Assert.assertEquals("trimmed title", content.getTitle());
    }
}
