package com.termux.shared.errors;

import org.junit.Assert;
import org.junit.Test;

public class FunctionErrnoTest {

    @Test
    public void typeConstantMatchesErrnoType() {
        Assert.assertEquals("Function Error", FunctionErrno.TYPE);
        Assert.assertEquals(FunctionErrno.TYPE, FunctionErrno.ERRNO_NULL_OR_EMPTY_PARAMETER.getType());
    }

    @Test
    public void nullOrEmptyParameterErrnoExposesCodeAndMessage() {
        Assert.assertEquals(100, FunctionErrno.ERRNO_NULL_OR_EMPTY_PARAMETER.getCode());
        Assert.assertTrue(FunctionErrno.ERRNO_NULL_OR_EMPTY_PARAMETER.getMessage().contains("null or empty"));
    }

    @Test
    public void unsetParameterCodesAreDistinct() {
        Assert.assertEquals(102, FunctionErrno.ERRNO_UNSET_PARAMETER.getCode());
        Assert.assertEquals(103, FunctionErrno.ERRNO_UNSET_PARAMETERS.getCode());
    }

    @Test
    public void getErrorFormatsParameterAndFunctionNames() {
        Error error = FunctionErrno.ERRNO_NULL_OR_EMPTY_PARAMETER.getError("path", "openFile");
        Assert.assertEquals(FunctionErrno.TYPE, error.getType());
        Assert.assertEquals(Integer.valueOf(100), error.getCode());
        Assert.assertEquals("The path parameter passed to \"openFile\" is null or empty.", error.getMessage());
    }

    @Test
    public void parameterNotInstanceOfErrnoIsRegistered() {
        Error error = FunctionErrno.ERRNO_PARAMETER_NOT_INSTANCE_OF.getError("value", "run", "String");
        Assert.assertTrue(error.getMessage().contains("is not an instance of String"));
    }

    @Test
    public void valueOfResolvesRegisteredFunctionErrno() {
        Errno expected = FunctionErrno.ERRNO_UNSET_PARAMETER;
        Errno resolved = Errno.valueOf(FunctionErrno.TYPE, 102);
        Assert.assertSame(expected, resolved);
    }
}
