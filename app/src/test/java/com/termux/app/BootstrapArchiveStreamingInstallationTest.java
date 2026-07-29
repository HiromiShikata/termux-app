package com.termux.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.termux.shared.errors.Error;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@RunWith(RobolectricTestRunner.class)
public class BootstrapArchiveStreamingInstallationTest {

    private static final int ARCHIVE_ENTRY_COUNT = 12;

    private static final int ARCHIVE_ENTRY_BYTES = 512 * 1024;

    private static final long ARCHIVE_BYTES_ALLOWED_TO_BE_READ_BUT_NOT_YET_WRITTEN = 1024L * 1024L;

    private static final int ARCHIVE_BYTES_DELIVERED_BEFORE_THE_INJECTED_FAILURE = 64 * 1024;

    private static final long RESIDENCY_OBSERVATION_INTERVAL_BYTES = 4096L;

    private static final String SYMLINK_LIST_ENTRY_NAME = "SYMLINKS.txt";

    private static final String ENTRY_NAME_PREFIX = "share/bootstrap-payload-";

    private static final long ARCHIVE_CONTENT_SEED = 20283252L;

    private static final String MISSING_STREAMING_INSTALLATION_ENTRY_POINT =
        "TermuxInstaller exposes no bootstrap installation entry point that accepts the archive as a"
            + " java.io.InputStream together with a target directory. The archive is only reachable through"
            + " loadZipBytes(), which hands the caller the whole archive as one byte array, so installation"
            + " cannot be driven from a source that is larger than the memory available to hold it, and a"
            + " failure raised while the archive is being obtained cannot be observed by any caller.";

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void installationReadsTheArchiveIncrementallyInsteadOfHoldingItAllBeforeWritingAnything() throws Throwable {
        Method entryPoint = requireStreamingInstallationEntryPoint();
        File archive = writeSyntheticBootstrapArchive();
        File targetDirectory = temporaryFolder.newFolder("prefix-staging-incremental");

        ArchiveResidencyObservingInputStream observedArchive =
            new ArchiveResidencyObservingInputStream(new FileInputStream(archive), targetDirectory);

        Object returnedValue;
        try {
            returnedValue = invokeStreamingInstallation(entryPoint, observedArchive, targetDirectory);
        } finally {
            observedArchive.close();
        }

        assertNoFailureWasReported(returnedValue);

        for (int index = 0; index < ARCHIVE_ENTRY_COUNT; index++) {
            File extracted = new File(targetDirectory, entryName(index));
            assertTrue("archive entry " + entryName(index) + " was not extracted into the target directory",
                extracted.isFile());
            assertEquals("archive entry " + entryName(index) + " was extracted with the wrong length",
                ARCHIVE_ENTRY_BYTES, extracted.length());
        }

        assertTrue("installation held " + observedArchive.maximumArchiveBytesReadButNotYetWritten
                + " bytes of the archive before writing them out, which exceeds the "
                + ARCHIVE_BYTES_ALLOWED_TO_BE_READ_BUT_NOT_YET_WRITTEN + " byte bound and means the archive"
                + " is being materialised rather than streamed",
            observedArchive.maximumArchiveBytesReadButNotYetWritten
                <= ARCHIVE_BYTES_ALLOWED_TO_BE_READ_BUT_NOT_YET_WRITTEN);
    }

    @Test
    public void installationSurfacesAnInputFailureRaisedPartWayThroughTheArchive() throws Throwable {
        IOException injectedInputFailure =
            new IOException("bootstrap archive source stopped delivering bytes part way through");

        assertFailureReachesTheCaller(injectedInputFailure, "prefix-staging-input-failure");
    }

    @Test
    public void installationSurfacesAnAllocationFailureRaisedPartWayThroughTheArchive() throws Throwable {
        OutOfMemoryError injectedAllocationFailure =
            new OutOfMemoryError("failed to allocate a bootstrap archive buffer");

        assertFailureReachesTheCaller(injectedAllocationFailure, "prefix-staging-allocation-failure");
    }

