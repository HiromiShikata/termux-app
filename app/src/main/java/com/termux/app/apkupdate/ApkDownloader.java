package com.termux.app.apkupdate;

import android.content.Context;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

public final class ApkDownloader {

    private static final int CONNECT_TIMEOUT_MILLIS = 30000;
    private static final int READ_TIMEOUT_MILLIS = 60000;
    private static final String CACHE_SUBDIRECTORY_NAME = "apkupdate";
    private static final String PARTIAL_FILE_SUFFIX = ".part";

    private final Context context;

    private final ApkUpdateCachePruner cachePruner = new ApkUpdateCachePruner();

    public ApkDownloader(Context context) {
        this.context = context.getApplicationContext();
    }

    public File download(String downloadUrl, String fileName) throws IOException {
        File targetDirectory = new File(context.getCacheDir(), CACHE_SUBDIRECTORY_NAME);
        if (!targetDirectory.isDirectory() && !targetDirectory.mkdirs()) {
            throw new IOException("Failed to create APK cache directory: " + targetDirectory.getAbsolutePath());
        }
        cachePruner.pruneExcept(targetDirectory, fileName);
        File targetFile = new File(targetDirectory, fileName);
        File partialFile = new File(targetDirectory, fileName + PARTIAL_FILE_SUFFIX);
        deleteQuietly(partialFile);

        URLConnection urlConnection = new URL(downloadUrl).openConnection();
        if (!(urlConnection instanceof HttpURLConnection)) {
            throw new IOException("Unsupported protocol for APK download URL: " + downloadUrl);
        }
        HttpURLConnection connection = (HttpURLConnection) urlConnection;
        try {
            connection.setConnectTimeout(CONNECT_TIMEOUT_MILLIS);
            connection.setReadTimeout(READ_TIMEOUT_MILLIS);
            connection.setInstanceFollowRedirects(true);
            connection.setRequestMethod("GET");

            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new IOException("Unexpected HTTP response code " + responseCode + " for " + downloadUrl);
            }

            long contentLength = connection.getContentLengthLong();
            long bytesWritten = 0L;
            try (InputStream inputStream = connection.getInputStream();
                 OutputStream outputStream = new FileOutputStream(partialFile)) {
                byte[] chunk = new byte[8192];
                int read;
                while ((read = inputStream.read(chunk)) != -1) {
                    outputStream.write(chunk, 0, read);
                    bytesWritten += read;
                }
                outputStream.flush();
            }

            if (contentLength > 0 && bytesWritten != contentLength) {
                throw new IOException("Truncated download for " + downloadUrl + ": wrote " + bytesWritten
                    + " bytes but Content-Length was " + contentLength);
            }

            deleteQuietly(targetFile);
            if (!partialFile.renameTo(targetFile)) {
                throw new IOException("Failed to move downloaded APK into place: "
                    + partialFile.getAbsolutePath() + " -> " + targetFile.getAbsolutePath());
            }
        } catch (IOException | RuntimeException exception) {
            deleteQuietly(partialFile);
            throw exception;
        } finally {
            connection.disconnect();
        }
        return targetFile;
    }

    private static void deleteQuietly(File file) {
        if (file.exists()) {
            file.delete();
        }
    }
}
