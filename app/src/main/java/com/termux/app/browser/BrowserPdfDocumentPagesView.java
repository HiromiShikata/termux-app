package com.termux.app.browser;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import androidx.annotation.NonNull;

public final class BrowserPdfDocumentPagesView {

    private BrowserPdfDocumentPagesView() {
    }

    @NonNull
    public static View create(@NonNull Context context, @NonNull BrowserPdfDocumentPages pages,
                              int widthPixels) {
        LinearLayout pageColumn = new LinearLayout(context);
        pageColumn.setOrientation(LinearLayout.VERTICAL);
        for (int pageIndex = 0; pageIndex < pages.count(); pageIndex++) {
            ImageView pageImage = new ImageView(context);
            pageImage.setAdjustViewBounds(true);
            pageImage.setImageBitmap(pages.render(pageIndex, widthPixels));
            pageColumn.addView(pageImage, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }
        ScrollView scrollingDocument = new ScrollView(context);
        scrollingDocument.addView(pageColumn, new ScrollView.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return scrollingDocument;
    }
}
