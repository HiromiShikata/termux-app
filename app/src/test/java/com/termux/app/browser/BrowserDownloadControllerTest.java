package com.termux.app.browser;

import android.content.Context;

import androidx.annotation.NonNull;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class BrowserDownloadControllerTest {

    private static final class RecordingHost implements BrowserDownloadController.Host {

        boolean activityVisible = true;
        final List<String> startedFiles = new ArrayList<>();
        int completeCount;
        int failedCount;

        @NonNull
        @Override
        public Context getDownloadContext() {
            return RuntimeEnvironment.getApplication();
        }

        @Override
        public boolean isActivityVisible() {
            return activityVisible;
        }

        @Override
        public void onDownloadStarted(@NonNull String fileName) {
            startedFiles.add(fileName);
        }

        @Override
        public void onDownloadComplete() {
            completeCount++;
        }

        @Override
        public void onDownloadFailed() {
            failedCount++;
        }
    }

    @Test
    public void enqueueWithNullUrlReportsFailure() {
        RecordingHost host = new RecordingHost();
        BrowserDownloadController controller = new BrowserDownloadController(host);
        controller.enqueueDownload(null, "ua", null, "text/plain");
        Assert.assertEquals(1, host.failedCount);
        Assert.assertTrue(host.startedFiles.isEmpty());
    }

    @Test
    public void enqueueWithValidUrlReportsStartedWithGuessedFileName() {
        RecordingHost host = new RecordingHost();
        BrowserDownloadController controller = new BrowserDownloadController(host);
        controller.enqueueDownload("https://example.com/file.pdf", "ua", null, "application/pdf");
        Assert.assertEquals(1, host.startedFiles.size());
        Assert.assertTrue(host.startedFiles.get(0).endsWith(".pdf"));
        Assert.assertEquals(0, host.failedCount);
    }

    @Test
    public void unregisterWithoutRegistrationIsNoOp() {
        RecordingHost host = new RecordingHost();
        BrowserDownloadController controller = new BrowserDownloadController(host);
        controller.unregisterDownloadCompleteReceiver();
        Assert.assertEquals(0, host.failedCount);
        Assert.assertEquals(0, host.completeCount);
    }
}
