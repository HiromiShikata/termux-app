package com.termux.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.termux.app.bootstrap.BootstrapInstallationRunner;
import com.termux.app.bootstrap.CanonicalPathBootstrapDirectoryCreator;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@RunWith(RobolectricTestRunner.class)
public class BootstrapInstallationConcurrentRequestSerializationTest {

    private static final String FIRST_REQUEST_NAME = "first bootstrap installation request";

    private static final String SECOND_REQUEST_NAME = "second bootstrap installation request";

    private static final byte FIRST_REQUEST_CONTENT_MARKER = 0x11;

    private static final byte SECOND_REQUEST_CONTENT_MARKER = 0x22;

    private static final int ARCHIVE_ENTRY_BYTES = 4096;

    private static final String EXTRACTED_EXECUTABLE_ENTRY_NAME = "bin/login";

    private static final List<String> ARCHIVE_ENTRY_NAMES =
        Arrays.asList(EXTRACTED_EXECUTABLE_ENTRY_NAME, "bin/tail", "lib/apt/methods/http");

    private static final String SYMLINK_LIST_ENTRY_NAME = "SYMLINKS.txt";

    private static final String SYMLINK_LIST_CONTENT = EXTRACTED_EXECUTABLE_ENTRY_NAME + "←bin/login-alias\n";

    private static final long SERIALIZED_HANDOFF_BOUND_SECONDS = 10L;

    private static final long REQUEST_TERMINATION_BOUND_MILLIS = 60L * 1000L;

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    private final CountDownLatch firstRequestExtractedItsEntries = new CountDownLatch(1);

    private final CountDownLatch secondRequestClearedTheStagingDirectoryOrSettled = new CountDownLatch(1);

    private final CountDownLatch firstRequestFinished = new CountDownLatch(1);

    private final List<String> extractionsIntoTheStagingDirectory = new CopyOnWriteArrayList<>();

    private final List<Throwable> firstRequestReportedFailures = new CopyOnWriteArrayList<>();

    private final List<Throwable> secondRequestReportedFailures = new CopyOnWriteArrayList<>();

    private File stagingDirectory;

    @Test
    public void concurrentBootstrapInstallationRequestsExtractIntoTheStagingDirectoryOnlyOnce() throws Exception {
        requestTwoConcurrentBootstrapInstallations();

        assertEquals("the prefix staging directory \"" + stagingDirectory.getAbsolutePath()
                + "\" was extracted into by " + extractionsIntoTheStagingDirectory
                + ", so more than one bootstrap installation ran in this process",
            1, extractionsIntoTheStagingDirectory.size());
    }

    @Test
    public void secondBootstrapInstallationRequestSettlesWithoutOverwritingTheFirstRequestsExtraction()
        throws Exception {
        requestTwoConcurrentBootstrapInstallations();

        for (String archiveEntryName : ARCHIVE_ENTRY_NAMES) {
            File extractedEntry = new File(stagingDirectory, archiveEntryName);
            assertTrue("the " + SECOND_REQUEST_NAME + " removed \"" + archiveEntryName
                    + "\" from the prefix staging directory after the " + FIRST_REQUEST_NAME + " extracted it",
                extractedEntry.isFile());
            assertEquals("the " + SECOND_REQUEST_NAME + " left \"" + archiveEntryName
                    + "\" in the prefix staging directory with the wrong length",
                ARCHIVE_ENTRY_BYTES, extractedEntry.length());
            assertEquals("\"" + archiveEntryName + "\" in the prefix staging directory carries the content of the "
                    + SECOND_REQUEST_NAME + " instead of the content of the " + FIRST_REQUEST_NAME
                    + ", so the second request overwrote the first request's extraction",
                FIRST_REQUEST_CONTENT_MARKER, distinctContentMarkerOf(extractedEntry));
        }

        assertEquals("the " + SECOND_REQUEST_NAME + " extracted into the prefix staging directory that the "
                + FIRST_REQUEST_NAME + " was installing into, instead of waiting for it or being refused",
            0, extractionsBy(SECOND_REQUEST_NAME));

        Throwable secondRequestFailure = reportedFailureOf(secondRequestReportedFailures);
        assertFalse("the " + SECOND_REQUEST_NAME + " was neither completed nor refused with an explained error"
                + " but failed on the prefix staging directory with " + describe(secondRequestFailure),
            isMissingFileFailure(secondRequestFailure));
    }

    @Test
    public void fileOperationFollowingExtractionSucceedsWhileASecondInstallationIsRequested() throws Exception {
        requestTwoConcurrentBootstrapInstallations();

        Throwable firstRequestFailure = reportedFailureOf(firstRequestReportedFailures);
        assertNull("the file operation that follows the extraction of the " + FIRST_REQUEST_NAME
                + " failed because the " + SECOND_REQUEST_NAME + " removed its target: "
                + describe(firstRequestFailure),
            firstRequestFailure);
    }

