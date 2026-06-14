package com.termux.app.browser;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

public final class BrowserGithubTaskUrls {

    public static final int OPEN_FIRST_N_LIMIT = 10;

    public static final String TASK_URL_JS_PATTERN =
        "github\\.com\\/[^\\/]+\\/[^\\/]+\\/(issues|pull)\\/\\d+";

    public static final String COLLECT_SCRIPT =
        "(function(){"
            + "var links=document.getElementsByTagName('a');"
            + "var uniqueUrls=new Set();"
            + "Array.prototype.forEach.call(links,function(link){"
            + "if(link.href&&link.href.match(/" + TASK_URL_JS_PATTERN + "/)){"
            + "uniqueUrls.add(link.href);}});"
            + "return Array.from(uniqueUrls);"
            + "})();";

    @NonNull
    public static List<String> parseCollectedUrls(@Nullable String evaluateJavascriptResult)
        throws JSONException {
        List<String> urls = new ArrayList<>();
        if (evaluateJavascriptResult == null) return urls;
        String trimmed = evaluateJavascriptResult.trim();
        if (trimmed.isEmpty() || "null".equals(trimmed)) return urls;
        JSONArray array = new JSONArray(trimmed);
        for (int i = 0; i < array.length(); i++) {
            String url = array.isNull(i) ? null : array.getString(i);
            if (url != null && !url.isEmpty()) urls.add(url);
        }
        return urls;
    }

    @NonNull
    public static List<String> selectForBulkOpen(@NonNull List<String> displayedUrls, int limit) {
        if (limit <= 0 || limit >= displayedUrls.size()) {
            return new ArrayList<>(displayedUrls);
        }
        return new ArrayList<>(displayedUrls.subList(0, limit));
    }

    private BrowserGithubTaskUrls() {
    }
}
