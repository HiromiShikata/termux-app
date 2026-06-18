package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

public class ProjectActionTokenTest {

    @Test
    public void exposesNormalizedProjectNameAndAction() {
        ProjectActionToken token = new ProjectActionToken("xmile", ProjectAction.OVERVIEW_URL);

        Assert.assertEquals("xmile", token.getNormalizedProjectName());
        Assert.assertEquals(ProjectAction.OVERVIEW_URL, token.getAction());
    }

    @Test
    public void equalsReturnsTrueForSameInstance() {
        ProjectActionToken token = new ProjectActionToken("xmile", ProjectAction.OVERVIEW_URL);

        Assert.assertEquals(token, token);
    }

    @Test
    public void equalsReturnsTrueForSameProjectNameAndAction() {
        ProjectActionToken first = new ProjectActionToken("xmile", ProjectAction.OVERVIEW_URL);
        ProjectActionToken second = new ProjectActionToken("xmile", ProjectAction.OVERVIEW_URL);

        Assert.assertEquals(first, second);
    }

    @Test
    public void equalsReturnsFalseForDifferentProjectName() {
        ProjectActionToken first = new ProjectActionToken("xmile", ProjectAction.OVERVIEW_URL);
        ProjectActionToken second = new ProjectActionToken("umino", ProjectAction.OVERVIEW_URL);

        Assert.assertNotEquals(first, second);
    }

    @Test
    public void equalsReturnsFalseForDifferentAction() {
        ProjectActionToken first = new ProjectActionToken("xmile", ProjectAction.OVERVIEW_URL);
        ProjectActionToken second = new ProjectActionToken("xmile", ProjectAction.TDPM_CONSOLE_URL);

        Assert.assertNotEquals(first, second);
    }

    @Test
    public void equalsReturnsFalseForNull() {
        ProjectActionToken token = new ProjectActionToken("xmile", ProjectAction.OVERVIEW_URL);

        Assert.assertNotEquals(token, null);
    }

    @Test
    public void equalsReturnsFalseForUnrelatedType() {
        ProjectActionToken token = new ProjectActionToken("xmile", ProjectAction.OVERVIEW_URL);

        Assert.assertNotEquals(token, "xmile:overviewUrl");
    }

    @Test
    public void hashCodeIsEqualForEqualTokens() {
        ProjectActionToken first = new ProjectActionToken("xmile", ProjectAction.OVERVIEW_URL);
        ProjectActionToken second = new ProjectActionToken("xmile", ProjectAction.OVERVIEW_URL);

        Assert.assertEquals(first.hashCode(), second.hashCode());
    }

    @Test
    public void toStringContainsProjectNameAndAction() {
        ProjectActionToken token = new ProjectActionToken("xmile", ProjectAction.NEW_ISSUE_URL);

        Assert.assertEquals("ProjectActionToken{xmile:NEW_ISSUE_URL}", token.toString());
    }
}
