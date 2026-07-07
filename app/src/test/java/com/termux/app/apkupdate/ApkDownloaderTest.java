package com.termux.app.apkupdate;

import android.content.Context;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

@RunWith(RobolectricTestRunner.class)
public class ApkDownloaderTest {

    private static final String CACHE_SUBDIRECTORY_NAME = "apkupdate";
    private static final String TARGET_FILE_NAME = "termux-app_universal.apk";
    private static final String PARTIAL_FILE_NAME = "termux-app_universal.apk.part";

    @Test
    public void downloadsToFinalNameWithoutLeavingPartialFileOnSuccess() throws Exception {
        Context context = RuntimeEnvironment.getApplication();
        File targetDirectory = cacheDirectory(context);
        byte[] body = apkBody(2 * 1024 * 1024);

        try (ServerSocket serverSocket =
                 new ServerSocket(0, 0, java.net.InetAddress.getByName("127.0.0.1"))) {
            startDaemon(() -> serveBody(serverSocket, body, body.length));

            String url = downloadUrl(serverSocket);
            ApkDownloader downloader = new ApkDownloader(context);

            File downloaded = downloader.download(url, TARGET_FILE_NAME);

            Assert.assertEquals(new File(targetDirectory, TARGET_FILE_NAME).getAbsolutePath(),
                downloaded.getAbsolutePath());
            Assert.assertTrue("Final file must exist after a successful download", downloaded.exists());
            Assert.assertEquals(body.length, downloaded.length());
            Assert.assertFalse("No .part file may remain after a successful download",
                new File(targetDirectory, PARTIAL_FILE_NAME).exists());
        }
    }

    @Test
    public void leavesNeitherPartialNorFinalFileWhenConnectionIsInterrupted() throws Exception {
        Context context = RuntimeEnvironment.getApplication();
        File targetDirectory = cacheDirectory(context);
        deleteQuietly(new File(targetDirectory, TARGET_FILE_NAME));
        deleteQuietly(new File(targetDirectory, PARTIAL_FILE_NAME));

        try (ServerSocket serverSocket =
                 new ServerSocket(0, 0, java.net.InetAddress.getByName("127.0.0.1"))) {
            startDaemon(() -> closeWithoutResponding(serverSocket));

            String url = downloadUrl(serverSocket);
            ApkDownloader downloader = new ApkDownloader(context);

            Assert.assertThrows(IOException.class, () -> downloader.download(url, TARGET_FILE_NAME));

            Assert.assertFalse("No .part file may remain after a failed download",
                new File(targetDirectory, PARTIAL_FILE_NAME).exists());
            Assert.assertFalse("No final file may appear after a failed download",
                new File(targetDirectory, TARGET_FILE_NAME).exists());
        }
    }

    @Test
    public void leavesNeitherPartialNorFinalFileWhenServerReturnsNonOkStatus() throws Exception {
        Context context = RuntimeEnvironment.getApplication();
        File targetDirectory = cacheDirectory(context);
        deleteQuietly(new File(targetDirectory, TARGET_FILE_NAME));
        deleteQuietly(new File(targetDirectory, PARTIAL_FILE_NAME));

        try (ServerSocket serverSocket =
                 new ServerSocket(0, 0, java.net.InetAddress.getByName("127.0.0.1"))) {
            startDaemon(() -> serveNotFound(serverSocket));

            String url = downloadUrl(serverSocket);
            ApkDownloader downloader = new ApkDownloader(context);

            Assert.assertThrows(IOException.class, () -> downloader.download(url, TARGET_FILE_NAME));

            Assert.assertFalse("No .part file may remain after a non-200 response",
                new File(targetDirectory, PARTIAL_FILE_NAME).exists());
            Assert.assertFalse("No final file may appear after a non-200 response",
                new File(targetDirectory, TARGET_FILE_NAME).exists());
        }
    }

    @Test
    public void throwsAndDeletesPartialWhenContentLengthDoesNotMatchBytesWritten() throws Exception {
        Context context = RuntimeEnvironment.getApplication();
        File targetDirectory = cacheDirectory(context);
        byte[] body = apkBody(2 * 1024 * 1024);

        try (ServerSocket serverSocket =
                 new ServerSocket(0, 0, java.net.InetAddress.getByName("127.0.0.1"))) {
            startDaemon(() -> serveBody(serverSocket, body, body.length + 1024));

            String url = downloadUrl(serverSocket);
            ApkDownloader downloader = new ApkDownloader(context);

            Assert.assertThrows(IOException.class, () -> downloader.download(url, TARGET_FILE_NAME));

            Assert.assertFalse("No .part file may remain after a truncated transfer",
                new File(targetDirectory, PARTIAL_FILE_NAME).exists());
            Assert.assertFalse("No final file may appear after a truncated transfer",
                new File(targetDirectory, TARGET_FILE_NAME).exists());
        }
    }

    private void deleteQuietly(File file) {
        if (file.exists()) {
            Assert.assertTrue(file.delete());
        }
    }

    private File cacheDirectory(Context context) {
        File targetDirectory = new File(context.getCacheDir(), CACHE_SUBDIRECTORY_NAME);
        Assert.assertTrue(targetDirectory.isDirectory() || targetDirectory.mkdirs());
        return targetDirectory;
    }

    private String downloadUrl(ServerSocket serverSocket) {
        return "http://127.0.0.1:" + serverSocket.getLocalPort() + "/" + TARGET_FILE_NAME;
    }

    private byte[] apkBody(int totalSize) {
        byte[] body = new byte[totalSize];
        body[0] = 0x50;
        body[1] = 0x4B;
        body[2] = 0x03;
        body[3] = 0x04;
        return body;
    }

    private void startDaemon(Runnable runnable) {
        Thread thread = new Thread(runnable);
        thread.setDaemon(true);
        thread.start();
    }

    private void serveBody(ServerSocket serverSocket, byte[] body, long advertisedContentLength) {
        try (Socket socket = serverSocket.accept()) {
            drainRequest(socket);
            OutputStream out = socket.getOutputStream();
            String header = "HTTP/1.1 200 OK\r\nContent-Length: " + advertisedContentLength
                + "\r\nConnection: close\r\n\r\n";
            out.write(header.getBytes(StandardCharsets.UTF_8));
            out.write(body);
            out.flush();
        } catch (IOException ignored) {
        }
    }

    private void closeWithoutResponding(ServerSocket serverSocket) {
        try (Socket socket = serverSocket.accept()) {
            drainRequest(socket);
            socket.close();
        } catch (IOException ignored) {
        }
    }

    private void serveNotFound(ServerSocket serverSocket) {
        try (Socket socket = serverSocket.accept()) {
            drainRequest(socket);
            OutputStream out = socket.getOutputStream();
            String response = "HTTP/1.1 404 Not Found\r\nContent-Length: 0\r\nConnection: close\r\n\r\n";
            out.write(response.getBytes(StandardCharsets.UTF_8));
            out.flush();
        } catch (IOException ignored) {
        }
    }

    private void drainRequest(Socket socket) throws IOException {
        java.io.BufferedReader reader = new java.io.BufferedReader(
            new java.io.InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        String line;
        while ((line = reader.readLine()) != null && !line.isEmpty()) {
        }
    }
}
