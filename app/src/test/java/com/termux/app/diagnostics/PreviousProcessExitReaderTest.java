package com.termux.app.diagnostics;

import android.app.ActivityManager;
import android.app.ApplicationExitInfo;
import android.content.Context;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowActivityManager;

import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 30)
public class PreviousProcessExitReaderTest {

    private static final int ANDROID_VERSION_KEEPING_THE_RECORD = 30;

    private static final int ANDROID_VERSION_KEEPING_NO_RECORD = 29;

    private static final long ENDED_AT_MILLIS = 1783216740000L;

    private Context context() {
        return RuntimeEnvironment.getApplication();
    }

    private void recordEnding(long endedAtMillis, int reason, int importance, String description) {
        ActivityManager activityManager =
            (ActivityManager) context().getSystemService(Context.ACTIVITY_SERVICE);
        ApplicationExitInfo recordedEnding = ShadowActivityManager.ApplicationExitInfoBuilder.newBuilder()
            .setTimestamp(endedAtMillis)
            .setReason(reason)
            .setImportance(importance)
            .setDescription(description)
            .build();
        Shadows.shadowOf(activityManager).addApplicationExitInfo(recordedEnding);
    }

    @Test
    public void eachFieldOfARecordedEndingReachesThePlaceInTheReadingThatNamesIt() {
        recordEnding(ENDED_AT_MILLIS, ApplicationExitInfo.REASON_ANR,
            ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND, "Input dispatching timed out");

        DiagnosticsPreviousProcessExits reading =
            new PreviousProcessExitReader().read(context(), ANDROID_VERSION_KEEPING_THE_RECORD);

        Assert.assertEquals("a reading the system answered must be presented as read, because an unread"
                + " state and an answered one lead the investigation to opposite conclusions",
            DiagnosticsPreviousProcessExits.Reading.READ, reading.getReading());
        List<DiagnosticsPreviousProcessExit> endings = reading.getExits();
        Assert.assertEquals("the ending the system recorded has to arrive in the reading at all",
            1, endings.size());
        DiagnosticsPreviousProcessExit ending = endings.get(0);
        Assert.assertEquals("the time of the ending is what lines it up against the moment the sessions"
                + " were rebuilt, so it cannot be taken from another field",
            ENDED_AT_MILLIS, ending.getEndedAtMillis());
        Assert.assertEquals("the reason and the importance are both small integers, so a reading that"
                + " swapped them would still render and would name a cause that never happened",
            "an unresponsive main thread", ending.getReasonLabel());
        Assert.assertEquals("the importance says whether the app was in front of the user when it ended,"
                + " which is the difference between an ending the owner saw and one the system did while"
                + " the app was cached",
            "in the foreground", ending.getImportanceLabel());
        Assert.assertEquals("the system's own words carry detail no reason code does",
            "Input dispatching timed out", ending.getDescription());
    }

    @Test
    public void theMostRecentEndingIsTheFirstOneTheReadingCarries() {
        recordEnding(ENDED_AT_MILLIS - 60000L, ApplicationExitInfo.REASON_LOW_MEMORY,
            ActivityManager.RunningAppProcessInfo.IMPORTANCE_CACHED, "isolated not needed");
        recordEnding(ENDED_AT_MILLIS, ApplicationExitInfo.REASON_CRASH,
            ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND, "java.lang.IllegalStateException");

        DiagnosticsPreviousProcessExits reading =
            new PreviousProcessExitReader().read(context(), ANDROID_VERSION_KEEPING_THE_RECORD);

        List<DiagnosticsPreviousProcessExit> endings = reading.getExits();
        Assert.assertEquals("both endings the system recorded have to arrive, because a single ending"
            + " cannot show whether the app is ending repeatedly", 2, endings.size());
        Assert.assertEquals("the reading is read top down and the ending that matters most is the one"
                + " that just happened, so the newest has to come first",
            ENDED_AT_MILLIS, endings.get(0).getEndedAtMillis());
    }

    @Test
    public void anAndroidVersionThatKeepsNoSuchRecordIsReportedAsSuchRatherThanAsAnEmptyRecord() {
        DiagnosticsPreviousProcessExits reading =
            new PreviousProcessExitReader().read(context(), ANDROID_VERSION_KEEPING_NO_RECORD);

        Assert.assertEquals("a version that keeps no record and a system that holds an empty one look"
                + " identical in the reading unless they are separated here, and only one of them means"
                + " no process of this app has ended",
            DiagnosticsPreviousProcessExits.Reading.NOT_KEPT_BY_THIS_ANDROID, reading.getReading());
        Assert.assertTrue("a version that keeps no record cannot carry endings",
            reading.getExits().isEmpty());
    }

    @Test
    public void aSystemHoldingNoEndingYetIsReadRatherThanReportedAsUnavailable() {
        DiagnosticsPreviousProcessExits reading =
            new PreviousProcessExitReader().read(context(), ANDROID_VERSION_KEEPING_THE_RECORD);

        Assert.assertEquals("a system that answered with nothing has been read, and saying otherwise"
                + " would hide that this app has never had a process end",
            DiagnosticsPreviousProcessExits.Reading.READ, reading.getReading());
        Assert.assertTrue("an empty answer carries no endings", reading.getExits().isEmpty());
    }
}
