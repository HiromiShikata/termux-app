package com.termux.shared.errors;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class ErrorBehaviorTest {

    private static final int FAILED = Errno.ERRNO_FAILED.getCode();
    private static final int SUCCESS = Errno.ERRNO_SUCCESS.getCode();

    @Test
    public void constructorWithTypeCodeMessageThrowableRetainsAllValues() {
        RuntimeException throwable = new RuntimeException("cause");
        Error error = new Error("CustomType", FAILED, "msg", throwable);

        Assert.assertEquals("CustomType", error.getType());
        Assert.assertEquals(Integer.valueOf(FAILED), error.getCode());
        Assert.assertEquals("msg", error.getMessage());
        Assert.assertEquals(1, error.getThrowablesList().size());
        Assert.assertSame(throwable, error.getThrowablesList().get(0));
    }

    @Test
    public void constructorWithTypeCodeMessageThrowablesListRetainsList() {
        List<Throwable> throwables = Arrays.asList(new RuntimeException("a"), new IllegalStateException("b"));
        Error error = new Error("CustomType", FAILED, "msg", throwables);

        Assert.assertEquals(2, error.getThrowablesList().size());
    }

    @Test
    public void constructorWithCodeMessageThrowableUsesErrnoType() {
        Error error = new Error(FAILED, "msg", new RuntimeException("cause"));

        Assert.assertEquals(Errno.TYPE, error.getType());
        Assert.assertEquals(1, error.getThrowablesList().size());
    }

    @Test
    public void constructorWithCodeMessageThrowablesListUsesErrnoType() {
        Error error = new Error(FAILED, "msg", Collections.singletonList(new RuntimeException("cause")));

        Assert.assertEquals(Errno.TYPE, error.getType());
        Assert.assertEquals(1, error.getThrowablesList().size());
    }

    @Test
    public void constructorWithMessageThrowableHasSuccessCodeUntilFailed() {
        Error error = new Error("just a message", new RuntimeException("cause"));

        Assert.assertEquals(Integer.valueOf(SUCCESS), error.getCode());
        Assert.assertEquals("just a message", error.getMessage());
        Assert.assertEquals(1, error.getThrowablesList().size());
        Assert.assertFalse(error.isStateFailed());
    }

    @Test
    public void constructorWithMessageThrowablesListRetainsList() {
        Error error = new Error("msg", Arrays.asList(new RuntimeException("a"), new RuntimeException("b")));

        Assert.assertEquals(2, error.getThrowablesList().size());
    }

    @Test
    public void constructorWithMessageOnlyHasNullTypeFallbackAndNoThrowables() {
        Error error = new Error("only message");

        Assert.assertEquals(Errno.TYPE, error.getType());
        Assert.assertEquals("only message", error.getMessage());
        Assert.assertTrue(error.getThrowablesList().isEmpty());
    }

    @Test
    public void setStateFailedFromErrorCopiesTypeCodeAndMessage() {
        Error source = new Error("SrcType", FAILED, "source message");
        Error target = new Error();

        Assert.assertTrue(target.setStateFailed(source));
        Assert.assertEquals("SrcType", target.getType());
        Assert.assertEquals(Integer.valueOf(FAILED), target.getCode());
        Assert.assertEquals("source message", target.getMessage());
    }

    @Test
    public void setStateFailedFromErrorWithThrowableCopiesThrowable() {
        Error source = new Error("SrcType", FAILED, "source message");
        Error target = new Error();
        RuntimeException throwable = new RuntimeException("boom");

        Assert.assertTrue(target.setStateFailed(source, throwable));
        Assert.assertEquals(1, target.getThrowablesList().size());
        Assert.assertSame(throwable, target.getThrowablesList().get(0));
    }

    @Test
    public void setStateFailedFromErrorWithThrowablesListCopiesList() {
        Error source = new Error("SrcType", FAILED, "source message");
        Error target = new Error();
        List<Throwable> throwables = Arrays.asList(new RuntimeException("a"), new RuntimeException("b"));

        Assert.assertTrue(target.setStateFailed(source, throwables));
        Assert.assertEquals(2, target.getThrowablesList().size());
    }

    @Test
    public void setStateFailedWithCodeMessageThrowableCopiesThrowable() {
        Error error = new Error();
        RuntimeException throwable = new RuntimeException("boom");

        Assert.assertTrue(error.setStateFailed(FAILED, "msg", throwable));
        Assert.assertEquals(1, error.getThrowablesList().size());
    }

    @Test
    public void setStateFailedWithInvalidCodeForcesFailedCodeAndReturnsFalse() {
        Error error = new Error();

        Assert.assertFalse(error.setStateFailed(SUCCESS, "should be forced"));
        Assert.assertEquals(Integer.valueOf(Errno.ERRNO_FAILED.getCode()), error.getCode());
        Assert.assertTrue(error.isStateFailed());
    }

    @Test
    public void setStateFailedWithExplicitTypeOverridesType() {
        Error error = new Error();

        Assert.assertTrue(error.setStateFailed("OverrideType", FAILED, "msg", null));
        Assert.assertEquals("OverrideType", error.getType());
    }

    @Test
    public void setStateFailedWithNullTypeKeepsExistingType() {
        Error error = new Error("ExistingType", FAILED, "msg");

        Assert.assertTrue(error.setStateFailed((String) null, FAILED, "new msg", null));
        Assert.assertEquals("ExistingType", error.getType());
        Assert.assertEquals("new msg", error.getMessage());
    }

    @Test
    public void appendMessageIsIgnoredWhenStateNotFailed() {
        Error error = new Error();
        error.appendMessage("ignored");
        Assert.assertNull(error.getMessage());
    }

    @Test
    public void appendAndPrependIgnoreNullMessageArgument() {
        Error error = new Error(FAILED, "core");
        error.appendMessage(null);
        error.prependMessage(null);
        Assert.assertEquals("core", error.getMessage());
    }

    @Test
    public void getMinimalErrorStringInstanceMatchesStaticForSameError() {
        Error error = new Error("CustomType", FAILED, "boom");
        Assert.assertEquals(error.getMinimalErrorString(), Error.getMinimalErrorString(error));
    }

    @Test
    public void getMinimalErrorLogStringInstanceContainsCodeAndMessage() {
        Error error = new Error(FAILED, "boom");
        String logString = error.getMinimalErrorLogString();
        Assert.assertTrue(logString.contains("Error Code"));
        Assert.assertTrue(logString.contains("boom"));
    }

    @Test
    public void getMinimalErrorLogStringStaticMatchesInstance() {
        Error error = new Error(FAILED, "boom");
        Assert.assertEquals(error.getMinimalErrorLogString(), Error.getMinimalErrorLogString(error));
    }

    @Test
    public void getErrorLogStringStaticMatchesInstance() {
        Error error = new Error(FAILED, "boom");
        Assert.assertEquals(error.getErrorLogString(), Error.getErrorLogString(error));
    }

    @Test
    public void getErrorMarkdownStringStaticMatchesInstance() {
        Error error = new Error(FAILED, "boom");
        Assert.assertEquals(error.getErrorMarkdownString(), Error.getErrorMarkdownString(error));
    }

    @Test
    public void getErrorLogStringIncludesStackTracesWhenThrowablesPresent() {
        Error error = new Error(FAILED, "boom", new RuntimeException("trace-marker"));
        String logString = error.getErrorLogString();
        Assert.assertTrue(logString.contains("StackTraces"));
        Assert.assertTrue(logString.contains("trace-marker"));
    }

    @Test
    public void getErrorMarkdownStringIncludesStackTracesWhenThrowablesPresent() {
        Error error = new Error(FAILED, "boom", new RuntimeException("trace-marker"));
        String markdownString = error.getErrorMarkdownString();
        Assert.assertTrue(markdownString.contains("StackTraces"));
        Assert.assertTrue(markdownString.contains("trace-marker"));
    }

    @Test
    public void getErrorMarkdownStringUsesTypeAnnotationForNonErrnoType() {
        Error error = new Error("CustomType", FAILED, "boom");
        Assert.assertTrue(error.getErrorMarkdownString().contains("CustomType"));
    }

    @Test
    public void getTypeAndMessageLogStringAnnotatesNonErrnoType() {
        Error error = new Error("CustomType", FAILED, "boom");
        Assert.assertTrue(error.getTypeAndMessageLogString().contains("CustomType"));
    }

    @Test
    public void getTypeAndMessageLogStringOmitsTypeForErrnoType() {
        Error error = new Error(FAILED, "boom");
        String logString = error.getTypeAndMessageLogString();
        Assert.assertTrue(logString.contains("Error Message"));
        Assert.assertFalse(logString.contains("Error Message ("));
    }

    @Test
    public void getCodeStringContainsErrorCodeLabelAndValue() {
        Error error = new Error(FAILED, "boom");
        String codeString = error.getCodeString();
        Assert.assertTrue(codeString.contains("Error Code"));
        Assert.assertTrue(codeString.contains(String.valueOf(FAILED)));
    }

    @Test
    public void toStringMatchesErrorLogString() {
        Error error = new Error(FAILED, "boom");
        Assert.assertEquals(error.getErrorLogString(), error.toString());
    }

    @Test
    public void getMinimalErrorLogStringStaticReturnsNullLiteralForNull() {
        Assert.assertEquals("null", Error.getMinimalErrorLogString(null));
    }

    @Test
    public void logErrorAndShowToastStaticIgnoresNullError() {
        Error.logErrorAndShowToast(null, "tag", null);
    }
}
