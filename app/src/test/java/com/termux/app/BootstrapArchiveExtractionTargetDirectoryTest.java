package com.termux.app;

import com.termux.app.bootstrap.CanonicalPathBootstrapDirectoryCreator;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@RunWith(RobolectricTestRunner.class)
public class BootstrapArchiveExtractionTargetDirectoryTest {

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void surfacesANonDirectoryFileBlockingAnEntryDirectory() throws Exception {
        File targetDirectory = temporaryFolder.newFolder("prefix-staging-blocked");
        File blockingFile = new File(targetDirectory, "bin");
        try (FileOutputStream blockingFileOutput = new FileOutputStream(blockingFile)) {
            blockingFileOutput.write("not a directory".getBytes(StandardCharsets.UTF_8));
        }

        try {
            TermuxInstaller.extractBootstrapArchive(
                new ByteArrayInputStream(buildArchiveWithSingleExecutableEntry()), targetDirectory,
                new CanonicalPathBootstrapDirectoryCreator());
            Assert.fail("A non-directory file blocking an entry directory must surface as a reported failure.");
        } catch (IOException expected) {
            Assert.assertTrue(expected.getMessage(), expected.getMessage().contains("Non-directory file found"));
            Assert.assertTrue(expected.getMessage(), expected.getMessage().contains(blockingFile.getAbsolutePath()));
        }
    }

    @Test
    public void surfacesASymbolicLinkStandingWhereAnEntryDirectoryMustBeCreated() throws Exception {
        File targetDirectory = temporaryFolder.newFolder("prefix-staging-symlinked");
        File directoryOutsideTheTargetDirectory = temporaryFolder.newFolder("outside-the-prefix-staging");
        File linkStandingWhereTheEntryDirectoryMustBeCreated = new File(targetDirectory, "bin");
        Files.createSymbolicLink(linkStandingWhereTheEntryDirectoryMustBeCreated.toPath(),
            directoryOutsideTheTargetDirectory.toPath());

        try {
            TermuxInstaller.extractBootstrapArchive(
                new ByteArrayInputStream(buildArchiveWithSymlinkListAndSingleExecutableEntry()), targetDirectory,
                new CanonicalPathBootstrapDirectoryCreator());
            Assert.fail("A symbolic link standing where a bootstrap directory has to be created must surface as a"
                + " reported failure instead of being written through.");
        } catch (IOException expected) {
            Assert.assertTrue(expected.getMessage(), expected.getMessage().contains("Non-directory file found"));
            Assert.assertTrue(expected.getMessage(),
                expected.getMessage().contains(linkStandingWhereTheEntryDirectoryMustBeCreated.getAbsolutePath()));
        }

        String[] filesWrittenOutsideTheTargetDirectory = directoryOutsideTheTargetDirectory.list();
        Assert.assertNotNull(filesWrittenOutsideTheTargetDirectory);
        Assert.assertEquals("Extraction wrote through the symbolic link into "
                + directoryOutsideTheTargetDirectory.getAbsolutePath()
                + ", which is outside the target directory " + targetDirectory.getAbsolutePath() + ".",
            0, filesWrittenOutsideTheTargetDirectory.length);
    }

    @Test
    public void surfacesAnArchiveWithoutASymlinkList() throws Exception {
        File targetDirectory = temporaryFolder.newFolder("prefix-staging-without-symlinks");

        try {
            TermuxInstaller.extractBootstrapArchive(
                new ByteArrayInputStream(buildArchiveWithSingleExecutableEntry()), targetDirectory,
                new CanonicalPathBootstrapDirectoryCreator());
            Assert.fail("An archive without a symlink list must surface as a reported failure.");
        } catch (RuntimeException expected) {
            Assert.assertEquals("No SYMLINKS.txt encountered", expected.getMessage());
        }
    }

    private byte[] buildArchiveWithSingleExecutableEntry() throws IOException {
        ByteArrayOutputStream archiveBytes = new ByteArrayOutputStream();
        try (ZipOutputStream archiveOutput = new ZipOutputStream(archiveBytes)) {
            writeExecutableEntry(archiveOutput);
        }
        return archiveBytes.toByteArray();
    }

    private byte[] buildArchiveWithSymlinkListAndSingleExecutableEntry() throws IOException {
        ByteArrayOutputStream archiveBytes = new ByteArrayOutputStream();
        try (ZipOutputStream archiveOutput = new ZipOutputStream(archiveBytes)) {
            archiveOutput.putNextEntry(new ZipEntry("SYMLINKS.txt"));
            archiveOutput.write("bin/login←bin/sh\n".getBytes(StandardCharsets.UTF_8));
            archiveOutput.closeEntry();
            writeExecutableEntry(archiveOutput);
        }
        return archiveBytes.toByteArray();
    }

    private void writeExecutableEntry(ZipOutputStream archiveOutput) throws IOException {
        archiveOutput.putNextEntry(new ZipEntry("bin/login"));
        archiveOutput.write("bootstrap payload".getBytes(StandardCharsets.UTF_8));
        archiveOutput.closeEntry();
    }
}
