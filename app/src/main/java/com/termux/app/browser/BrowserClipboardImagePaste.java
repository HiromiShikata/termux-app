package com.termux.app.browser;

import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public final class BrowserClipboardImagePaste {

    private static final Executor sBackgroundExecutor = Executors.newSingleThreadExecutor();

    private BrowserClipboardImagePaste() {
    }

    public static boolean pasteIfImageInClipboard(@NonNull Context context, @NonNull WebView webView) {
        ClipboardManager cm = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm == null) return false;
        ClipData clipData = cm.getPrimaryClip();
        String mimeType = extractImageMimeType(clipData);
        if (mimeType == null) return false;
        if (clipData.getItemCount() == 0) return false;
        ClipData.Item item = clipData.getItemAt(0);
        if (item == null) return false;
        Uri uri = item.getUri();
        if (uri == null) return false;

        ContentResolver cr = context.getContentResolver();
        Handler mainHandler = new Handler(Looper.getMainLooper());
        sBackgroundExecutor.execute(() -> {
            byte[] bytes = readBytes(cr, uri);
            if (bytes == null) return;
            String base64 = Base64.encodeToString(bytes, Base64.NO_WRAP);
            String dataUrl = "data:" + mimeType + ";base64," + base64;
            String script = buildPasteScript(dataUrl);
            mainHandler.post(() -> webView.evaluateJavascript(script, null));
        });
        return true;
    }

    @Nullable
    static String extractImageMimeType(@Nullable ClipData clipData) {
        if (clipData == null) return null;
        ClipDescription desc = clipData.getDescription();
        if (desc == null) return null;
        for (int i = 0; i < desc.getMimeTypeCount(); i++) {
            String mimeType = desc.getMimeType(i);
            if (mimeType != null && mimeType.startsWith("image/")) {
                return mimeType;
            }
        }
        return null;
    }

    @NonNull
    static String buildPasteScript(@NonNull String dataUrl) {
        return "(function(){"
            + "try{"
            + "var u='" + dataUrl + "';"
            + "var m=u.substring(5,u.indexOf(';'));"
            + "var e=m.indexOf('/')>-1?m.split('/')[1]:'png';"
            + "var b=atob(u.split(',')[1]);"
            + "var a=new Uint8Array(b.length);"
            + "for(var i=0;i<b.length;i++){a[i]=b.charCodeAt(i);}"
            + "var f=new File([a],'image.'+e,{type:m});"
            + "var dt=new DataTransfer();"
            + "dt.items.add(f);"
            + "var ev=new ClipboardEvent('paste',{bubbles:true,cancelable:true,clipboardData:dt});"
            + "(document.activeElement||document.body).dispatchEvent(ev);"
            + "}catch(ex){console.error('[TermuxBrowser] clipboard image paste: '+ex);}"
            + "})();";
    }

    @Nullable
    private static byte[] readBytes(@NonNull ContentResolver cr, @NonNull Uri uri) {
        try (InputStream is = cr.openInputStream(uri)) {
            if (is == null) return null;
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = is.read(buf)) != -1) {
                baos.write(buf, 0, n);
            }
            return baos.toByteArray();
        } catch (IOException e) {
            return null;
        }
    }
}
