package com.termux.shared.errors;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class ErrnoBehaviorTest {

    @Test
    public void gettersExposeConstructorValues() {
        Errno errno = new Errno("CustomType", 4242, "custom message");
        Assert.assertEquals("CustomType", errno.getType());
        Assert.assertEquals(4242, errno.getCode());
        Assert.assertEquals("custom message", errno.getMessage());
    }

    @Test
    public void getErrorWithoutArgsCopiesTypeCodeMessage() {
        Errno errno = new Errno("CustomType", 4243, "plain message");
        Error error = errno.getError();
        Assert.assertEquals("CustomType", error.getType());
        Assert.assertEquals(Integer.valueOf(4243), error.getCode());
        Assert.assertEquals("plain message", error.getMessage());
    }

    @Test
    public void getErrorWithArgsFormatsMessage() {
        Errno errno = new Errno("CustomType", 4244, "value is %1$s and %2$s");
        Error error = errno.getError("alpha", "beta");
        Assert.assertEquals("value is alpha and beta", error.getMessage());
    }

    @Test
    public void getErrorWithMalformedFormatFallsBackToUnformattedMessageWithArgs() {
        Errno errno = new Errno("CustomType", 4245, "broken %1$d format");
        Error error = errno.getError("not-a-number");
        Assert.assertTrue(error.getMessage().contains("broken %1$d format"));
        Assert.assertTrue(error.getMessage().contains("not-a-number"));
    }

    @Test
    public void getErrorWithNullThrowableDelegatesToArgsOnlyVariant() {
        Errno errno = new Errno("CustomType", 4246, "message %1$s");
        Error error = errno.getError((Throwable) null, "x");
        Assert.assertEquals("message x", error.getMessage());
        Assert.assertTrue(error.getThrowablesList().isEmpty());
    }

    @Test
    public void getErrorWithThrowableRecordsThrowable() {
        Errno errno = new Errno("CustomType", 4247, "message %1$s");
        RuntimeException throwable = new RuntimeException("cause");
        Error error = errno.getError(throwable, "x");
        Assert.assertEquals("message x", error.getMessage());
        Assert.assertEquals(1, error.getThrowablesList().size());
        Assert.assertSame(throwable, error.getThrowablesList().get(0));
    }

    @Test
    public void getErrorWithNullThrowablesListFormatsMessageWithoutThrowables() {
        Errno errno = new Errno("CustomType", 4248, "message %1$s");
        Error error = errno.getError((List<Throwable>) null, "x");
        Assert.assertEquals("message x", error.getMessage());
        Assert.assertTrue(error.getThrowablesList().isEmpty());
    }

    @Test
    public void getErrorWithThrowablesListRecordsAllThrowables() {
        Errno errno = new Errno("CustomType", 4249, "message %1$s");
        List<Throwable> throwables = Arrays.asList(new RuntimeException("a"), new IllegalStateException("b"));
        Error error = errno.getError(throwables, "x");
        Assert.assertEquals("message x", error.getMessage());
        Assert.assertEquals(2, error.getThrowablesList().size());
    }

    @Test
    public void getErrorWithThrowablesListAndMalformedFormatFallsBackButKeepsThrowables() {
        Errno errno = new Errno("CustomType", 4250, "broken %1$d");
        List<Throwable> throwables = Collections.singletonList(new RuntimeException("cause"));
        Error error = errno.getError(throwables, "not-a-number");
        Assert.assertTrue(error.getMessage().contains("broken %1$d"));
        Assert.assertTrue(error.getMessage().contains("not-a-number"));
        Assert.assertEquals(1, error.getThrowablesList().size());
    }

    @Test
    public void equalsErrorTypeAndCodeFalseForDifferentType() {
        Errno errno = new Errno("TypeA", 4251, "message");
        Error error = new Error("TypeB", 4251, "message");
        Assert.assertFalse(errno.equalsErrorTypeAndCode(error));
    }

    @Test
    public void equalsErrorTypeAndCodeTrueForMatchingTypeAndCode() {
        Errno errno = new Errno("TypeA", 4252, "message");
        Error error = errno.getError();
        Assert.assertTrue(errno.equalsErrorTypeAndCode(error));
    }
}
