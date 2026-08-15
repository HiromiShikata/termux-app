package com.termux.app.browser;

import org.junit.Assert;
import org.junit.Test;

public class BrowserClipboardUrlOfferTest {

    @Test
    public void aClipboardHoldingASecureAddressIsOfferedCarryingThatAddress() {
        BrowserClipboardUrlOffer offer = BrowserClipboardUrlOffer.of("https://example.com/a/page?q=1");

        Assert.assertTrue("a copied web address is the whole reason the owner opens a new tab, so it has"
                + " to be offered", offer.isOffered());
        Assert.assertEquals("the offer opens what was copied, not a rewritten form of it",
            "https://example.com/a/page?q=1", offer.getUrl());
    }

    @Test
    public void aClipboardHoldingAPlainAddressIsOfferedToo() {
        Assert.assertTrue("an address that is not served over a secure connection is still an address the"
                + " owner copied in order to open it",
            BrowserClipboardUrlOffer.of("http://example.com").isOffered());
    }

    @Test
    public void surroundingWhitespaceIsRemovedFromTheOfferedAddress() {
        BrowserClipboardUrlOffer offer = BrowserClipboardUrlOffer.of("  https://example.com/page\n");

        Assert.assertTrue("selecting a link on a page routinely takes the newline after it with it, and"
                + " that is still the address the owner copied", offer.isOffered());
        Assert.assertEquals("the trailing newline would be sent to the network as part of the path",
            "https://example.com/page", offer.getUrl());
    }

    @Test
    public void theSchemeIsReadWithoutRegardToItsCase() {
        Assert.assertTrue("a scheme is case insensitive, so an address copied from a source that upper"
                + " cases it is the same address",
            BrowserClipboardUrlOffer.of("HTTPS://example.com").isOffered());
    }

    @Test
    public void aClipboardHoldingPlainTextIsNotOffered() {
        Assert.assertFalse("offering a control that opens ordinary copied text would send whatever the"
                + " owner last copied to a search engine on a single tap",
            BrowserClipboardUrlOffer.of("the meeting is at four").isOffered());
    }

    @Test
    public void anEmptyClipboardIsNotOffered() {
        Assert.assertFalse("nothing was copied, so there is nothing to open",
            BrowserClipboardUrlOffer.of("   ").isOffered());
        Assert.assertFalse("a clipboard the platform reports as absent is not an address either",
            BrowserClipboardUrlOffer.of(null).isOffered());
    }

    @Test
    public void anAddressWithWhitespaceInsideItIsNotOffered() {
        Assert.assertFalse("a copied sentence that happens to start with an address is not one address,"
                + " and opening its first part would take the owner somewhere he did not copy",
            BrowserClipboardUrlOffer.of("https://example.com and then call me").isOffered());
    }

    @Test
    public void aSchemeWithNoAddressBehindItIsNotOffered() {
        Assert.assertFalse("a scheme on its own names no page, so there is nothing to open",
            BrowserClipboardUrlOffer.of("https://").isOffered());
    }

    @Test
    public void anAddressThatNamesNoSiteBehindItsSchemeIsNotOffered() {
        Assert.assertFalse("the control that opens it sits in the dialog's button row and has room only"
                + " for the site, so an address carrying no site names nothing the owner could read",
            BrowserClipboardUrlOffer.of("https://?q=1").isOffered());
        Assert.assertFalse("an empty site between the scheme and the path is the same absence",
            BrowserClipboardUrlOffer.of("https:///a/page").isOffered());
    }

    @Test
    public void theSiteTheAddressWouldOpenIsNamedOnItsOwn() {
        Assert.assertEquals("a button sharing a row with the open and cancel buttons has room for the"
                + " site rather than for a whole address, and the site is what tells the owner where a"
                + " press would take him",
            "example.com", BrowserClipboardUrlOffer.of("https://example.com/a/page?q=1").getHost());
        Assert.assertEquals("an address ending at its site names that site",
            "example.com", BrowserClipboardUrlOffer.of("http://example.com").getHost());
        Assert.assertEquals("a fragment ends the site exactly as a path does",
            "example.com", BrowserClipboardUrlOffer.of("https://example.com#section").getHost());
    }

    @Test
    public void theSiteIsNamedWithoutTheCredentialsAndPortAroundIt() {
        Assert.assertEquals("credentials copied inside an address are not where it goes, and putting"
                + " them on a button would show them on screen",
            "example.com",
            BrowserClipboardUrlOffer.of("https://someone:secret@example.com:8443/page").getHost());
    }

    @Test
    public void anAddressWrittenToANumericSiteNamesThatSiteWholeRatherThanCutAtItsFirstColon() {
        Assert.assertEquals("cutting a numeric address at its first colon would name a site that does"
                + " not exist and tell the owner nothing about where a press would take him",
            "[2001:db8::1]", BrowserClipboardUrlOffer.of("http://[2001:db8::1]:8080/page").getHost());
    }

    @Test
    public void anOfferThatWasNotMadeRefusesToNameASite() {
        BrowserClipboardUrlOffer offer = BrowserClipboardUrlOffer.of("the meeting is at four");

        try {
            offer.getHost();
            Assert.fail("an empty site would be drawn on a button that opens nothing rather than"
                + " reported as the absence it is");
        } catch (IllegalStateException expected) {
            Assert.assertTrue("the refusal has to say what was asked for. Actual: " + expected.getMessage(),
                expected.getMessage().contains("clipboard"));
        }
    }

    @Test
    public void anOfferThatWasNotMadeRefusesToNameAnAddress() {
        BrowserClipboardUrlOffer offer = BrowserClipboardUrlOffer.of("the meeting is at four");

        try {
            offer.getUrl();
            Assert.fail("returning an empty address here would be opened as a blank page rather than"
                + " reported as the absence it is");
        } catch (IllegalStateException expected) {
            Assert.assertTrue("the refusal has to say what was asked for. Actual: " + expected.getMessage(),
                expected.getMessage().contains("clipboard"));
        }
    }
}
