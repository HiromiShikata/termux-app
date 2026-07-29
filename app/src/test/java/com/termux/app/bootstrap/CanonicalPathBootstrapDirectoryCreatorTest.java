package com.termux.app.bootstrap;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class CanonicalPathBootstrapDirectoryCreatorTest {

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    private final CanonicalPathBootstrapDirectoryCreator directoryCreator =
        new CanonicalPathBootstrapDirectoryCreator();

    @Test
    public void createsAMissingDirectoryTogetherWithItsMissingParents() throws IOException {
        File missingDirectory = new File(temporaryFolder.newFolder("prefix-staging"), "lib/apt/methods");

        directoryCreator.createBootstrapDirectory(missingDirectory);

        Assert.assertTrue(missingDirectory.getAbsolutePath(), missingDirectory.isDirectory());
    }

    @Test
    public void acceptsADirectoryThatAlreadyExists() throws IOException {
        File existingDirectory = temporaryFolder.newFolder("prefix-staging-existing");

        directoryCreator.createBootstrapDirectory(existingDirectory);

        Assert.assertTrue(existingDirectory.getAbsolutePath(), existingDirectory.isDirectory());
    }

    @Test
    public void rejectsARegularFileStandingWhereTheDirectoryHasToBeCreated() throws IOException {
        File regularFile = temporaryFolder.newFile("prefix-staging-regular-file");

        assertRejectsAsNonDirectory(regularFile);
    }

    @Test
    public void rejectsASymbolicLinkToADirectoryStandingWhereTheDirectoryHasToBeCreated() throws IOException {
        File linkTargetDirectory = temporaryFolder.newFolder("link-target-directory");
        File link = new File(temporaryFolder.getRoot(), "link-to-a-directory");
        Files.createSymbolicLink(link.toPath(), linkTargetDirectory.toPath());

        assertRejectsAsNonDirectory(link);

        String[] filesWrittenThroughTheLink = linkTargetDirectory.list();
        Assert.assertNotNull(filesWrittenThroughTheLink);
        Assert.assertEquals(linkTargetDirectory.getAbsolutePath(), 0, filesWrittenThroughTheLink.length);
    }

    @Test
    public void rejectsADanglingSymbolicLinkStandingWhereTheDirectoryHasToBeCreated() throws IOException {
        File danglingLink = new File(temporaryFolder.getRoot(), "dangling-link");
        Files.createSymbolicLink(danglingLink.toPath(),
            new File(temporaryFolder.getRoot(), "a-path-that-does-not-exist").toPath());

        assertRejectsAsNonDirectory(danglingLink);
    }

    @Test
    public void rejectsAChainOfSymbolicLinksEndingAtADirectory() throws IOException {
        File linkTargetDirectory = temporaryFolder.newFolder("chain-target-directory");
        File innerLink = new File(temporaryFolder.getRoot(), "inner-link");
        Files.createSymbolicLink(innerLink.toPath(), linkTargetDirectory.toPath());
        File outerLink = new File(temporaryFolder.getRoot(), "outer-link");
        Files.createSymbolicLink(outerLink.toPath(), innerLink.toPath());

        assertRejectsAsNonDirectory(outerLink);
    }

    @Test
    public void createsADirectoryUnderASymbolicLinkParentAsTheSharedValidatorDoes() throws IOException {
        File parentTargetDirectory = temporaryFolder.newFolder("parent-target-directory");
        File linkParent = new File(temporaryFolder.getRoot(), "link-parent");
        Files.createSymbolicLink(linkParent.toPath(), parentTargetDirectory.toPath());

        directoryCreator.createBootstrapDirectory(new File(linkParent, "child"));

        Assert.assertTrue(parentTargetDirectory.getAbsolutePath(),
            new File(parentTargetDirectory, "child").isDirectory());
    }

    @Test
    public void reportsThePathWhenTheDirectoryCannotBeCreated() throws IOException {
        File regularFile = temporaryFolder.newFile("prefix-staging-blocking-parent");
        try (FileOutputStream regularFileOutput = new FileOutputStream(regularFile)) {
            regularFileOutput.write("not a directory".getBytes(StandardCharsets.UTF_8));
        }
        File uncreatableDirectory = new File(regularFile, "usr/lib");

        try {
            directoryCreator.createBootstrapDirectory(uncreatableDirectory);
            Assert.fail("A bootstrap directory that cannot be created must be reported rather than ignored.");
        } catch (IOException expected) {
            Assert.assertTrue(expected.getMessage(), expected.getMessage().contains("failed"));
            Assert.assertTrue(expected.getMessage(),
                expected.getMessage().contains(uncreatableDirectory.getAbsolutePath()));
        }
    }

    private void assertRejectsAsNonDirectory(File blockedDirectory) {
        try {
            directoryCreator.createBootstrapDirectory(blockedDirectory);
            Assert.fail("A non-directory file standing where the bootstrap directory \""
                + blockedDirectory.getAbsolutePath() + "\" has to be created must be reported.");
        } catch (IOException expected) {
            Assert.assertTrue(expected.getMessage(), expected.getMessage().contains("Non-directory file found"));
            Assert.assertTrue(expected.getMessage(),
                expected.getMessage().contains(blockedDirectory.getAbsolutePath()));
        }
    }
}
