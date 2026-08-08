package com.termux.app.browser;

import org.junit.Assert;
import org.junit.Test;

public class BrowserDownloadedPdfRecognitionTest {

    @Test
    public void aPdfIsRecognisedFromItsOwnFileNameWhenTheServerDescribedItGenerically() {
        Assert.assertTrue("a server that names no format still sent a PDF, and the user who tapped"
                + " a PDF link is owed the document rather than a file listing",
            BrowserDownloadedDocumentDisplay.displaysAsDocument("application/octet-stream", "statement.pdf"));
    }

    @Test
    public void aPdfIsRecognisedFromItsFileNameWhenTheServerNamedNoFormatAtAll() {
        Assert.assertTrue("a download with no media type at all is still a PDF when the file it"
                + " produced is one",
            BrowserDownloadedDocumentDisplay.displaysAsDocument(null, "STATEMENT.PDF"));
    }

    @Test
    public void aFormatTheServerNamedIsBelievedOverTheFileName() {
        Assert.assertFalse("a server that named a different format is describing what it actually"
                + " sent, so the file name must not override it",
            BrowserDownloadedDocumentDisplay.displaysAsDocument("application/zip", "archive.pdf"));
    }

    @Test
    public void aFileThatIsNotAPdfIsNotTreatedAsOne() {
        Assert.assertFalse("only a PDF can be displayed as a document",
            BrowserDownloadedDocumentDisplay.displaysAsDocument("application/octet-stream", "archive.zip"));
    }
}
