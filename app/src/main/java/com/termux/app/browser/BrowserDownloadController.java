package com.termux.app.browser;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.text.TextUtils;
import android.webkit.CookieManager;
import android.webkit.URLUtil;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.termux.shared.logger.Logger;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class BrowserDownloadController {

    private static final String LOG_TAG = "BrowserDownloadController";

    public interface Host {

        @NonNull
        Context getDownloadContext();

        boolean isActivityVisible();

        void onDownloadStarted(@NonNull String fileName);

        void onDownloadComplete();

        void onDownloadFailed();
    }

    private final Host mHost;

    private final Set<Long> mEnqueuedDownloadIds = new HashSet<>();

    private final Map<Long, String> mDownloadFileNames = new HashMap<>();

    private final BrowserPendingDocumentDisplay mPendingDocumentDisplay =
        new BrowserPendingDocumentDisplay();

    private BroadcastReceiver mDownloadCompleteReceiver;

    private boolean mDownloadReceiverRegistered;

    public BrowserDownloadController(@NonNull Host host) {
        this.mHost = host;
    }

    public void enqueueDownload(@Nullable String url, @Nullable String userAgent,
                                @Nullable String contentDisposition, @Nullable String mimetype) {
        DownloadManager downloadManager = downloadManager();
        if (downloadManager == null || url == null) {
            mHost.onDownloadFailed();
            return;
        }
        try {
            String fileName = URLUtil.guessFileName(url, contentDisposition, mimetype);
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
            if (!TextUtils.isEmpty(mimetype)) {
                request.setMimeType(mimetype);
            }
            if (!TextUtils.isEmpty(userAgent)) {
                request.addRequestHeader("User-Agent", userAgent);
            }
            String cookie = CookieManager.getInstance().getCookie(url);
            if (!TextUtils.isEmpty(cookie)) {
                request.addRequestHeader("Cookie", cookie);
            }
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);
            boolean receiverReady = registerDownloadCompleteReceiver();
            long downloadId = downloadManager.enqueue(request);
            if (receiverReady) {
                mEnqueuedDownloadIds.add(downloadId);
                mDownloadFileNames.put(downloadId, fileName);
            }
            mHost.onDownloadStarted(fileName);
        } catch (Exception e) {
            Logger.logStackTraceWithMessage(LOG_TAG, "Failed to enqueue browser download", e);
            mHost.onDownloadFailed();
        }
    }

    private boolean registerDownloadCompleteReceiver() {
        if (mDownloadReceiverRegistered) return true;
        mDownloadCompleteReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                long completedId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                if (!mEnqueuedDownloadIds.remove(completedId)) return;
                if (isDownloadSuccessful(completedId)) {
                    displayDownloadedDocumentOrOpenDownloadsView(completedId);
                } else {
                    mDownloadFileNames.remove(completedId);
                    mHost.onDownloadFailed();
                }
            }
        };
        IntentFilter filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        try {
            ContextCompat.registerReceiver(mHost.getDownloadContext(), mDownloadCompleteReceiver, filter,
                ContextCompat.RECEIVER_NOT_EXPORTED);
            mDownloadReceiverRegistered = true;
            return true;
        } catch (Exception e) {
            Logger.logStackTraceWithMessage(LOG_TAG, "Failed to register download receiver", e);
            mDownloadCompleteReceiver = null;
            return false;
        }
    }

    public void unregisterDownloadCompleteReceiver() {
        if (!mDownloadReceiverRegistered) return;
        try {
            mHost.getDownloadContext().unregisterReceiver(mDownloadCompleteReceiver);
        } catch (Exception e) {
            Logger.logStackTraceWithMessage(LOG_TAG, "Failed to unregister download receiver", e);
        }
        mDownloadReceiverRegistered = false;
    }

    private boolean isDownloadSuccessful(long downloadId) {
        DownloadManager downloadManager = downloadManager();
        if (downloadManager == null) return false;
        try (Cursor cursor = downloadManager.query(new DownloadManager.Query().setFilterById(downloadId))) {
            if (cursor == null || !cursor.moveToFirst()) return false;
            int statusColumn = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
            if (statusColumn < 0) return false;
            return cursor.getInt(statusColumn) == DownloadManager.STATUS_SUCCESSFUL;
        } catch (Exception e) {
            Logger.logStackTraceWithMessage(LOG_TAG, "Failed to query download status", e);
            return false;
        }
    }

    private void displayDownloadedDocumentOrOpenDownloadsView(long downloadId) {
        String fileName = mDownloadFileNames.get(downloadId);
        if (!BrowserDownloadedDocumentDisplay.displaysAsDocument(downloadedMediaType(downloadId), fileName)) {
            mDownloadFileNames.remove(downloadId);
            openDownloadsView();
            return;
        }
        if (!mHost.isActivityVisible()) {
            mPendingDocumentDisplay.rememberUntilTheActivityReturns(downloadId);
            return;
        }
        mDownloadFileNames.remove(downloadId);
        DownloadManager downloadManager = downloadManager();
        if (downloadManager == null) {
            mHost.onDownloadFailed();
            return;
        }
        try {
            Uri downloadedDocument = downloadManager.getUriForDownloadedFile(downloadId);
            Intent displayIntent = new Intent(Intent.ACTION_VIEW);
            displayIntent.setDataAndType(downloadedDocument, BrowserDownloadedDocumentDisplay.PDF_MEDIA_TYPE);
            displayIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            displayIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mHost.getDownloadContext().startActivity(displayIntent);
        } catch (Exception e) {
            Logger.logStackTraceWithMessage(LOG_TAG, "Failed to open the downloaded document in a viewer", e);
            mHost.onDownloadFailed();
        }
    }

    public void displayPendingDocumentIfAny() {
        mPendingDocumentDisplay.displayRememberedDocument(this::displayDownloadedDocumentOrOpenDownloadsView);
    }

    @Nullable
    private String downloadedMediaType(long downloadId) {
        DownloadManager downloadManager = downloadManager();
        if (downloadManager == null) return null;
        try (Cursor cursor = downloadManager.query(new DownloadManager.Query().setFilterById(downloadId))) {
            if (cursor == null || !cursor.moveToFirst()) return null;
            int mediaTypeColumn = cursor.getColumnIndex(DownloadManager.COLUMN_MEDIA_TYPE);
            if (mediaTypeColumn < 0) return null;
            return cursor.getString(mediaTypeColumn);
        } catch (Exception e) {
            Logger.logStackTraceWithMessage(LOG_TAG, "Failed to query the downloaded media type", e);
            return null;
        }
    }

    private void openDownloadsView() {
        if (!mHost.isActivityVisible()) return;
        try {
            mHost.getDownloadContext().startActivity(new Intent(DownloadManager.ACTION_VIEW_DOWNLOADS));
            mHost.onDownloadComplete();
        } catch (Exception e) {
            Logger.logStackTraceWithMessage(LOG_TAG, "Failed to open downloads view", e);
        }
    }

    @Nullable
    private DownloadManager downloadManager() {
        return (DownloadManager) mHost.getDownloadContext().getSystemService(Context.DOWNLOAD_SERVICE);
    }
}
