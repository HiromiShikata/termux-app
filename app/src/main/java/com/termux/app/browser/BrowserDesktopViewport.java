package com.termux.app.browser;

import androidx.annotation.Nullable;

public final class BrowserDesktopViewport {

    public static final int LAYOUT_WIDTH_CSS_PX = 1280;

    public static final String OBSERVER_PROPERTY_NAME = "desktopViewportObserver";

    public static final String INJECTION_SCRIPT =
        "(function(){"
            + "var observerName='" + OBSERVER_PROPERTY_NAME + "';"
            + "if(Object.getOwnPropertyDescriptor(window,observerName)){return;}"
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
            + "var observer=new MutationObserver(forceDesktopViewport);"
            + "Object.defineProperty(window,observerName,"
            + "{value:observer,enumerable:false,configurable:true,writable:true});"
            + "observer.observe(document.documentElement,"
            + "{subtree:true,childList:true,attributes:true,attributeFilter:['content','name']});"
            + "})();";

    public static boolean appliesTo(@Nullable BrowserTab tab) {
        return tab != null && tab.isDesktopMode();
    }

    private BrowserDesktopViewport() {
    }
}
