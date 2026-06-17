package com.termux.shared.net.socket.local;

import com.termux.shared.errors.Errno;
import com.termux.shared.errors.Error;

import org.junit.Assert;
import org.junit.Test;

public class LocalSocketErrnoTest {

    @Test
    public void typeConstantIsLocalSocketError() {
        Assert.assertEquals("LocalSocket Error", LocalSocketErrno.TYPE);
        Assert.assertEquals(LocalSocketErrno.TYPE, LocalSocketErrno.ERRNO_START_LOCAL_SOCKET_LIB_LOAD_FAILED_WITH_EXCEPTION.getType());
    }

    @Test
    public void libLoadErrnoExposesCodeAndFormatsMessage() {
        Assert.assertEquals(100, LocalSocketErrno.ERRNO_START_LOCAL_SOCKET_LIB_LOAD_FAILED_WITH_EXCEPTION.getCode());
        Error error = LocalSocketErrno.ERRNO_START_LOCAL_SOCKET_LIB_LOAD_FAILED_WITH_EXCEPTION.getError("libtermux", "missing");
        Assert.assertEquals("Failed to load \"libtermux\" library.\nException: missing", error.getMessage());
    }

    @Test
    public void serverSocketErrnosCoverTheExpectedCodeRange() {
        Assert.assertEquals(150, LocalSocketErrno.ERRNO_SERVER_SOCKET_PATH_NULL_OR_EMPTY.getCode());
        Assert.assertEquals(151, LocalSocketErrno.ERRNO_SERVER_SOCKET_PATH_TOO_LONG.getCode());
        Assert.assertEquals(152, LocalSocketErrno.ERRNO_SERVER_SOCKET_PATH_NOT_ABSOLUTE.getCode());
        Assert.assertEquals(153, LocalSocketErrno.ERRNO_SERVER_SOCKET_BACKLOG_INVALID.getCode());
        Assert.assertEquals(154, LocalSocketErrno.ERRNO_CREATE_SERVER_SOCKET_FAILED.getCode());
    }

    @Test
    public void clientSocketErrnosCoverTheExpectedCodeRange() {
        Assert.assertEquals(200, LocalSocketErrno.ERRNO_SET_CLIENT_SOCKET_READ_TIMEOUT_FAILED.getCode());
        Assert.assertEquals(204, LocalSocketErrno.ERRNO_SEND_DATA_TO_CLIENT_SOCKET_FAILED.getCode());
        Assert.assertEquals(208, LocalSocketErrno.ERRNO_USING_CLIENT_SOCKET_WITH_INVALID_FD.getCode());
    }

    @Test
    public void pathTooLongErrnoFormatsBothArguments() {
        Error error = LocalSocketErrno.ERRNO_SERVER_SOCKET_PATH_TOO_LONG.getError("server", "/very/long/path");
        Assert.assertTrue(error.getMessage().contains("\"server\""));
        Assert.assertTrue(error.getMessage().contains("\"/very/long/path\""));
        Assert.assertTrue(error.getMessage().contains("108 bytes"));
    }

    @Test
    public void valueOfResolvesRegisteredLocalSocketErrno() {
        Errno expected = LocalSocketErrno.ERRNO_START_LOCAL_SOCKET_LIB_LOAD_FAILED_WITH_EXCEPTION;
        Errno resolved = Errno.valueOf(LocalSocketErrno.TYPE, 100);
        Assert.assertSame(expected, resolved);
    }
}
