package com.termux.app.browser;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class NewTabDialogOffersTheCopiedLinkTest {

    private static final String CONTROLLER_RELATIVE_PATH =
        "src/main/java/com/termux/app/browser/TermuxBrowserController.java";

    private static final String NEW_TAB_LAYOUT_RELATIVE_PATH =
        "src/main/res/layout/dialog_browser_new_tab.xml";

    private static final String STRINGS_RELATIVE_PATH = "src/main/res/values/strings.xml";

    private String readSource(String relativePath) throws IOException {
        Path moduleRelative = Paths.get(relativePath);
        if (Files.exists(moduleRelative)) {
            return new String(Files.readAllBytes(moduleRelative), StandardCharsets.UTF_8);
        }
        Path repoRelative = Paths.get("app").resolve(relativePath);
        return new String(Files.readAllBytes(repoRelative), StandardCharsets.UTF_8);
    }

    private String methodBody(String source, String signature) {
        int methodIndex = source.indexOf(signature);
        Assert.assertTrue("method not found: " + signature, methodIndex >= 0);
        int methodEnd = source.indexOf("\n    }", methodIndex);
        Assert.assertTrue(methodEnd > methodIndex);
        return source.substring(methodIndex, methodEnd);
    }

    @Test
    public void theNewTabLayoutCarriesAControlForTheCopiedLink() throws IOException {
        String layout = readSource(NEW_TAB_LAYOUT_RELATIVE_PATH);

        Assert.assertTrue("without a control of its own, the only route from a copied address to an open"
                + " tab is a long press on the input field and a paste",
            layout.contains("@+id/browser_new_tab_clipboard_url_button"));
        Assert.assertTrue("the control must start hidden, because a clipboard holding ordinary text has"
                + " no address to open and the dialog must look exactly as it does today",
            layout.contains("android:visibility=\"gone\""));
    }

    @Test
    public void theNewTabDialogDecidesTheOfferFromTheClipboardAndOpensWhatItOffers() throws IOException {
        String body = methodBody(readSource(CONTROLLER_RELATIVE_PATH), "public void promptNewTab() {");

        Assert.assertTrue("the dialog must read the clipboard to know whether there is an address to offer",
            body.contains("getTextFromClipboard"));
        Assert.assertTrue("whether the clipboard holds an address is decided by the class that is tested"
                + " for it, not by a check written into the dialog",
            body.contains("BrowserClipboardUrlOffer.of("));
        Assert.assertTrue("the control must be shown only when there is an address to open",
            body.contains("isOffered()"));
        Assert.assertTrue("the control must wire to the same path a typed address takes, so a copied"
                + " address opens in a new tab exactly as a typed one does",
            body.contains("browser_new_tab_clipboard_url_button"));
        Assert.assertTrue("the dialog must close when the copied address opens, as it does for a typed"
                + " address and for a list entry", body.contains("dismiss()"));
    }

    @Test
    public void theControlNamesTheAddressItWouldOpen() throws IOException {
        String strings = readSource(STRINGS_RELATIVE_PATH);

        Assert.assertTrue("the owner has to see which address a tap would open before tapping it, because"
                + " the clipboard holds whatever was last copied anywhere on the device",
            strings.contains("action_browser_new_tab_open_clipboard_url"));
        Assert.assertTrue("that label has to carry the address itself rather than describe it",
            strings.contains("%1$s"));
    }
}
