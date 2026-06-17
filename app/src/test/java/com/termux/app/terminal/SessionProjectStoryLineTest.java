package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

public class SessionProjectStoryLineTest {

    @Test
    public void joinsProjectAndStoryWithMiddleDotSeparator() {
        SessionProjectStoryLine line = SessionProjectStoryLine.of("termux-app", "Header redesign");
        Assert.assertTrue(line.hasContent());
        Assert.assertEquals("termux-app · Header redesign", line.getText());
    }

    @Test
    public void showsOnlyProjectWhenStoryIsNull() {
        SessionProjectStoryLine line = SessionProjectStoryLine.of("termux-app", null);
        Assert.assertTrue(line.hasContent());
        Assert.assertEquals("termux-app", line.getText());
    }

    @Test
    public void showsOnlyStoryWhenProjectIsNull() {
        SessionProjectStoryLine line = SessionProjectStoryLine.of(null, "Header redesign");
        Assert.assertTrue(line.hasContent());
        Assert.assertEquals("Header redesign", line.getText());
    }

    @Test
    public void showsOnlyProjectWhenStoryIsBlank() {
        SessionProjectStoryLine line = SessionProjectStoryLine.of("termux-app", "   ");
        Assert.assertTrue(line.hasContent());
        Assert.assertEquals("termux-app", line.getText());
    }

    @Test
    public void hasNoContentWhenBothAreNull() {
        SessionProjectStoryLine line = SessionProjectStoryLine.of(null, null);
        Assert.assertFalse(line.hasContent());
        Assert.assertEquals("", line.getText());
    }

    @Test
    public void hasNoContentWhenBothAreBlank() {
        SessionProjectStoryLine line = SessionProjectStoryLine.of("  ", "\t");
        Assert.assertFalse(line.hasContent());
        Assert.assertEquals("", line.getText());
    }

    @Test
    public void trimsSurroundingWhitespaceFromProjectAndStory() {
        SessionProjectStoryLine line = SessionProjectStoryLine.of("  termux-app  ", "  Header redesign  ");
        Assert.assertEquals("termux-app · Header redesign", line.getText());
    }
}
