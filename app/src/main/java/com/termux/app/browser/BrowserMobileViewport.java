package com.termux.app.browser;

public final class BrowserMobileViewport {

    public static final String LAYOUT_CONTENT =
        "width=device-width, initial-scale=1, minimum-scale=1, shrink-to-fit=no";

    public static final String OBSERVER_PROPERTY_NAME = "mobileViewportObserver";

    public static final String INJECTION_SCRIPT =
        "(function(){"
            + "var observerName='" + OBSERVER_PROPERTY_NAME + "';"
            + "if(Object.getOwnPropertyDescriptor(window,observerName)){return;}"
            + "var mobileContent='" + LAYOUT_CONTENT + "';"
            + "function forceMobileViewport(){"
            + "var metas=document.querySelectorAll('meta[name=\"viewport\"]');"
            + "if(metas.length===0){"
            + "var meta=document.createElement('meta');"
            + "meta.setAttribute('name','viewport');"
            + "meta.setAttribute('content',mobileContent);"
            + "(document.head||document.documentElement).appendChild(meta);"
            + "return;}"
            + "for(var i=0;i<metas.length;i++){"
            + "if(metas[i].getAttribute('content')!==mobileContent){"
            + "metas[i].setAttribute('content',mobileContent);}}}"
            + "forceMobileViewport();"
            + "var observer=new MutationObserver(forceMobileViewport);"
            + "Object.defineProperty(window,observerName,"
            + "{value:observer,enumerable:false,configurable:true,writable:true});"
            + "observer.observe(document.documentElement,"
            + "{subtree:true,childList:true,attributes:true,attributeFilter:['content','name']});"
            + "})();";

    private BrowserMobileViewport() {
    }
}
