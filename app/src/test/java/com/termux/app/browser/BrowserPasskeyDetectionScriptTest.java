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
        Assert.assertEquals("onLoginFormDetected", BrowserPasskeyDetectionScript.LOGIN_FORM_BRIDGE_METHOD);
    }

    @Test
    public void detectsPasswordFieldsAndSignalsTheLoginFormBridgeWithoutAPageControlledUrl() {
        String script = BrowserPasskeyDetectionScript.documentStartScript();

        Assert.assertTrue(script.contains("input[type=\"password\"]"));
        Assert.assertTrue(script.contains("window." + BrowserPasskeyDetectionScript.BRIDGE_NAME
            + "." + BrowserPasskeyDetectionScript.LOGIN_FORM_BRIDGE_METHOD + "('');"));
    }

    @Test
    public void notifiesTheLoginFormBridgeAtMostOncePerPageViaAGuardFlag() {
        String script = BrowserPasskeyDetectionScript.documentStartScript();

        Assert.assertTrue(script.contains("var loginFormNotified=false;"));
        Assert.assertTrue(script.contains("if(loginFormNotified){return;}"));
        Assert.assertTrue(script.contains("loginFormNotified=true;notifyLoginForm();"));
    }

    @Test
    public void checksForLoginFormsBothAtDomContentLoadedAndOnPasswordFieldFocus() {
        String script = BrowserPasskeyDetectionScript.documentStartScript();

        Assert.assertTrue(script.contains("document.addEventListener('DOMContentLoaded',checkLoginForm,{once:true});"));
        Assert.assertTrue(script.contains("document.addEventListener('focusin',function(event){"));
        Assert.assertFalse(script.contains("location.href"));
    }
}
