package com.termux.app.browser;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.TouchDelegate;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.termux.R;

import java.util.List;

public final class BrowserTabFaviconStripController {

    static final int CLOSE_BUTTON_TOUCH_PADDING_DP = 4;

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
            View loadingIndicator = item.findViewById(R.id.browser_tab_strip_loading_indicator);
            View closeButton = item.findViewById(R.id.browser_tab_strip_close_button);
            Bitmap favicon = tab.getFavicon();
            if (favicon != null) {
                faviconView.setImageBitmap(favicon);
            } else {
                faviconView.setImageBitmap(BrowserTabFaviconPlaceholder.letterBitmapForTab(tab));
            }
            indicator.setVisibility(tab == activeTab ? View.VISIBLE : View.INVISIBLE);
            loadingIndicator.setVisibility(tab.isLoading() ? View.VISIBLE : View.GONE);
            item.setOnClickListener(v -> mListener.openTab(tab));
            closeButton.setOnClickListener(v -> mListener.closeTab(tab));
            applyCloseButtonCornerTouchTarget(item, closeButton);
            mContainer.addView(item);
        }
        View addItem = inflater.inflate(R.layout.item_browser_tab_favicon_strip_add, mContainer, false);
        View addButton = addItem.findViewById(R.id.browser_tab_strip_add_button);
        addButton.setOnClickListener(v -> mListener.promptNewTab());
        mContainer.addView(addItem);
    }

    private static void applyCloseButtonCornerTouchTarget(
            @NonNull View item, @NonNull View closeButton) {
        int paddingPx = Math.round(TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            CLOSE_BUTTON_TOUCH_PADDING_DP,
            item.getResources().getDisplayMetrics()));
        Runnable applyDelegate = () -> {
            Rect buttonBounds = new Rect();
            closeButton.getHitRect(buttonBounds);
            Rect cornerTarget = computeCloseButtonCornerTarget(
                buttonBounds, item.getWidth(), item.getHeight(), paddingPx);
            item.setTouchDelegate(new TouchDelegate(cornerTarget, closeButton));
        };
        item.addOnLayoutChangeListener(
            (v, l, t, r, b, ol, ot, or, ob) -> applyDelegate.run());
        if (item.getWidth() > 0) {
            applyDelegate.run();
        }
    }

    @NonNull
    static Rect computeCloseButtonCornerTarget(
            @NonNull Rect closeButtonBounds, int itemWidth, int itemHeight, int paddingPx) {
        int right = closeButtonBounds.right;
        int top = closeButtonBounds.top;
        int horizontalLimit = itemWidth > 0 ? itemWidth / 2 : closeButtonBounds.left;
        int verticalLimit = itemHeight > 0 ? itemHeight / 2 : closeButtonBounds.bottom;
        int left = Math.max(horizontalLimit, closeButtonBounds.left - paddingPx);
        int bottom = Math.min(verticalLimit, closeButtonBounds.bottom + paddingPx);
        return new Rect(left, top, right, bottom);
    }
}