    private void assertFailureReachesTheCaller(Throwable injectedFailure, String targetDirectoryName) throws Throwable {
        Method entryPoint = requireStreamingInstallationEntryPoint();
        File archive = writeSyntheticBootstrapArchive();
        File targetDirectory = temporaryFolder.newFolder(targetDirectoryName);

        FailingInputStream failingArchive = new FailingInputStream(new FileInputStream(archive),
            ARCHIVE_BYTES_DELIVERED_BEFORE_THE_INJECTED_FAILURE, injectedFailure);

        Object returnedValue = null;
        Throwable surfacedThrowable = null;
        try {
            returnedValue = invokeStreamingInstallation(entryPoint, failingArchive, targetDirectory);
        } catch (Throwable throwable) {
            surfacedThrowable = throwable;
        } finally {
            failingArchive.close();
        }

        if (surfacedThrowable != null) {
            assertSame("installation surfaced a failure that is not the one raised by the archive source",
                injectedFailure, rootCauseOf(surfacedThrowable));
            return;
        }

        assertTrue("installation returned " + describe(returnedValue) + " although the archive source failed with "
                + injectedFailure + ", so the caller is never told that the installation failed",
            returnedValue instanceof Error);
    }

    private void assertNoFailureWasReported(Object returnedValue) {
        if (returnedValue instanceof Error) {
            fail("installation of an intact archive reported the failure " + ((Error) returnedValue).getMessage());
        }
    }

    private static Method requireStreamingInstallationEntryPoint() {
        List<Method> candidates = findStreamingInstallationEntryPoints();
        if (candidates.isEmpty()) {
            fail(MISSING_STREAMING_INSTALLATION_ENTRY_POINT);
        }
        if (candidates.size() > 1) {
            fail("TermuxInstaller exposes more than one bootstrap installation entry point that accepts the"
                + " archive as a java.io.InputStream together with a target directory, so this test cannot tell"
                + " which one callers are expected to use: " + candidates);
        }
        return candidates.get(0);
    }

    private Object invokeStreamingInstallation(Method entryPoint, InputStream archive, File targetDirectory)
        throws Throwable {
        Object[] arguments = new Object[entryPoint.getParameterTypes().length];
        Class<?>[] parameterTypes = entryPoint.getParameterTypes();
        for (int index = 0; index < parameterTypes.length; index++) {
            if (InputStream.class.isAssignableFrom(parameterTypes[index])) {
                arguments[index] = archive;
            } else if (File.class.equals(parameterTypes[index])) {
                arguments[index] = targetDirectory;
            } else {
                arguments[index] = targetDirectory.getAbsolutePath();
            }
        }

        try {
            return entryPoint.invoke(null, arguments);
        } catch (InvocationTargetException invocationFailure) {
            throw invocationFailure.getCause();
        }
    }

    private static List<Method> findStreamingInstallationEntryPoints() {
        List<Method> publiclyReachable = new ArrayList<>();
        List<Method> anyVisibility = new ArrayList<>();

        for (Method method : TermuxInstaller.class.getDeclaredMethods()) {
            if (!Modifier.isStatic(method.getModifiers())) continue;
            if (method.isSynthetic()) continue;

            Class<?>[] parameterTypes = method.getParameterTypes();
            if (parameterTypes.length != 2) continue;

            int archiveParameters = 0;
            int targetDirectoryParameters = 0;
            for (Class<?> parameterType : parameterTypes) {
                if (InputStream.class.isAssignableFrom(parameterType)) {
                    archiveParameters++;
                } else if (File.class.equals(parameterType) || String.class.equals(parameterType)) {
                    targetDirectoryParameters++;
                }
            }
            if (archiveParameters != 1 || targetDirectoryParameters != 1) continue;

            method.setAccessible(true);
            anyVisibility.add(method);
            if (Modifier.isPublic(method.getModifiers())) publiclyReachable.add(method);
        }

        return publiclyReachable.isEmpty() ? anyVisibility : publiclyReachable;
    }

    private static Throwable rootCauseOf(Throwable throwable) {
        Throwable cause = throwable;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        return cause;
    }

    private static String describe(Object value) {
        return value == null ? "nothing" : value.getClass().getName() + " " + value;
    }

    private static String entryName(int index) {
        return ENTRY_NAME_PREFIX + index + ".bin";
    }