    private void requestTwoConcurrentBootstrapInstallations() throws Exception {
        stagingDirectory = temporaryFolder.newFolder("usr-staging");
        File firstRequestArchive = writeBootstrapArchive(FIRST_REQUEST_CONTENT_MARKER);
        File secondRequestArchive = writeBootstrapArchive(SECOND_REQUEST_CONTENT_MARKER);

        Thread firstRequestThread = new Thread(() -> new BootstrapInstallationRunner(firstRequestReportedFailures::add)
            .run(() -> installBootstrapArchiveIntoTheStagingDirectory(FIRST_REQUEST_NAME, firstRequestArchive)),
            FIRST_REQUEST_NAME);
        firstRequestThread.start();

        awaitHandoff(firstRequestExtractedItsEntries);

        Thread secondRequestThread = new Thread(() -> {
            try {
                new BootstrapInstallationRunner(secondRequestReportedFailures::add)
                    .run(() -> installBootstrapArchiveIntoTheStagingDirectory(SECOND_REQUEST_NAME,
                        secondRequestArchive));
            } finally {
                secondRequestClearedTheStagingDirectoryOrSettled.countDown();
            }
        }, SECOND_REQUEST_NAME);
        secondRequestThread.start();

        firstRequestThread.join(REQUEST_TERMINATION_BOUND_MILLIS);
        secondRequestThread.join(REQUEST_TERMINATION_BOUND_MILLIS);

        assertFalse("the " + FIRST_REQUEST_NAME + " never terminated", firstRequestThread.isAlive());
        assertFalse("the " + SECOND_REQUEST_NAME + " never terminated", secondRequestThread.isAlive());
    }

    private void installBootstrapArchiveIntoTheStagingDirectory(String requestName, File archive) throws Exception {
        boolean isTheFirstRequest = FIRST_REQUEST_NAME.equals(requestName);
        try {
            clearStagingDirectory();
            if (!isTheFirstRequest) {
                secondRequestClearedTheStagingDirectoryOrSettled.countDown();
                awaitHandoff(firstRequestFinished);
            }

            createStagingDirectory();
            extractionsIntoTheStagingDirectory.add(requestName);
            try (InputStream archiveStream = new FileInputStream(archive)) {
                TermuxInstaller.extractBootstrapArchive(archiveStream, stagingDirectory,
                    new CanonicalPathBootstrapDirectoryCreator());
            }

            if (isTheFirstRequest) {
                firstRequestExtractedItsEntries.countDown();
                awaitHandoff(secondRequestClearedTheStagingDirectoryOrSettled);
            }

            grantExecutePermissionToExtractedExecutable();
        } finally {
            if (isTheFirstRequest) {
                firstRequestFinished.countDown();
            }
        }
    }

    private void clearStagingDirectory() throws IOException {
        deleteRecursively(stagingDirectory);
    }

    private void createStagingDirectory() throws IOException {
        if (!stagingDirectory.isDirectory() && !stagingDirectory.mkdirs() && !stagingDirectory.isDirectory()) {
            throw new IOException("Creating the prefix staging directory \"" + stagingDirectory.getAbsolutePath()
                + "\" failed");
        }
    }

    private void grantExecutePermissionToExtractedExecutable() throws IOException {
        Files.setPosixFilePermissions(new File(stagingDirectory, EXTRACTED_EXECUTABLE_ENTRY_NAME).toPath(),
            PosixFilePermissions.fromString("rwx------"));
    }

    private void awaitHandoff(CountDownLatch handoff) {
        try {
            handoff.await(SERIALIZED_HANDOFF_BOUND_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException interruption) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Waiting for a bootstrap installation handoff was interrupted",
                interruption);
        }
    }

    private int extractionsBy(String requestName) {
        int extractions = 0;
        for (String extractingRequestName : extractionsIntoTheStagingDirectory) {
            if (extractingRequestName.equals(requestName)) {
                extractions++;
            }
        }
        return extractions;
    }

    private File writeBootstrapArchive(byte contentMarker) throws IOException {
        File archive = temporaryFolder.newFile("bootstrap-" + contentMarker + ".zip");
        byte[] entryContent = new byte[ARCHIVE_ENTRY_BYTES];
        Arrays.fill(entryContent, contentMarker);

        try (ZipOutputStream archiveOutput = new ZipOutputStream(new FileOutputStream(archive))) {
            archiveOutput.putNextEntry(new ZipEntry(SYMLINK_LIST_ENTRY_NAME));
            archiveOutput.write(SYMLINK_LIST_CONTENT.getBytes(StandardCharsets.UTF_8));
            archiveOutput.closeEntry();

            for (String archiveEntryName : ARCHIVE_ENTRY_NAMES) {
                archiveOutput.putNextEntry(new ZipEntry(archiveEntryName));
                archiveOutput.write(entryContent);
                archiveOutput.closeEntry();
            }
        }
        return archive;
    }

    private static byte distinctContentMarkerOf(File extractedEntry) throws IOException {
        byte[] content = Files.readAllBytes(extractedEntry.toPath());
        if (content.length == 0) {
            throw new IOException("\"" + extractedEntry.getAbsolutePath() + "\" holds no content to identify");
        }
        for (byte contentByte : content) {
            if (contentByte != content[0]) {
                throw new IOException("\"" + extractedEntry.getAbsolutePath()
                    + "\" mixes the content of more than one bootstrap installation request");
            }
        }
        return content[0];
    }

    private static void deleteRecursively(File file) throws IOException {
        File[] children = file.listFiles();
        if (children != null) {
            for (File child : children) {
                deleteRecursively(child);
            }
        }
        if (file.exists() && !file.delete()) {
            throw new IOException("Deleting \"" + file.getAbsolutePath() + "\" failed");
        }
    }

    private static Throwable reportedFailureOf(List<Throwable> reportedFailures) {
        return reportedFailures.isEmpty() ? null : reportedFailures.get(0);
    }

    private static boolean isMissingFileFailure(Throwable failure) {
        return failure instanceof NoSuchFileException || failure instanceof FileNotFoundException;
    }

    private static String describe(Throwable failure) {
        return failure == null ? "no reported failure"
            : failure.getClass().getName() + ": " + failure.getMessage();
    }
}
