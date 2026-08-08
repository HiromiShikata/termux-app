package com.termux.app.browser;

import org.junit.Assert;
import org.junit.Test;

public class BrowserDownloadedDocumentDisplayTest {

    @Test
    public void aPdfIsDisplayedAsADocument() {
        Assert.assertTrue(BrowserDownloadedDocumentDisplay.displaysAsDocument("application/pdf"));
    }

    @Test
    public void theMediaTypeIsMatchedRegardlessOfCaseAndSurroundingSpace() {
        Assert.assertTrue(BrowserDownloadedDocumentDisplay.displaysAsDocument("  Application/PDF  "));
    }

    @Test
    public void aMediaTypeCarryingParametersIsStillRecognised() {
        Assert.assertTrue(BrowserDownloadedDocumentDisplay.displaysAsDocument("application/pdf; charset=binary"));
    }

    @Test
    public void anArchiveIsNotDisplayedAsADocument() {
        Assert.assertFalse(BrowserDownloadedDocumentDisplay.displaysAsDocument("application/zip"));
    }

    @Test
    public void anUnknownMediaTypeIsNotDisplayedAsADocument() {
        Assert.assertFalse(BrowserDownloadedDocumentDisplay.displaysAsDocument(null));
    }
}
