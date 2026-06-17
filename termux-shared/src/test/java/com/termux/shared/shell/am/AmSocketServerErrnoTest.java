package com.termux.shared.shell.am;

import com.termux.shared.errors.Errno;
import com.termux.shared.errors.Error;

import org.junit.Assert;
import org.junit.Test;

public class AmSocketServerErrnoTest {

    @Test
    public void typeConstantIsAmSocketServerError() {
        Assert.assertEquals("AmSocketServer Error", AmSocketServerErrno.TYPE);
        Assert.assertEquals(AmSocketServerErrno.TYPE, AmSocketServerErrno.ERRNO_PARSE_AM_COMMAND_FAILED_WITH_EXCEPTION.getType());
    }

    @Test
    public void amCommandErrnosHaveExpectedCodes() {
        Assert.assertEquals(100, AmSocketServerErrno.ERRNO_PARSE_AM_COMMAND_FAILED_WITH_EXCEPTION.getCode());
        Assert.assertEquals(101, AmSocketServerErrno.ERRNO_RUN_AM_COMMAND_FAILED_WITH_EXCEPTION.getCode());
    }

    @Test
    public void getErrorFormatsCommandAndException() {
        Error error = AmSocketServerErrno.ERRNO_PARSE_AM_COMMAND_FAILED_WITH_EXCEPTION.getError("am start", "bad");
        Assert.assertEquals(AmSocketServerErrno.TYPE, error.getType());
        Assert.assertEquals("Parse am command `am start` failed.\nException: bad", error.getMessage());
    }

    @Test
    public void valueOfResolvesRegisteredAmSocketServerErrno() {
        Errno expected = AmSocketServerErrno.ERRNO_RUN_AM_COMMAND_FAILED_WITH_EXCEPTION;
        Errno resolved = Errno.valueOf(AmSocketServerErrno.TYPE, 101);
        Assert.assertSame(expected, resolved);
    }
}
