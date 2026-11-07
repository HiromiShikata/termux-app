package com.termux.app.api.file;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class ReceivedFileDestinationTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private static final class NameCase {
        final String requestedName;
        final ReceivedFileDestinationStatus expectedStatus;
        final String expectedFileName;

        NameCase(String requestedName, ReceivedFileDestinationStatus expectedStatus, String expectedFileName) {
            this.requestedName = requestedName;
            this.expectedStatus = expectedStatus;
            this.expectedFileName = expectedFileName;
        }
    }

    @Test
    public void testResolveReducesRequestedNameToLastSegment() throws IOException {
        File receiveDir = temporaryFolder.newFolder("downloads");

        List<NameCase> cases = new ArrayList<>();
        cases.add(new NameCase("../bin/termux-file-editor", ReceivedFileDestinationStatus.ACCEPTED, "termux-file-editor"));
        cases.add(new NameCase("a/b/c.txt", ReceivedFileDestinationStatus.ACCEPTED, "c.txt"));
        cases.add(new NameCase("plain.txt", ReceivedFileDestinationStatus.ACCEPTED, "plain.txt"));
        cases.add(new NameCase("", ReceivedFileDestinationStatus.EMPTY_NAME, null));
        cases.add(new NameCase(null, ReceivedFileDestinationStatus.EMPTY_NAME, null));
        cases.add(new NameCase("dir/", ReceivedFileDestinationStatus.EMPTY_NAME, null));
        cases.add(new NameCase(".", ReceivedFileDestinationStatus.CURRENT_OR_PARENT_DIRECTORY_NAME, null));
        cases.add(new NameCase("..", ReceivedFileDestinationStatus.CURRENT_OR_PARENT_DIRECTORY_NAME, null));
        cases.add(new NameCase("a/..", ReceivedFileDestinationStatus.CURRENT_OR_PARENT_DIRECTORY_NAME, null));

        for (NameCase nameCase : cases) {
            ReceivedFileDestination destination = ReceivedFileDestination.resolve(receiveDir, nameCase.requestedName);
            Assert.assertEquals("requestedName=" + nameCase.requestedName, nameCase.expectedStatus, destination.getStatus());
            if (nameCase.expectedFileName == null) {
                Assert.assertNull("requestedName=" + nameCase.requestedName, destination.getFile());
            } else {
                Assert.assertEquals("requestedName=" + nameCase.requestedName,
                    new File(receiveDir, nameCase.expectedFileName).getAbsolutePath(),
                    destination.getFile().getAbsolutePath());
            }
        }
    }

    @Test
    public void testResolveDoesNotWriteOutsideReceiveDirectoryForParentSegmentName() throws IOException {
        File rootDir = temporaryFolder.newFolder("root");
        File receiveDir = new File(rootDir, "downloads");
        Assert.assertTrue(receiveDir.mkdirs());

        ReceivedFileDestination destination = ReceivedFileDestination.resolve(receiveDir, "../bin/termux-file-editor");
        Assert.assertEquals(ReceivedFileDestinationStatus.ACCEPTED, destination.getStatus());
        destination.writeContent(new ByteArrayInputStream("received".getBytes(StandardCharsets.UTF_8)));

        Assert.assertFalse(new File(rootDir, "bin/termux-file-editor").exists());
        Assert.assertEquals("received", readFileAsString(new File(receiveDir, "termux-file-editor")));
    }

    @Test
    public void testResolveAndWriteSavesPlainNameIntoReceiveDirectoryWithContent() throws IOException {
        File receiveDir = temporaryFolder.newFolder("downloads");

        ReceivedFileDestination destination = ReceivedFileDestination.resolve(receiveDir, "plain.txt");
        Assert.assertEquals(ReceivedFileDestinationStatus.ACCEPTED, destination.getStatus());
        destination.writeContent(new ByteArrayInputStream("plain content".getBytes(StandardCharsets.UTF_8)));

        Assert.assertEquals("plain content", readFileAsString(new File(receiveDir, "plain.txt")));
    }

    @Test
    public void testResolveRejectsSymbolicLinkInsideReceiveDirectoryPointingOutside() throws IOException {
        File rootDir = temporaryFolder.newFolder("root");
        File receiveDir = new File(rootDir, "downloads");
        Assert.assertTrue(receiveDir.mkdirs());

        File outsideFile = new File(rootDir, "outside.txt");
        Assert.assertTrue(outsideFile.createNewFile());
        Files.createSymbolicLink(new File(receiveDir, "linked.txt").toPath(), outsideFile.toPath());

        ReceivedFileDestination destination = ReceivedFileDestination.resolve(receiveDir, "linked.txt");
        Assert.assertEquals(ReceivedFileDestinationStatus.OUTSIDE_RECEIVE_DIRECTORY, destination.getStatus());
        Assert.assertNull(destination.getFile());
    }

    @Test
    public void testResolveRejectsDanglingSymbolicLinkInsideReceiveDirectoryPointingOutside() throws IOException {
        File rootDir = temporaryFolder.newFolder("root");
        File receiveDir = new File(rootDir, "downloads");
        Assert.assertTrue(receiveDir.mkdirs());

        Files.createSymbolicLink(new File(receiveDir, "dangling.txt").toPath(), new File(rootDir, "missing.txt").toPath());

        ReceivedFileDestination destination = ReceivedFileDestination.resolve(receiveDir, "dangling.txt");
        Assert.assertEquals(ReceivedFileDestinationStatus.OUTSIDE_RECEIVE_DIRECTORY, destination.getStatus());
        Assert.assertNull(destination.getFile());
    }

    private static String readFileAsString(File file) throws IOException {
        return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
    }
}
