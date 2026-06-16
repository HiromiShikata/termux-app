package com.termux.app.models;

import org.junit.Assert;
import org.junit.Test;

public class UserActionTest {

    @Test
    public void aboutActionExposesItsName() {
        Assert.assertEquals("about", UserAction.ABOUT.getName());
    }

    @Test
    public void valueOfResolvesEnumConstant() {
        Assert.assertSame(UserAction.ABOUT, UserAction.valueOf("ABOUT"));
    }

    @Test
    public void valuesContainsAboutAction() {
        UserAction[] values = UserAction.values();

        Assert.assertEquals(1, values.length);
        Assert.assertSame(UserAction.ABOUT, values[0]);
    }
}
