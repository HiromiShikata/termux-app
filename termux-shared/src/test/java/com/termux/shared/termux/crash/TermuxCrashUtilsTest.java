package com.termux.shared.termux.crash;

import android.content.Context;
import android.os.Build;

import com.termux.shared.crash.CrashHandler;
import com.termux.shared.termux.TermuxConstants;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = Build.VERSION_CODES.P)
public class TermuxCrashUtilsTest {

    private Context context() {
        return RuntimeEnvironment.getApplication();
    }

    @Test
    public void typeEnumExposesBothCrashKinds() {
        Assert.assertEquals(2, TermuxCrashUtils.TYPE.values().length);
        Assert.assertEquals(TermuxCrashUtils.TYPE.UNCAUGHT_EXCEPTION, TermuxCrashUtils.TYPE.valueOf("UNCAUGHT_EXCEPTION"));
        Assert.assertEquals(TermuxCrashUtils.TYPE.CAUGHT_EXCEPTION, TermuxCrashUtils.TYPE.valueOf("CAUGHT_EXCEPTION"));
    }

    @Test
    public void getCrashHandlerReturnsHandlerWithTermuxCrashClient() {
        CrashHandler handler = TermuxCrashUtils.getCrashHandler(context());
        Assert.assertNotNull(handler);
    }

    @Test
    public void getCrashLogFilePathReturnsTermuxCrashLogPath() {
        TermuxCrashUtils client = new TermuxCrashUtils(TermuxCrashUtils.TYPE.CAUGHT_EXCEPTION);
        Assert.assertEquals(TermuxConstants.TERMUX_CRASH_LOG_FILE_PATH, client.getCrashLogFilePath(context()));
    }

    @Test
    public void onPreLogCrashAlwaysReturnsFalse() {
        TermuxCrashUtils client = new TermuxCrashUtils(TermuxCrashUtils.TYPE.CAUGHT_EXCEPTION);
        Assert.assertFalse(client.onPreLogCrash(context(), Thread.currentThread(), new RuntimeException("x")));
    }

    @Test
    public void getAppInfoMarkdownStringReturnsNonEmptyMarkdown() {
        TermuxCrashUtils client = new TermuxCrashUtils(TermuxCrashUtils.TYPE.CAUGHT_EXCEPTION);
        String markdown = client.getAppInfoMarkdownString(context());
        Assert.assertNotNull(markdown);
        Assert.assertFalse(markdown.isEmpty());
    }

    @Test
    public void onPostLogCrashWithNullContextIsNoOp() {
        TermuxCrashUtils client = new TermuxCrashUtils(TermuxCrashUtils.TYPE.CAUGHT_EXCEPTION);
        client.onPostLogCrash(null, Thread.currentThread(), new RuntimeException("x"));
    }

    @Test
    public void onPostLogCrashWithoutTermuxPackageContextReturnsQuietly() {
        TermuxCrashUtils client = new TermuxCrashUtils(TermuxCrashUtils.TYPE.CAUGHT_EXCEPTION);
        client.onPostLogCrash(context(), Thread.currentThread(), new RuntimeException("x"));
    }

    @Test
    public void logCrashWithNullThrowableIsNoOp() {
        TermuxCrashUtils.logCrash(context(), null);
    }

    @Test
    public void logCrashWithThrowableDoesNotPropagateException() {
        TermuxCrashUtils.logCrash(context(), new IllegalStateException("recorded crash"));
    }

    @Test
    public void notifyAppCrashWithNullContextIsNoOp() {
        TermuxCrashUtils.notifyAppCrashFromCrashLogFile(null, "tag");
    }

    @Test
    public void notifyAppCrashWithoutTermuxPackageContextReturnsQuietly() {
        TermuxCrashUtils.notifyAppCrashFromCrashLogFile(context(), "tag");
    }

    @Test
    public void sendCrashReportNotificationWithNullContextIsNoOp() {
        TermuxCrashUtils.sendCrashReportNotification(null, "tag", "title", "text", "message");
    }

    @Test
    public void setupCrashReportsNotificationChannelDoesNotThrow() {
        TermuxCrashUtils.setupCrashReportsNotificationChannel(context());
    }

    @Test
    public void setDefaultAndThreadCrashHandlersDoNotThrow() {
        TermuxCrashUtils.setDefaultCrashHandler(context());
        TermuxCrashUtils.setCrashHandler(context());
    }
}
