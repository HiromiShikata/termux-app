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

    static final int CLOSE_BUTTON_TOUCH_TARGET_DP = 48;

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
            expandCloseButtonCornerTouchTarget(item, closeButton, faviconView);
            mContainer.addView(item);
        }
        View addItem = inflater.inflate(R.layout.item_browser_tab_favicon_strip_add, mContainer, false);
        View addButton = addItem.findViewById(R.id.browser_tab_strip_add_button);
        addButton.setOnClickListener(v -> mListener.promptNewTab());
        mContainer.addView(addItem);
    }

    private static void expandCloseButtonCornerTouchTarget(
            @NonNull View item, @NonNull View closeButton, @NonNull View favicon) {
        int targetSizePx = Math.round(TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            CLOSE_BUTTON_TOUCH_TARGET_DP,
            item.getResources().getDisplayMetrics()));
        Runnable applyDelegate = () -> {
            Rect buttonBounds = new Rect();
            closeButton.getHitRect(buttonBounds);
            Rect faviconBounds = new Rect();
            favicon.getHitRect(faviconBounds);
            Rect cornerTarget = computeCloseButtonCornerTarget(buttonBounds, faviconBounds, targetSizePx);
            item.setTouchDelegate(new TouchDelegate(cornerTarget, closeButton));
        };
        item.addOnLayoutChangeListener(
            (v, l, t, r, b, ol, ot, or, ob) -> applyDelegate.run());
        if (closeButton.getWidth() > 0) {
            applyDelegate.run();
        }
    }

    @NonNull
    static Rect computeCloseButtonCornerTarget(
            @NonNull Rect closeButtonBounds, @NonNull Rect faviconBounds, int targetSizePx) {
        int right = closeButtonBounds.right;
        int top = closeButtonBounds.top;
        int bottom = Math.max(closeButtonBounds.bottom, top + targetSizePx);
        int left = Math.min(right - targetSizePx, faviconBounds.right);
        return new Rect(left, top, right, bottom);
    }
}
