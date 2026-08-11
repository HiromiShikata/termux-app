package com.termux.app.browser;

import androidx.annotation.NonNull;

import com.termux.shared.termux.settings.preferences.TermuxAppSharedPreferences;

public final class BrowserMeetLowPowerVideoSettings {

    public static final boolean DEFAULT_ENABLED = false;
    public static final int DEFAULT_MAX_WIDTH = 640;
    public static final int DEFAULT_MAX_HEIGHT = 360;
    public static final int DEFAULT_MAX_FRAMERATE = 15;

    public static final String APPLIED_PROPERTY_NAME = "meetLowPowerVideoApplied";

    private final boolean enabled;
    private final int maxWidth;
    private final int maxHeight;
    private final int maxFramerate;

    public BrowserMeetLowPowerVideoSettings(boolean enabled, int maxWidth, int maxHeight, int maxFramerate) {
        this.enabled = enabled;
        this.maxWidth = maxWidth;
        this.maxHeight = maxHeight;
        this.maxFramerate = maxFramerate;
    }

    public static BrowserMeetLowPowerVideoSettings defaults() {
        return new BrowserMeetLowPowerVideoSettings(
            DEFAULT_ENABLED, DEFAULT_MAX_WIDTH, DEFAULT_MAX_HEIGHT, DEFAULT_MAX_FRAMERATE);
    }

