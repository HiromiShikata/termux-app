package com.termux.app.browser;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import androidx.annotation.NonNull;

public final class BrowserTabFaviconPlaceholder {

    private static final int SIZE = 64;

    private static final int BACKGROUND_COLOR = 0xFF455A64;

    private BrowserTabFaviconPlaceholder() {
    }

    @NonNull
    public static Bitmap letterBitmapForTab(@NonNull BrowserTab tab) {
        Bitmap bitmap = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        backgroundPaint.setColor(BACKGROUND_COLOR);
        canvas.drawRect(0, 0, SIZE, SIZE, backgroundPaint);
        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(SIZE * 0.5f);
        textPaint.setTextAlign(Paint.Align.CENTER);
        String title = tab.getTitle();
        char letter = (title != null && !title.isEmpty()) ? Character.toUpperCase(title.charAt(0)) : '?';
        float yPosition = (SIZE / 2f) - ((textPaint.descent() + textPaint.ascent()) / 2f);
        canvas.drawText(String.valueOf(letter), SIZE / 2f, yPosition, textPaint);
        return bitmap;
    }
}
