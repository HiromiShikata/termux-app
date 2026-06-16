package com.termux.shared.errors;

import org.junit.Assert;
import org.junit.Test;

public class ErrnoTest {

    @Test
    public void successErrnoExposesTypeCodeAndMessage() {
        Assert.assertEquals(Errno.TYPE, Errno.ERRNO_SUCCESS.getType());
        Assert.assertEquals("Success", Errno.ERRNO_SUCCESS.getMessage());
    }

    @Test
    public void failedErrnoCodeIsGreaterThanSuccessCode() {
        Assert.assertTrue(Errno.ERRNO_FAILED.getCode() > Errno.ERRNO_SUCCESS.getCode());
    }

    @Test
    public void toStringContainsTypeCodeAndMessage() {
        String text = Errno.ERRNO_FAILED.toString();
        Assert.assertTrue(text.contains("type=" + Errno.TYPE));
        Assert.assertTrue(text.contains("code=" + Errno.ERRNO_FAILED.getCode()));
        Assert.assertTrue(text.contains("Failed"));
    }

    @Test
    public void valueOfResolvesRegisteredErrno() {
        Errno resolved = Errno.valueOf(Errno.TYPE, Errno.ERRNO_FAILED.getCode());
        Assert.assertSame(Errno.ERRNO_FAILED, resolved);
    }

    @Test
    public void valueOfReturnsNullForNullType() {
        Assert.assertNull(Errno.valueOf(null, 1));
    }

    @Test
    public void valueOfReturnsNullForEmptyType() {
        Assert.assertNull(Errno.valueOf("", 1));
    }

    @Test
    public void valueOfReturnsNullForNullCode() {
        Assert.assertNull(Errno.valueOf(Errno.TYPE, null));
    }

    @Test
    public void valueOfReturnsNullForUnknownCode() {
        Assert.assertNull(Errno.valueOf(Errno.TYPE, 999999));
    }

    @Test
    public void getErrorBuildsErrorWithSameTypeCodeAndMessage() {
        Error error = Errno.ERRNO_FAILED.getError();
        Assert.assertEquals(Errno.ERRNO_FAILED.getType(), error.getType());
        Assert.assertEquals(Integer.valueOf(Errno.ERRNO_FAILED.getCode()), error.getCode());
        Assert.assertEquals(Errno.ERRNO_FAILED.getMessage(), error.getMessage());
    }

    @Test
    public void getErrorWithArgsFormatsMessage() {
        Errno formatted = new Errno("ErrnoTestType", 9100, "value is %s");
        Error error = formatted.getError("42");
        Assert.assertEquals("value is 42", error.getMessage());
    }

    @Test
    public void equalsErrorTypeAndCodeTrueForMatchingError() {
        Error error = Errno.ERRNO_FAILED.getError();
        Assert.assertTrue(Errno.ERRNO_FAILED.equalsErrorTypeAndCode(error));
    }

    @Test
    public void equalsErrorTypeAndCodeFalseForNullError() {
        Assert.assertFalse(Errno.ERRNO_FAILED.equalsErrorTypeAndCode(null));
    }

    @Test
    public void equalsErrorTypeAndCodeFalseForDifferentCode() {
        Error successError = Errno.ERRNO_SUCCESS.getError();
        Assert.assertFalse(Errno.ERRNO_FAILED.equalsErrorTypeAndCode(successError));
    }
}
