package com.termux.app.browser;

import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class BrowserFileChooserResult {

    private BrowserFileChooserResult() {
    }

    @Nullable
    public static Uri[] parse(int resultCode, @Nullable Intent data) {
        if (resultCode != android.app.Activity.RESULT_OK || data == null) {
            return null;
        }
        List<Uri> uris = new ArrayList<>();
        ClipData clipData = data.getClipData();
        if (clipData != null) {
            for (int index = 0; index < clipData.getItemCount(); index++) {
                ClipData.Item item = clipData.getItemAt(index);
                if (item == null) {
                    continue;
                }
                Uri uri = item.getUri();
                if (uri != null) {
                    uris.add(uri);
                }
            }
        }
        Uri singleUri = data.getData();
        if (singleUri != null && !uris.contains(singleUri)) {
            uris.add(singleUri);
        }
        if (uris.isEmpty()) {
            return null;
        }
        return uris.toArray(new Uri[0]);
    }

    @NonNull
    public static Intent buildIntent(boolean allowMultiple, @Nullable String[] acceptTypes) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(resolveMimeType(acceptTypes));
        String[] explicitTypes = resolveExtraMimeTypes(acceptTypes);
        if (explicitTypes.length > 0) {
            intent.putExtra(Intent.EXTRA_MIME_TYPES, explicitTypes);
        }
        if (allowMultiple) {
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        }
        return intent;
    }

    @NonNull
    static String resolveMimeType(@Nullable String[] acceptTypes) {
        String[] explicitTypes = resolveExtraMimeTypes(acceptTypes);
        if (explicitTypes.length == 1) {
            return explicitTypes[0];
        }
        return "*/*";
    }

    @NonNull
    static String[] resolveExtraMimeTypes(@Nullable String[] acceptTypes) {
        if (acceptTypes == null) {
            return new String[0];
        }
        List<String> resolved = new ArrayList<>();
        for (String acceptType : acceptTypes) {
            if (acceptType == null) {
                continue;
            }
            for (String token : acceptType.split(",")) {
                String trimmed = token.trim();
                if (!trimmed.isEmpty() && trimmed.contains("/") && !resolved.contains(trimmed)) {
                    resolved.add(trimmed);
                }
            }
        }
        return resolved.toArray(new String[0]);
    }
}
