package com.termux.app.browser;

public final class BrowserDesktopViewport {

    public static final int LAYOUT_WIDTH_CSS_PX = 1024;

    public static final String INJECTION_SCRIPT =
        "(function(){"
            + "var meta=document.querySelector('meta[name=\"viewport\"]');"
            + "if(meta==null){meta=document.createElement('meta');meta.setAttribute('name','viewport');"
            + "(document.head||document.documentElement).appendChild(meta);}"
            + "meta.setAttribute('content','width=" + LAYOUT_WIDTH_CSS_PX + "');"
            + "})();";

    private BrowserDesktopViewport() {
    }
}
