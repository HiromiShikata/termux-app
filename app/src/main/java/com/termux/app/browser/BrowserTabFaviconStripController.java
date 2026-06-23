package com.termux.app.browser;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.termux.R;

import java.util.List;

public final class BrowserTabFaviconStripController {

    private final HorizontalScrollView mScrollView;
    private final LinearLayout mContainer;
    private final BrowserTabSelectionListener mListener;

    public BrowserTabFaviconStripController(
            @NonNull HorizontalScrollView scrollView,
            @NonNull LinearLayout container,
            @NonNull BrowserTabSelectionListener listener) {
        this.mScrollView = scrollView;
        this.mContainer = container;
        this.mListener = listener;
    }

    public void update(@NonNull List<BrowserTab> tabs, @Nullable BrowserTab activeTab) {
        mContainer.removeAllViews();
        if (tabs.size() <= 1) {
            mScrollView.setVisibility(View.GONE);
            return;
        }
        mScrollView.setVisibility(View.VISIBLE);
        LayoutInflater inflater = LayoutInflater.from(mContainer.getContext());
        for (BrowserTab tab : tabs) {
            View item = inflater.inflate(R.layout.item_browser_tab_favicon_strip, mContainer, false);
            ImageView faviconView = item.findViewById(R.id.browser_tab_strip_favicon);
            View indicator = item.findViewById(R.id.browser_tab_strip_active_indicator);
            Bitmap favicon = tab.getFavicon();
            if (favicon != null) {
                faviconView.setImageBitmap(favicon);
            } else {
                faviconView.setImageBitmap(makeLetterBitmap(tab));
            }
            indicator.setVisibility(tab == activeTab ? View.VISIBLE : View.INVISIBLE);
            item.setOnClickListener(v -> mListener.openTab(tab));
            mContainer.addView(item);
        }
    }

    private Bitmap makeLetterBitmap(@NonNull BrowserTab tab) {
        int size = 64;
        Bitmap bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
        Paint bg = new Paint(Paint.ANTI_ALIAS_FLAG);
        bg.setColor(0xFF455A64);
        canvas.drawRect(0, 0, size, size, bg);
        Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
        text.setColor(Color.WHITE);
        text.setTextSize(size * 0.5f);
        text.setTextAlign(Paint.Align.CENTER);
        String title = tab.getTitle();
        char letter = (title != null && !title.isEmpty()) ? Character.toUpperCase(title.charAt(0)) : '?';
        float yPos = (size / 2f) - ((text.descent() + text.ascent()) / 2f);
        canvas.drawText(String.valueOf(letter), size / 2f, yPos, text);
        return bmp;
    }
}
