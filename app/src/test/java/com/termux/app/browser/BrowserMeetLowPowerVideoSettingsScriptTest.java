package com.termux.app.browser;

import org.junit.Assert;
import org.junit.Test;

public class BrowserMeetLowPowerVideoSettingsScriptTest {

    @Test
    public void disabledScriptInjectsDisabledFlagAndReturnsEarly() {
        BrowserMeetLowPowerVideoSettings settings =
            new BrowserMeetLowPowerVideoSettings(false, 640, 360, 15);

        String script = settings.toDocumentStartScript();

        Assert.assertTrue(script.contains("var ENABLED=false;"));
        Assert.assertTrue(script.contains("if(!ENABLED){return;}"));
    }

    @Test
    public void enabledScriptInjectsConfiguredCapsAsConstants() {
        BrowserMeetLowPowerVideoSettings settings =
            new BrowserMeetLowPowerVideoSettings(true, 480, 270, 10);

        String script = settings.toDocumentStartScript();

        Assert.assertTrue(script.contains("var ENABLED=true;"));
        Assert.assertTrue(script.contains("var MAX_WIDTH=480;"));
        Assert.assertTrue(script.contains("var MAX_HEIGHT=270;"));
        Assert.assertTrue(script.contains("var MAX_FRAMERATE=10;"));
    }

    @Test
    public void scriptScopesItselfToMeetOrigin() {
        BrowserMeetLowPowerVideoSettings settings =
            new BrowserMeetLowPowerVideoSettings(true, 320, 180, 5);

        String script = settings.toDocumentStartScript();

        Assert.assertTrue(script.contains("location.origin!=='https://meet.google.com'"));
    }

    @Test
    public void scriptWrapsGetUserMediaAndRtcPeerConnection() {
        BrowserMeetLowPowerVideoSettings settings =
            new BrowserMeetLowPowerVideoSettings(true, 640, 360, 15);

        String script = settings.toDocumentStartScript();

        Assert.assertTrue(script.contains("navigator.mediaDevices.getUserMedia=function"));
        Assert.assertTrue(script.contains("window.RTCPeerConnection=PatchedRTCPeerConnection;"));
        Assert.assertTrue(script.contains("scaleResolutionDownBy"));
        Assert.assertTrue(script.contains("maxFramerate=MAX_FRAMERATE;"));
    }

    @Test
    public void scriptIsGuardedByTryCatchAndReentryFlag() {
        BrowserMeetLowPowerVideoSettings settings =
            new BrowserMeetLowPowerVideoSettings(true, 640, 360, 24);

        String script = settings.toDocumentStartScript();

        Assert.assertTrue(script.startsWith("(function(){try{"));
        Assert.assertTrue(script.endsWith("catch(e){}})();"));
        Assert.assertTrue(script.contains("if(Object.getOwnPropertyDescriptor(window,appliedName)){return;}"));
    }

    @Test
    public void defaultsProduceDisabledScript() {
        String script = BrowserMeetLowPowerVideoSettings.defaults().toDocumentStartScript();

        Assert.assertTrue(script.contains("var ENABLED=false;"));
        Assert.assertTrue(script.contains("var MAX_WIDTH=640;"));
        Assert.assertTrue(script.contains("var MAX_HEIGHT=360;"));
        Assert.assertTrue(script.contains("var MAX_FRAMERATE=15;"));
    }
}
