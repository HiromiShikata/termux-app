package com.termux.shared.termux.extrakeys;

import org.junit.Assert;
import org.junit.Test;

public class SpecialButtonTest {

    @Test
    public void predefinedSpecialButtonsExposeTheirKeys() {
        Assert.assertEquals("CTRL", SpecialButton.CTRL.getKey());
        Assert.assertEquals("ALT", SpecialButton.ALT.getKey());
        Assert.assertEquals("SHIFT", SpecialButton.SHIFT.getKey());
        Assert.assertEquals("FN", SpecialButton.FN.getKey());
    }

    @Test
    public void valueOfResolvesRegisteredPredefinedButtons() {
        Assert.assertSame(SpecialButton.CTRL, SpecialButton.valueOf("CTRL"));
        Assert.assertSame(SpecialButton.FN, SpecialButton.valueOf("FN"));
    }

    @Test
    public void valueOfReturnsNullForUnknownKey() {
        Assert.assertNull(SpecialButton.valueOf("UNKNOWN"));
    }

    @Test
    public void toStringReturnsKey() {
        Assert.assertEquals("ALT", SpecialButton.ALT.toString());
    }

    @Test
    public void constructingSpecialButtonRegistersItForLookup() {
        SpecialButton custom = new SpecialButton("META");
        Assert.assertSame(custom, SpecialButton.valueOf("META"));
        Assert.assertEquals("META", custom.getKey());
    }
}
