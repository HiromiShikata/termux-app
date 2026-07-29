package com.termux.app.bootstrap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.system.Os;
import android.system.OsConstants;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@RunWith(AndroidJUnit4.class)
public class BootstrapDirectoryCreatorEquivalenceInstrumentedTest {

    private interface DirectoryCase {

        File prepareUnder(File caseRoot) throws Exception;
    }

    private final BootstrapDirectoryCreator productionDirectoryCreator = new FileUtilsBootstrapDirectoryCreator();

    private final BootstrapDirectoryCreator offDeviceDirectoryCreator = new CanonicalPathBootstrapDirectoryCreator();

    @Test
    public void bothCreatorsCreateAMissingDirectory() throws Exception {
        assertBothCreatorsAgree("missing-directory",
            caseRoot -> new File(caseRoot, "missing/lib/apt/methods"), true);
    }

    @Test
    public void bothCreatorsAcceptAnExistingDirectory() throws Exception {
        assertBothCreatorsAgree("existing-directory", caseRoot -> {
            File existingDirectory = new File(caseRoot, "existing");
            assertTrue(existingDirectory.getAbsolutePath(), existingDirectory.mkdirs());
            return existingDirectory;
        }, true);
    }

    @Test
    public void bothCreatorsRejectARegularFile() throws Exception {
        assertBothCreatorsAgree("regular-file", caseRoot -> {
            File regularFile = new File(caseRoot, "regular-file");
            writeRegularFile(regularFile);
            return regularFile;
        }, false);
    }

    @Test
    public void bothCreatorsRejectASymbolicLinkToADirectory() throws Exception {
        assertBothCreatorsAgree("symbolic-link-to-a-directory", caseRoot -> {
            File linkTargetDirectory = new File(caseRoot, "link-target-directory");
            assertTrue(linkTargetDirectory.getAbsolutePath(), linkTargetDirectory.mkdirs());
            File link = new File(caseRoot, "link-to-a-directory");
            Os.symlink(linkTargetDirectory.getAbsolutePath(), link.getAbsolutePath());
            return link;
        }, false);
    }

    @Test
    public void bothCreatorsRejectADanglingSymbolicLink() throws Exception {
        assertBothCreatorsAgree("dangling-symbolic-link", caseRoot -> {
            File danglingLink = new File(caseRoot, "dangling-link");
            Os.symlink(new File(caseRoot, "a-path-that-does-not-exist").getAbsolutePath(),
                danglingLink.getAbsolutePath());
            return danglingLink;
        }, false);
    }

    @Test
    public void bothCreatorsRejectAFirstInFirstOutNode() throws Exception {
        assertBothCreatorsAgree("first-in-first-out-node", caseRoot -> {
            File firstInFirstOutNode = new File(caseRoot, "first-in-first-out-node");
            Os.mkfifo(firstInFirstOutNode.getAbsolutePath(), OsConstants.S_IRUSR | OsConstants.S_IWUSR);
            return firstInFirstOutNode;
        }, false);
    }

    @Test
    public void bothCreatorsRejectAChainOfSymbolicLinksEndingAtADirectory() throws Exception {
        assertBothCreatorsAgree("symbolic-link-chain", caseRoot -> {
            File linkTargetDirectory = new File(caseRoot, "link-target-directory");
            assertTrue(linkTargetDirectory.getAbsolutePath(), linkTargetDirectory.mkdirs());
            File innerLink = new File(caseRoot, "inner-link");
            Os.symlink(linkTargetDirectory.getAbsolutePath(), innerLink.getAbsolutePath());
            File outerLink = new File(caseRoot, "outer-link");
            Os.symlink(innerLink.getAbsolutePath(), outerLink.getAbsolutePath());
            return outerLink;
        }, false);
    }

    @Test
    public void bothCreatorsCreateADirectoryUnderASymbolicLinkParent() throws Exception {
        assertBothCreatorsAgree("symbolic-link-parent", caseRoot -> {
            File parentTargetDirectory = new File(caseRoot, "parent-target-directory");
            assertTrue(parentTargetDirectory.getAbsolutePath(), parentTargetDirectory.mkdirs());
            File linkParent = new File(caseRoot, "link-parent");
            Os.symlink(parentTargetDirectory.getAbsolutePath(), linkParent.getAbsolutePath());
            return new File(linkParent, "child");
        }, true);
    }

    @Test
    public void bothCreatorsRejectADirectoryThatCannotBeCreated() throws Exception {
        assertBothCreatorsAgree("uncreatable-directory", caseRoot -> {
            File regularFile = new File(caseRoot, "blocking-parent");
            writeRegularFile(regularFile);
            return new File(regularFile, "usr/lib");
        }, false);
    }

    @Test
    public void neitherCreatorWritesThroughASymbolicLinkStandingWhereADirectoryHasToBeCreated() throws Exception {
        for (BootstrapDirectoryCreator directoryCreator : allCreators()) {
            File caseRoot = newCaseRoot("no-write-through-" + directoryCreator.getClass().getSimpleName());
            File linkTargetDirectory = new File(caseRoot, "link-target-directory");
            assertTrue(linkTargetDirectory.getAbsolutePath(), linkTargetDirectory.mkdirs());
            File link = new File(caseRoot, "link-to-a-directory");
            Os.symlink(linkTargetDirectory.getAbsolutePath(), link.getAbsolutePath());

            assertEquals(directoryCreator.getClass().getName(), "rejected", outcomeOf(directoryCreator, link));

            String[] filesWrittenThroughTheLink = linkTargetDirectory.list();
            assertNotNull(filesWrittenThroughTheLink);
            assertEquals(directoryCreator.getClass().getName(), 0, filesWrittenThroughTheLink.length);
        }
    }

    private void assertBothCreatorsAgree(String caseName, DirectoryCase directoryCase, boolean expectedToBeAccepted)
        throws Exception {
        String productionOutcome = outcomeOf(productionDirectoryCreator,
            directoryCase.prepareUnder(newCaseRoot(caseName + "-production")));
        String offDeviceOutcome = outcomeOf(offDeviceDirectoryCreator,
            directoryCase.prepareUnder(newCaseRoot(caseName + "-off-device")));

        assertEquals("The bootstrap directory creator used off device disagrees with the one used in production"
            + " about the case \"" + caseName + "\".", productionOutcome, offDeviceOutcome);
        assertEquals(caseName, expectedToBeAccepted ? "accepted" : "rejected", productionOutcome);
    }

    private BootstrapDirectoryCreator[] allCreators() {
        return new BootstrapDirectoryCreator[]{productionDirectoryCreator, offDeviceDirectoryCreator};
    }

    private File newCaseRoot(String caseName) {
        File caseRoot = new File(ApplicationProvider.getApplicationContext().getCacheDir(),
            "bootstrap-directory-creator-equivalence/" + caseName + "-" + System.nanoTime());
        assertTrue(caseRoot.getAbsolutePath(), caseRoot.mkdirs());
        return caseRoot;
    }

    private static String outcomeOf(BootstrapDirectoryCreator directoryCreator, File directory) {
        try {
            directoryCreator.createBootstrapDirectory(directory);
            return "accepted";
        } catch (IOException rejection) {
            return "rejected";
        }
    }

    private static void writeRegularFile(File regularFile) throws IOException {
        try (FileOutputStream regularFileOutput = new FileOutputStream(regularFile)) {
            regularFileOutput.write("not a directory".getBytes(StandardCharsets.UTF_8));
        }
    }
}
