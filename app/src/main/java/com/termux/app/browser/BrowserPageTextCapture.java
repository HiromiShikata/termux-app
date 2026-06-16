package com.termux.app.browser;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;

public final class BrowserPageTextCapture {

    public static final String CAPTURE_SCRIPT =
        "(function(){"
            + "var text='';"
            + "if(document.body&&document.body.innerText){text=document.body.innerText;}"
            + "else if(document.documentElement&&document.documentElement.innerText){text=document.documentElement.innerText;}"
            + "else if(document.body&&document.body.textContent){text=document.body.textContent;}"
            + "else if(document.documentElement&&document.documentElement.textContent){text=document.documentElement.textContent;}"
            + "return text||'';"
            + "})();";

    @NonNull
    public static String parseCapturedText(@Nullable String evaluateJavascriptResult)
        throws JSONException {
        if (evaluateJavascriptResult == null) return "";
        String trimmed = evaluateJavascriptResult.trim();
        if (trimmed.isEmpty() || "null".equals(trimmed)) return "";
        JSONArray array = new JSONArray("[" + trimmed + "]");
        if (array.isNull(0)) return "";
        return array.getString(0);
    }

    private BrowserPageTextCapture() {
    }
}
