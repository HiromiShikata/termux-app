package com.termux.app.browser;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class BrowserDisplaysDownloadedDocumentTest {

    private static final String DOWNLOAD_CONTROLLER_PATH =
        "src/main/java/com/termux/app/browser/BrowserDownloadController.java";

    private String readModuleSource(String relativePath) throws IOException {
        Path moduleRelative = Paths.get(relativePath);
        if (Files.exists(moduleRelative)) {
            return new String(Files.readAllBytes(moduleRelative), StandardCharsets.UTF_8);
        }
        Path repoRelative = Paths.get("app").resolve(relativePath);
        return new String(Files.readAllBytes(repoRelative), StandardCharsets.UTF_8);
    }

    private String blockStartingAt(String source, String marker, String terminator) {
        int markerIndex = source.indexOf(marker);
        Assert.assertTrue("marker not found: " + marker, markerIndex >= 0);
        int blockEnd = source.indexOf(terminator, markerIndex);
        Assert.assertTrue("terminator not found after: " + marker, blockEnd > markerIndex);
        return source.substring(markerIndex, blockEnd);
    }

    @Test
    public void aDocumentThatFinishedDownloadingIsDisplayedRatherThanListed() throws IOException {
        String receiver = blockStartingAt(readModuleSource(DOWNLOAD_CONTROLLER_PATH),
            "if (!mEnqueuedDownloadIds.remove(completedId)) return;", "\n            }");

        Assert.assertTrue(
            "a document the browser could not render is downloaded instead, so finishing the"
                + " download must display it rather than only list it in the downloads view",
            receiver.contains("displayDownloadedDocumentOrOpenDownloadsView(completedId)"));
    }

    @Test
    public void displayingTheDownloadedDocumentOpensTheFileItself() throws IOException {
        String display = blockStartingAt(readModuleSource(DOWNLOAD_CONTROLLER_PATH),
            "private void displayDownloadedDocumentOrOpenDownloadsView(long downloadId)", "\n    }");

        Assert.assertTrue("the decision of what counts as a displayable document must be its own unit",
            display.contains("BrowserDownloadedDocumentDisplay.displaysAsDocument("));
        Assert.assertTrue("the file that was downloaded is what the user asked to see",
            display.contains("getUriForDownloadedFile(downloadId)"));
        Assert.assertTrue("displaying a document is a view action on that document",
            display.contains("Intent.ACTION_VIEW"));
    }
}
