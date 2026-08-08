package com.termux.app.browser;

import android.app.Application;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;

import androidx.annotation.NonNull;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.shadows.ShadowLooper;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = BrowserDownloadedPdfOpensInAViewerTest.FinishedDownloadManager.class)
public class BrowserDownloadedPdfOpensInAViewerTest {

    private static final String DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION_SUFFIX =
        ".DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION";

    private static final long DOWNLOAD_ID = 1;

    private static final Uri DOWNLOADED_DOCUMENT = Uri.parse("content://downloads/my_downloads/1");

    private static String sMediaTypeTheServerNamed = BrowserDownloadedDocumentDisplay.PDF_MEDIA_TYPE;

    @Implements(DownloadManager.class)
    public static class FinishedDownloadManager {

        @Implementation
        protected long enqueue(DownloadManager.Request request) {
            return DOWNLOAD_ID;
        }

        @Implementation
        protected Cursor query(DownloadManager.Query query) {
            MatrixCursor cursor = new MatrixCursor(
                new String[]{DownloadManager.COLUMN_ID, DownloadManager.COLUMN_STATUS, DownloadManager.COLUMN_MEDIA_TYPE});
            cursor.addRow(new Object[]{DOWNLOAD_ID, DownloadManager.STATUS_SUCCESSFUL, sMediaTypeTheServerNamed});
            return cursor;
        }

        @Implementation
        protected Uri getUriForDownloadedFile(long id) {
            return DOWNLOADED_DOCUMENT;
        }
    }

    private static final class RecordingHost implements BrowserDownloadController.Host {

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
            return true;
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

    private Intent whatTheFinishedDownloadOpened(String urlThatWasDownloaded, String mediaTypeTheServerNamed) {
        sMediaTypeTheServerNamed = mediaTypeTheServerNamed;
        Context context = RuntimeEnvironment.getApplication();
        ShadowApplication shadowApplication = Shadows.shadowOf((Application) context);
        shadowApplication.grantPermissions(context.getPackageName() + DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION_SUFFIX);
        RecordingHost host = new RecordingHost();
        BrowserDownloadController controller = new BrowserDownloadController(host);
        controller.enqueueDownload(urlThatWasDownloaded, "user-agent", null, mediaTypeTheServerNamed);
        context.sendBroadcast(new Intent(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
            .putExtra(DownloadManager.EXTRA_DOWNLOAD_ID, DOWNLOAD_ID));
        ShadowLooper.idleMainLooper();
        controller.unregisterDownloadCompleteReceiver();
        return shadowApplication.getNextStartedActivity();
    }

    @Test
    public void aFinishedPdfDownloadIsOpenedByAnApplicationThatDisplaysPdfs() {
        Intent opened = whatTheFinishedDownloadOpened("https://example.com/report.pdf",
            BrowserDownloadedDocumentDisplay.PDF_MEDIA_TYPE);

        Assert.assertNotNull("a finished PDF download that opens nothing leaves the user with a file and no document",
            opened);
        Assert.assertEquals(Intent.ACTION_VIEW, opened.getAction());
        Assert.assertNull("the document belongs to a PDF viewer on the device, not to a screen of this application",
            opened.getComponent());
        Assert.assertEquals("a PDF viewer is only offered the document when the intent names the PDF media type",
            BrowserDownloadedDocumentDisplay.PDF_MEDIA_TYPE, opened.getType());
        Assert.assertEquals(DOWNLOADED_DOCUMENT, opened.getData());
        Assert.assertTrue("the viewer cannot read the downloaded document without the read permission grant",
            (opened.getFlags() & Intent.FLAG_GRANT_READ_URI_PERMISSION) != 0);
    }

    @Test
    public void aPdfTheServerNamedNoFormatForIsStillOpenedAsAPdf() {
        Intent opened = whatTheFinishedDownloadOpened("https://example.com/report.pdf", "application/octet-stream");

        Assert.assertNotNull("a server that names no format still sent a PDF the user asked to read", opened);
        Assert.assertNull("the document belongs to a PDF viewer on the device, not to a screen of this application",
            opened.getComponent());
        Assert.assertEquals("a viewer offered a generic binary type cannot display the document",
            BrowserDownloadedDocumentDisplay.PDF_MEDIA_TYPE, opened.getType());
    }
}
