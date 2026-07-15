package com.termux.app.browser;

import org.junit.Assert;
import org.junit.Test;

public class BrowserPasskeyDetectionScriptTest {

    @Test
    public void wrapsBothCredentialsGetAndCreate() {
        String script = BrowserPasskeyDetectionScript.documentStartScript();

        Assert.assertTrue(script.contains("navigator.credentials.get=function"));
        Assert.assertTrue(script.contains("navigator.credentials.create=function"));
    }

    @Test
    public void signalsHostOnlyForPublicKeyRequests() {
        String script = BrowserPasskeyDetectionScript.documentStartScript();

        Assert.assertTrue(script.contains("options.publicKey"));
        Assert.assertTrue(script.contains("if(isPublicKeyRequest(options)){notifyHost();}"));
    }

    @Test
    public void callsThroughToTheOriginalFunctions() {
        String script = BrowserPasskeyDetectionScript.documentStartScript();

        Assert.assertTrue(script.contains("var originalGet=navigator.credentials.get.bind(navigator.credentials);"));
        Assert.assertTrue(script.contains("return originalGet(options);"));
        Assert.assertTrue(
            script.contains("var originalCreate=navigator.credentials.create.bind(navigator.credentials);"));
        Assert.assertTrue(script.contains("return originalCreate(options);"));
    }

    @Test
    public void signalsTheHostBridgeWithoutPassingThePageControlledUrl() {
        String script = BrowserPasskeyDetectionScript.documentStartScript();

        Assert.assertTrue(script.contains("window." + BrowserPasskeyDetectionScript.BRIDGE_NAME
            + "." + BrowserPasskeyDetectionScript.BRIDGE_METHOD + "('');"));
        Assert.assertFalse(script.contains("location.href"));
    }

    @Test
    public void isGuardedByTryCatchAndAReentryFlagSoItNeverThrowsIntoThePage() {
        String script = BrowserPasskeyDetectionScript.documentStartScript();

        Assert.assertTrue(script.startsWith("(function(){try{"));
        Assert.assertTrue(script.endsWith("catch(e){}})();"));
        Assert.assertTrue(script.contains("if(window.__termuxPasskeyShimApplied){return;}"));
        Assert.assertTrue(script.contains("if(!navigator.credentials){return;}"));
    }

    @Test
    public void bridgeIdentifiersAreStable() {
        Assert.assertEquals("__termuxPasskeyBridge", BrowserPasskeyDetectionScript.BRIDGE_NAME);
        Assert.assertEquals("onPasskeyCeremonyDetected", BrowserPasskeyDetectionScript.BRIDGE_METHOD);
    }
}
