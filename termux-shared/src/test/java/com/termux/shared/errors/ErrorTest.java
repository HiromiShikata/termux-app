package com.termux.shared.errors;

import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

public class ErrorTest {

    @Test
    public void defaultConstructorUsesSuccessCodeAndErrnoType() {
        Error error = new Error();
        Assert.assertEquals(Errno.TYPE, error.getType());
        Assert.assertEquals(Integer.valueOf(Errno.ERRNO_SUCCESS.getCode()), error.getCode());
        Assert.assertFalse(error.isStateFailed());
    }

    @Test
    public void constructorWithCodeAndMessageRetainsValues() {
        int code = Errno.ERRNO_FAILED.getCode();
        Error error = new Error(code, "boom");
        Assert.assertEquals(Integer.valueOf(code), error.getCode());
        Assert.assertEquals("boom", error.getMessage());
        Assert.assertTrue(error.isStateFailed());
    }

    @Test
    public void constructorWithExplicitTypeRetainsType() {
        Error error = new Error("CustomType", Errno.ERRNO_FAILED.getCode(), "msg");
        Assert.assertEquals("CustomType", error.getType());
    }

    @Test
    public void blankTypeFallsBackToErrnoType() {
        Error error = new Error("", Errno.ERRNO_FAILED.getCode(), "msg");
        Assert.assertEquals(Errno.TYPE, error.getType());
    }

    @Test
    public void codeAtOrBelowSuccessIsForcedToSuccess() {
        Error error = new Error(Errno.ERRNO_SUCCESS.getCode(), "msg");
        Assert.assertEquals(Integer.valueOf(Errno.ERRNO_SUCCESS.getCode()), error.getCode());
        Assert.assertFalse(error.isStateFailed());
    }

    @Test
    public void setLabelIsRetainedAndReturnsSameInstance() {
        Error error = new Error();
        Assert.assertSame(error, error.setLabel("label"));
        Assert.assertEquals("label", error.getLabel());
    }

    @Test
    public void prependMessageOnlyAppliesWhenStateFailed() {
        Error failed = new Error(Errno.ERRNO_FAILED.getCode(), "world");
        failed.prependMessage("hello ");
        Assert.assertEquals("hello world", failed.getMessage());
    }

    @Test
    public void prependMessageIsIgnoredWhenStateNotFailed() {
        Error ok = new Error();
        ok.prependMessage("hello ");
        Assert.assertNull(ok.getMessage());
    }

    @Test
    public void appendMessageOnlyAppliesWhenStateFailed() {
        Error failed = new Error(Errno.ERRNO_FAILED.getCode(), "hello");
        failed.appendMessage(" world");
        Assert.assertEquals("hello world", failed.getMessage());
    }

    @Test
    public void getThrowablesListReturnsUnmodifiableList() {
        Error error = new Error("msg", Collections.singletonList(new RuntimeException("x")));
        List<Throwable> list = error.getThrowablesList();
        Assert.assertEquals(1, list.size());
        Assert.assertThrows(UnsupportedOperationException.class,
            () -> list.add(new RuntimeException("y")));
    }

    @Test
    public void setStateFailedWithValidCodeReturnsTrueAndUpdatesState() {
        Error error = new Error();
        boolean result = error.setStateFailed(Errno.ERRNO_FAILED.getCode(), "failed message");
        Assert.assertTrue(result);
        Assert.assertTrue(error.isStateFailed());
        Assert.assertEquals("failed message", error.getMessage());
    }

    @Test
    public void getMinimalErrorStringFormatsCodeTypeAndMessage() {
        Error error = new Error("CustomType", Errno.ERRNO_FAILED.getCode(), "boom");
        Assert.assertEquals("(" + Errno.ERRNO_FAILED.getCode() + ") CustomType: boom",
            error.getMinimalErrorString());
    }

    @Test
    public void getMinimalErrorStringStaticReturnsNullLiteralForNull() {
        Assert.assertEquals("null", Error.getMinimalErrorString(null));
    }

    @Test
    public void getErrorLogStringStaticReturnsNullLiteralForNull() {
        Assert.assertEquals("null", Error.getErrorLogString(null));
    }

    @Test
    public void getMinimalErrorLogStringStaticReturnsNullLiteralForNull() {
        Assert.assertEquals("null", Error.getMinimalErrorLogString(null));
    }

    @Test
    public void getErrorMarkdownStringStaticReturnsNullLiteralForNull() {
        Assert.assertEquals("null", Error.getErrorMarkdownString(null));
    }

    @Test
    public void getErrorMarkdownStringContainsErrorCodeEntry() {
        Error error = new Error(Errno.ERRNO_FAILED.getCode(), "boom");
        Assert.assertTrue(error.getErrorMarkdownString().contains("Error Code"));
    }

    @Test
    public void getErrorLogStringContainsCodeAndMessage() {
        Error error = new Error(Errno.ERRNO_FAILED.getCode(), "boom");
        String logString = error.getErrorLogString();
        Assert.assertTrue(logString.contains("Error Code"));
        Assert.assertTrue(logString.contains("boom"));
    }
}
