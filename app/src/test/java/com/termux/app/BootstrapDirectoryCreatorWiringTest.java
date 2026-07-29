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
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@RunWith(RobolectricTestRunner.class)
public class BootstrapDirectoryCreatorWiringTest {

    private static final String SHARED_FILE_VALIDATOR_ERROR_MARKER = "FileUtils Error:";

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void theInstallationPathCreatesBootstrapDirectoriesThroughTheSharedFileValidator() throws Exception {
        File targetDirectory = temporaryFolder.newFolder("prefix-staging-installation-path");

        try {
            TermuxInstaller.extractBootstrapArchive(
                new ByteArrayInputStream(buildArchiveWithSymlinkListAndSingleExecutableEntry()), targetDirectory);
            Assert.fail("The installation path must create bootstrap directories through the shared file validator,"
                + " which resolves a file type without following symbolic links. That validator answers only on a"
                + " device, so extraction driven here must surface its error rather than complete.");
        } catch (IOException expected) {
            Assert.assertTrue("The installation path created a bootstrap directory without the shared file"
                    + " validator, so a symbolic link standing where a bootstrap directory has to be created would"
                    + " no longer be rejected in production: " + expected.getMessage(),
                expected.getMessage().contains(SHARED_FILE_VALIDATOR_ERROR_MARKER));
        }
    }

    @Test
    public void theInjectedDirectoryCreatorIsTheOnlyThingThatDecidesHowBootstrapDirectoriesAreCreated()
        throws Exception {
        File targetDirectory = temporaryFolder.newFolder("prefix-staging-injected-creator");

        TermuxInstaller.extractBootstrapArchive(
            new ByteArrayInputStream(buildArchiveWithSymlinkListAndSingleExecutableEntry()), targetDirectory,
            new CanonicalPathBootstrapDirectoryCreator());

        Assert.assertTrue(new File(targetDirectory, "bin/login").isFile());
    }

    @Test
    public void productionSourceCarriesNoTestDirectoryCreator() throws IOException {
        File productionSourceRoot = new File(locateRepositoryRoot(), "app/src/main/java");
        String testDirectoryCreatorName = CanonicalPathBootstrapDirectoryCreator.class.getSimpleName();

        for (File productionSourceFile : listJavaFilesUnder(productionSourceRoot)) {
            String productionSource = new String(
                Files.readAllBytes(productionSourceFile.toPath()), StandardCharsets.UTF_8);
            Assert.assertFalse("The bootstrap directory creator written for tests appears in the production source "
                    + productionSourceFile.getAbsolutePath()
                    + ", so production can reach a directory creator that does not use the shared file validator.",
                productionSource.contains(testDirectoryCreatorName));
        }
    }

    private List<File> listJavaFilesUnder(File directory) {
        List<File> javaFiles = new ArrayList<>();
        File[] children = directory.listFiles();
        Assert.assertNotNull(directory.getAbsolutePath(), children);
        for (File child : children) {
            if (child.isDirectory()) {
                javaFiles.addAll(listJavaFilesUnder(child));
            } else if (child.getName().endsWith(".java")) {
                javaFiles.add(child);
            }
        }
        return javaFiles;
    }

    private File locateRepositoryRoot() {
        File candidate = Paths.get("").toAbsolutePath().toFile();
        while (candidate != null && !new File(candidate, "settings.gradle").isFile()) {
            candidate = candidate.getParentFile();
        }
        Assert.assertNotNull("The repository root carrying settings.gradle was not found.", candidate);
        return candidate;
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