    private File writeSyntheticBootstrapArchive() throws IOException {
        File archive = temporaryFolder.newFile("bootstrap-" + System.nanoTime() + ".zip");
        Random contentGenerator = new Random(ARCHIVE_CONTENT_SEED);
        byte[] entryContent = new byte[ARCHIVE_ENTRY_BYTES];

        try (ZipOutputStream archiveOutput = new ZipOutputStream(new FileOutputStream(archive))) {
            archiveOutput.putNextEntry(new ZipEntry(SYMLINK_LIST_ENTRY_NAME));
            archiveOutput.write(symlinkListContent().getBytes(StandardCharsets.UTF_8));
            archiveOutput.closeEntry();

            for (int index = 0; index < ARCHIVE_ENTRY_COUNT; index++) {
                contentGenerator.nextBytes(entryContent);
                archiveOutput.putNextEntry(new ZipEntry(entryName(index)));
                archiveOutput.write(entryContent);
                archiveOutput.closeEntry();
            }
        }
        return archive;
    }

    private static String symlinkListContent() {
        List<String> lines = new ArrayList<>();
        lines.add(entryName(0) + "←" + ENTRY_NAME_PREFIX + "alias.bin");
        return String.join("\n", lines) + "\n";
    }

    private static long bytesWrittenInto(File directory) {
        File[] children = directory.listFiles();
        if (children == null) return 0L;

        long total = 0L;
        for (File child : children) {
            if (child.isDirectory()) {
                total += bytesWrittenInto(child);
            } else {
                total += child.length();
            }
        }
        return total;
    }

    private static final class ArchiveResidencyObservingInputStream extends InputStream {

        private final InputStream delegate;

        private final File observedTargetDirectory;

        private long archiveBytesRead;

        private long archiveBytesReadAtLastObservation;

        private long maximumArchiveBytesReadButNotYetWritten;

        private ArchiveResidencyObservingInputStream(InputStream delegate, File observedTargetDirectory) {
            this.delegate = delegate;
            this.observedTargetDirectory = observedTargetDirectory;
        }

        @Override
        public int read() throws IOException {
            int value = delegate.read();
            if (value != -1) {
                archiveBytesRead++;
                observeResidency();
            }
            return value;
        }

        @Override
        public int read(byte[] buffer, int offset, int length) throws IOException {
            int count = delegate.read(buffer, offset, length);
            if (count > 0) {
                archiveBytesRead += count;
                observeResidency();
            }
            return count;
        }

        @Override
        public void close() throws IOException {
            delegate.close();
        }

        private void observeResidency() {
            if (archiveBytesRead - archiveBytesReadAtLastObservation < RESIDENCY_OBSERVATION_INTERVAL_BYTES) return;

            archiveBytesReadAtLastObservation = archiveBytesRead;
            long readButNotYetWritten = archiveBytesRead - bytesWrittenInto(observedTargetDirectory);
            if (readButNotYetWritten > maximumArchiveBytesReadButNotYetWritten) {
                maximumArchiveBytesReadButNotYetWritten = readButNotYetWritten;
            }
        }
    }

    private static final class FailingInputStream extends InputStream {

        private final InputStream delegate;

        private final int bytesDeliveredBeforeTheFailure;

        private final Throwable failure;

        private int bytesDelivered;

        private FailingInputStream(InputStream delegate, int bytesDeliveredBeforeTheFailure, Throwable failure) {
            this.delegate = delegate;
            this.bytesDeliveredBeforeTheFailure = bytesDeliveredBeforeTheFailure;
            this.failure = failure;
        }

        @Override
        public int read() throws IOException {
            raiseFailureWhenTheDeliveryBudgetIsSpent();
            int value = delegate.read();
            if (value != -1) bytesDelivered++;
            return value;
        }

        @Override
        public int read(byte[] buffer, int offset, int length) throws IOException {
            raiseFailureWhenTheDeliveryBudgetIsSpent();
            int remaining = bytesDeliveredBeforeTheFailure - bytesDelivered;
            int count = delegate.read(buffer, offset, Math.min(length, remaining));
            if (count > 0) bytesDelivered += count;
            return count;
        }

        @Override
        public void close() throws IOException {
            delegate.close();
        }

        private void raiseFailureWhenTheDeliveryBudgetIsSpent() throws IOException {
            if (bytesDelivered < bytesDeliveredBeforeTheFailure) return;

            if (failure instanceof IOException) throw (IOException) failure;
            if (failure instanceof RuntimeException) throw (RuntimeException) failure;
            throw (java.lang.Error) failure;
        }
    }
}
