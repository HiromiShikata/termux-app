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

    @Test
    public void deletesPartialFileWhenConnectionIsInterrupted() throws Exception {
        Context context = RuntimeEnvironment.getApplication();
        File targetDirectory = new File(context.getCacheDir(), CACHE_SUBDIRECTORY_NAME);
        Assert.assertTrue(targetDirectory.isDirectory() || targetDirectory.mkdirs());
        File staleFile = new File(targetDirectory, "termux-app_universal.apk");
        java.nio.file.Files.write(staleFile.toPath(), new byte[]{0x50, 0x4B, 0x03, 0x04, 0x05});
        Assert.assertTrue(staleFile.exists());

        try (ServerSocket serverSocket =
                 new ServerSocket(0, 0, java.net.InetAddress.getByName("127.0.0.1"))) {
            Thread serverThread = new Thread(() -> closeWithoutResponding(serverSocket));
            serverThread.setDaemon(true);
            serverThread.start();

            String url = "http://127.0.0.1:" + serverSocket.getLocalPort() + "/termux-app_universal.apk";
            ApkDownloader downloader = new ApkDownloader(context);

            Assert.assertThrows(IOException.class,
                () -> downloader.download(url, "termux-app_universal.apk"));

            Assert.assertFalse("Partial file must be deleted after a failed download", staleFile.exists());
        }
    }

    @Test
    public void deletesPartialFileWhenServerReturnsNonOkStatus() throws Exception {
        Context context = RuntimeEnvironment.getApplication();
        File targetDirectory = new File(context.getCacheDir(), CACHE_SUBDIRECTORY_NAME);
        Assert.assertTrue(targetDirectory.isDirectory() || targetDirectory.mkdirs());
        File staleFile = new File(targetDirectory, "termux-app_universal.apk");
        java.nio.file.Files.write(staleFile.toPath(), new byte[]{0x01, 0x02, 0x03});
        Assert.assertTrue(staleFile.exists());

        try (ServerSocket serverSocket =
                 new ServerSocket(0, 0, java.net.InetAddress.getByName("127.0.0.1"))) {
            Thread serverThread = new Thread(() -> serveNotFound(serverSocket));
            serverThread.setDaemon(true);
            serverThread.start();

            String url = "http://127.0.0.1:" + serverSocket.getLocalPort() + "/termux-app_universal.apk";
            ApkDownloader downloader = new ApkDownloader(context);

            Assert.assertThrows(IOException.class,
                () -> downloader.download(url, "termux-app_universal.apk"));

            Assert.assertFalse("Stale partial file must be deleted after a non-200 response", staleFile.exists());
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
