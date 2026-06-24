package com.termux.app.browser;

import android.graphics.Bitmap;
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
        mScrollView.setVisibility(View.VISIBLE);
        LayoutInflater inflater = LayoutInflater.from(mContainer.getContext());
        for (BrowserTab tab : tabs) {
            View item = inflater.inflate(R.layout.item_browser_tab_favicon_strip, mContainer, false);
            ImageView faviconView = item.findViewById(R.id.browser_tab_strip_favicon);
            View indicator = item.findViewById(R.id.browser_tab_strip_active_indicator);
            View closeButton = item.findViewById(R.id.browser_tab_strip_close_button);
            Bitmap favicon = tab.getFavicon();
            if (favicon != null) {
                faviconView.setImageBitmap(favicon);
            } else {
                faviconView.setImageBitmap(BrowserTabFaviconPlaceholder.letterBitmapForTab(tab));
            }
            indicator.setVisibility(tab == activeTab ? View.VISIBLE : View.INVISIBLE);
            item.setOnClickListener(v -> mListener.openTab(tab));
            closeButton.setOnClickListener(v -> mListener.closeTab(tab));
            mContainer.addView(item);
        }
        View addItem = inflater.inflate(R.layout.item_browser_tab_favicon_strip_add, mContainer, false);
        View addButton = addItem.findViewById(R.id.browser_tab_strip_add_button);
        addButton.setOnClickListener(v -> mListener.promptNewTab());
        mContainer.addView(addItem);
    }
}
