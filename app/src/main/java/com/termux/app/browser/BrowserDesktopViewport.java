package com.termux.app.browser;

import androidx.annotation.Nullable;

public final class BrowserDesktopViewport {

    public static final int LAYOUT_WIDTH_CSS_PX = 1280;

    public static final String INJECTION_SCRIPT =
        "(function(){"
            + "var desktopContent='width=" + LAYOUT_WIDTH_CSS_PX + "';"
            + "function forceDesktopViewport(){"
            + "var metas=document.querySelectorAll('meta[name=\"viewport\"]');"
            + "if(metas.length===0){"
            + "var meta=document.createElement('meta');"
            + "meta.setAttribute('name','viewport');"
            + "meta.setAttribute('content',desktopContent);"
            + "(document.head||document.documentElement).appendChild(meta);"
            + "return;}"
            + "for(var i=0;i<metas.length;i++){"
            + "if(metas[i].getAttribute('content')!==desktopContent){"
            + "metas[i].setAttribute('content',desktopContent);}}}"
            + "forceDesktopViewport();"
            + "if(!window.__termuxDesktopViewportObserver){"
            + "window.__termuxDesktopViewportObserver=new MutationObserver(forceDesktopViewport);"
            + "window.__termuxDesktopViewportObserver.observe(document.documentElement,"
            + "{subtree:true,childList:true,attributes:true,attributeFilter:['content','name']});}"
            + "})();";

    public static boolean appliesTo(@Nullable BrowserTab tab) {
        return tab != null && tab.isDesktopMode();
    }

    private BrowserDesktopViewport() {
    }
}