    public static BrowserMeetLowPowerVideoSettings fromPreferences(@NonNull TermuxAppSharedPreferences preferences) {
        return new BrowserMeetLowPowerVideoSettings(
            preferences.isBrowserMeetLowPowerVideoEnabled(),
            preferences.getBrowserMeetLowPowerVideoMaxWidth(),
            preferences.getBrowserMeetLowPowerVideoMaxHeight(),
            preferences.getBrowserMeetLowPowerVideoMaxFramerate());
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getMaxWidth() {
        return maxWidth;
    }

    public int getMaxHeight() {
        return maxHeight;
    }

    public int getMaxFramerate() {
        return maxFramerate;
    }

    public String toDocumentStartScript() {
        return "(function(){try{"
            + "var appliedName='" + APPLIED_PROPERTY_NAME + "';"
            + "if(Object.getOwnPropertyDescriptor(window,appliedName)){return;}"
            + "Object.defineProperty(window,appliedName,"
            + "{value:true,enumerable:false,configurable:true,writable:true});"
            + "var ENABLED=" + (enabled ? "true" : "false") + ";"
            + "var MAX_WIDTH=" + maxWidth + ";"
            + "var MAX_HEIGHT=" + maxHeight + ";"
            + "var MAX_FRAMERATE=" + maxFramerate + ";"
            + "if(!ENABLED){return;}"
            + "if(location.origin!=='https://meet.google.com'){return;}"
            + "function clampConstraint(value,cap){"
            + "if(typeof value==='number'){return Math.min(value,cap);}"
            + "if(value&&typeof value==='object'){"
            + "var next={};for(var key in value){next[key]=value[key];}"
            + "if(typeof next.ideal==='number'){next.ideal=Math.min(next.ideal,cap);}"
            + "if(typeof next.max==='number'){next.max=Math.min(next.max,cap);}else{next.max=cap;}"
            + "if(typeof next.min==='number'){next.min=Math.min(next.min,cap);}"
            + "if(typeof next.exact==='number'){next.exact=Math.min(next.exact,cap);}"
            + "return next;}"
            + "return {ideal:cap,max:cap};}"
            + "function clampVideoConstraints(video){"
            + "if(!video||typeof video!=='object'){return {width:{ideal:MAX_WIDTH,max:MAX_WIDTH},"
            + "height:{ideal:MAX_HEIGHT,max:MAX_HEIGHT},frameRate:{ideal:MAX_FRAMERATE,max:MAX_FRAMERATE}};}"
            + "var next={};for(var key in video){next[key]=video[key];}"
            + "next.width=clampConstraint(video.width,MAX_WIDTH);"
            + "next.height=clampConstraint(video.height,MAX_HEIGHT);"
            + "next.frameRate=clampConstraint(video.frameRate,MAX_FRAMERATE);"
            + "return next;}"
            + "if(navigator.mediaDevices&&navigator.mediaDevices.getUserMedia){"
            + "var originalGetUserMedia=navigator.mediaDevices.getUserMedia.bind(navigator.mediaDevices);"
            + "navigator.mediaDevices.getUserMedia=function(constraints){try{"
            + "if(constraints&&constraints.video){var next={};for(var key in constraints){next[key]=constraints[key];}"
            + "next.video=clampVideoConstraints(constraints.video);constraints=next;}"
            + "}catch(e){}return originalGetUserMedia(constraints);};}"
            + "function computeScaleDownBy(track){try{"
            + "var settings=track&&track.getSettings?track.getSettings():null;"
            + "if(!settings){return 1;}"
            + "var scale=1;"
            + "if(typeof settings.width==='number'&&settings.width>MAX_WIDTH){scale=settings.width/MAX_WIDTH;}"
            + "if(typeof settings.height==='number'&&settings.height/scale>MAX_HEIGHT){scale=settings.height/MAX_HEIGHT;}"
            + "return scale<1?1:scale;}catch(e){return 1;}}"
            + "function applySenderCaps(sender){try{"
            + "if(!sender||!sender.track||sender.track.kind!=='video'){return;}"
            + "var parameters=sender.getParameters?sender.getParameters():null;"
            + "if(!parameters){return;}"
            + "if(!parameters.encodings||parameters.encodings.length===0){parameters.encodings=[{}];}"
            + "var scale=computeScaleDownBy(sender.track);"
            + "for(var i=0;i<parameters.encodings.length;i++){"
            + "parameters.encodings[i].scaleResolutionDownBy=scale;"
            + "parameters.encodings[i].maxFramerate=MAX_FRAMERATE;}"
            + "sender.setParameters(parameters).catch(function(){});"
            + "}catch(e){}}"
            + "function applyAllSenders(pc){try{"
            + "var senders=pc.getSenders?pc.getSenders():[];"
            + "for(var i=0;i<senders.length;i++){applySenderCaps(senders[i]);}"
            + "}catch(e){}}"
            + "var NativeRTCPeerConnection=window.RTCPeerConnection||window.webkitRTCPeerConnection;"
            + "if(NativeRTCPeerConnection){"
            + "function PatchedRTCPeerConnection(){"
            + "var pc=new NativeRTCPeerConnection(arguments[0],arguments[1]);"
            + "try{"
            + "var originalAddTrack=pc.addTrack.bind(pc);"
            + "pc.addTrack=function(){var sender=originalAddTrack.apply(pc,arguments);"
            + "setTimeout(function(){applySenderCaps(sender);},0);return sender;};"
            + "if(pc.addTransceiver){var originalAddTransceiver=pc.addTransceiver.bind(pc);"
            + "pc.addTransceiver=function(){var transceiver=originalAddTransceiver.apply(pc,arguments);"
            + "setTimeout(function(){if(transceiver&&transceiver.sender){applySenderCaps(transceiver.sender);}},0);"
            + "return transceiver;};}"
            + "pc.addEventListener('negotiationneeded',function(){setTimeout(function(){applyAllSenders(pc);},0);});"
            + "}catch(e){}"
            + "return pc;}"
            + "PatchedRTCPeerConnection.prototype=NativeRTCPeerConnection.prototype;"
            + "window.RTCPeerConnection=PatchedRTCPeerConnection;"
            + "if(window.webkitRTCPeerConnection){window.webkitRTCPeerConnection=PatchedRTCPeerConnection;}}"
            + "}catch(e){}})();";
    }
}
