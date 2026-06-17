package com.termux.shared.jni.models;

import org.junit.Assert;
import org.junit.Test;

public class JniResultTest {

    @Test
    public void threeArgConstructorStoresFieldsAndDefaultsIntData() {
        JniResult result = new JniResult(0, 0, "ok");
        Assert.assertEquals(0, result.retval);
        Assert.assertEquals(0, result.errno);
        Assert.assertEquals("ok", result.errmsg);
        Assert.assertEquals(0, result.intData);
    }

    @Test
    public void fourArgConstructorStoresIntData() {
        JniResult result = new JniResult(0, 0, null, 42);
        Assert.assertEquals(42, result.intData);
    }

    @Test
    public void throwableConstructorSetsFailureRetvalAndMessage() {
        JniResult result = new JniResult("boom", new IllegalStateException("bad"));
        Assert.assertEquals(-1, result.retval);
        Assert.assertEquals(0, result.errno);
        Assert.assertTrue(result.errmsg.startsWith("boom"));
        Assert.assertTrue(result.errmsg.contains("IllegalStateException"));
    }

    @Test
    public void getErrorStringIncludesRetval() {
        JniResult result = new JniResult(0, 0, null);
        String text = result.getErrorString();
        Assert.assertTrue(text.contains("Retval"));
        Assert.assertTrue(text.contains("0"));
    }

    @Test
    public void getErrorStringIncludesErrnoAndErrmsgWhenSet() {
        JniResult result = new JniResult(-1, 2, "No such file");
        String text = result.getErrorString();
        Assert.assertTrue(text.contains("Errno"));
        Assert.assertTrue(text.contains("Errmsg"));
        Assert.assertTrue(text.contains("No such file"));
    }

    @Test
    public void getErrorStringOmitsErrnoWhenZero() {
        JniResult result = new JniResult(-1, 0, null);
        String text = result.getErrorString();
        Assert.assertFalse(text.contains("Errno"));
        Assert.assertFalse(text.contains("Errmsg"));
    }

    @Test
    public void staticGetErrorStringReturnsNullLiteralForNullResult() {
        Assert.assertEquals("null", JniResult.getErrorString(null));
    }

    @Test
    public void staticGetErrorStringDelegatesToInstance() {
        JniResult result = new JniResult(5, 0, null);
        Assert.assertEquals(result.getErrorString(), JniResult.getErrorString(result));
    }
}
