package com.termux.app.browser;

public final class BrowserPasskeyDetectionScript {

    public static final String BRIDGE_NAME = "__termuxPasskeyBridge";

    public static final String BRIDGE_METHOD = "onPasskeyCeremonyDetected";

    private BrowserPasskeyDetectionScript() {
    }

    public static String documentStartScript() {
        return "(function(){try{"
            + "if(window.__termuxPasskeyShimApplied){return;}"
            + "window.__termuxPasskeyShimApplied=true;"
            + "function notifyHost(){try{"
            + "if(window." + BRIDGE_NAME + "&&typeof window." + BRIDGE_NAME + "." + BRIDGE_METHOD + "==='function'){"
            + "window." + BRIDGE_NAME + "." + BRIDGE_METHOD + "(String(location.href));}"
            + "}catch(e){}}"
            + "if(!navigator.credentials){return;}"
            + "function isPublicKeyRequest(options){try{"
            + "return !!(options&&typeof options==='object'&&options.publicKey);"
            + "}catch(e){return false;}}"
            + "if(typeof navigator.credentials.get==='function'){"
            + "var originalGet=navigator.credentials.get.bind(navigator.credentials);"
            + "navigator.credentials.get=function(options){try{"
            + "if(isPublicKeyRequest(options)){notifyHost();}"
            + "}catch(e){}return originalGet(options);};}"
            + "if(typeof navigator.credentials.create==='function'){"
            + "var originalCreate=navigator.credentials.create.bind(navigator.credentials);"
            + "navigator.credentials.create=function(options){try{"
            + "if(isPublicKeyRequest(options)){notifyHost();}"
            + "}catch(e){}return originalCreate(options);};}"
            + "}catch(e){}})();";
    }
}
