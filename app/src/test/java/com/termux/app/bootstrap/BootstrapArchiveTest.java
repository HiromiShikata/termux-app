package com.termux.app.bootstrap;

import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class BootstrapArchiveTest {

    @Test
    public void extractsEveryEntryOfAnArchiveThatHasNoJavaHeapBackingArray() throws IOException {
        Map<String, byte[]> archiveEntries = new LinkedHashMap<>();
        archiveEntries.put("SYMLINKS.txt", "usr/bin/sh←bin/sh\n".getBytes(StandardCharsets.UTF_8));
        archiveEntries.put("bin/login", buildContent(3 * 1024 * 1024));
        archiveEntries.put("lib/apt/methods/http", buildContent(512 * 1024));

        ByteBuffer directArchive = toDirectBuffer(buildZip(archiveEntries));
        Assert.assertFalse(
            "The test must exercise an archive without a Java heap backing array, as the native bootstrap accessor "
                + "hands out.",
            directArchive.hasArray());

        Map<String, byte[]> extractedEntries = extractWithFixedTransferBuffer(new BootstrapArchive(directArchive));

        Assert.assertEquals(archiveEntries.keySet(), extractedEntries.keySet());
        for (Map.Entry<String, byte[]> archiveEntry : archiveEntries.entrySet()) {
            Assert.assertArrayEquals(archiveEntry.getKey(),
                archiveEntry.getValue(), extractedEntries.get(archiveEntry.getKey()));
        }
    }

    @Test
    public void reportsTheArchiveSizeWithoutConsumingTheArchive() throws IOException {
        Map<String, byte[]> archiveEntries = new LinkedHashMap<>();
        archiveEntries.put("bin/login", buildContent(64 * 1024));
        byte[] archiveBytes = buildZip(archiveEntries);

        BootstrapArchive archive = new BootstrapArchive(toDirectBuffer(archiveBytes));

        Assert.assertEquals(archiveBytes.length, archive.sizeInBytes());
        Assert.assertEquals(archiveBytes.length, archive.sizeInBytes());
        Assert.assertEquals(archiveEntries.keySet(), extractWithFixedTransferBuffer(archive).keySet());
        Assert.assertEquals(archiveBytes.length, archive.sizeInBytes());
    }

    @Test
    public void opensAnIndependentStreamOnEveryCallSoTheArchiveCanBeInstalledAgain() throws IOException {
        Map<String, byte[]> archiveEntries = new LinkedHashMap<>();
        archiveEntries.put("bin/login", buildContent(128 * 1024));

        BootstrapArchive archive = new BootstrapArchive(toDirectBuffer(buildZip(archiveEntries)));

        Map<String, byte[]> firstExtraction = extractWithFixedTransferBuffer(archive);
        Map<String, byte[]> secondExtraction = extractWithFixedTransferBuffer(archive);

        Assert.assertArrayEquals(archiveEntries.get("bin/login"), firstExtraction.get("bin/login"));
        Assert.assertArrayEquals(archiveEntries.get("bin/login"), secondExtraction.get("bin/login"));
    }

    @Test
    public void rejectsAMissingArchiveBuffer() {
        try {
            new BootstrapArchive(null);
            Assert.fail("A missing archive buffer must be rejected rather than accepted.");
        } catch (IllegalArgumentException expected) {
            Assert.assertTrue(expected.getMessage(), expected.getMessage().contains("content buffer"));
        }
    }

    private Map<String, byte[]> extractWithFixedTransferBuffer(BootstrapArchive archive) throws IOException {
        Map<String, byte[]> extractedEntries = new LinkedHashMap<>();
        byte[] transferBuffer = new byte[8096];
        try (ZipInputStream archiveInput = new ZipInputStream(archive.openStream())) {
            ZipEntry archiveEntry;
            while ((archiveEntry = archiveInput.getNextEntry()) != null) {
                ByteArrayOutputStream entryContent = new ByteArrayOutputStream();
                int readLength;
                while ((readLength = archiveInput.read(transferBuffer)) != -1) {
                    entryContent.write(transferBuffer, 0, readLength);
                }
                extractedEntries.put(archiveEntry.getName(), entryContent.toByteArray());
            }
        }
        return extractedEntries;
    }

    private byte[] buildZip(Map<String, byte[]> archiveEntries) throws IOException {
        ByteArrayOutputStream archiveBytes = new ByteArrayOutputStream();
        try (ZipOutputStream archiveOutput = new ZipOutputStream(archiveBytes)) {
            for (Map.Entry<String, byte[]> archiveEntry : archiveEntries.entrySet()) {
                archiveOutput.putNextEntry(new ZipEntry(archiveEntry.getKey()));
                archiveOutput.write(archiveEntry.getValue());
                archiveOutput.closeEntry();
            }
        }
        return archiveBytes.toByteArray();
    }

    private byte[] buildContent(int length) {
        byte[] content = new byte[length];
        for (int index = 0; index < length; index++) {
            content[index] = (byte) (index * 37 + 11);
        }
        return content;
    }

    private ByteBuffer toDirectBuffer(byte[] content) {
        ByteBuffer directBuffer = ByteBuffer.allocateDirect(content.length);
        directBuffer.put(content);
        directBuffer.rewind();
        return directBuffer;
    }
}
