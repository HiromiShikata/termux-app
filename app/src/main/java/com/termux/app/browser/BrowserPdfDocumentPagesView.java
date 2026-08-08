package com.termux.app.browser;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;

import androidx.annotation.NonNull;

public final class BrowserPdfDocumentPagesView {

    private BrowserPdfDocumentPagesView() {
    }

    @NonNull
    public static View create(@NonNull Context context, @NonNull BrowserPdfDocumentPages pages,
                              int widthPixels) {
        ListView pageList = new ListView(context);
        pageList.setDivider(null);
        pageList.setDividerHeight(0);
        pageList.setAdapter(new PagesAdapter(context, pages, widthPixels));
        return pageList;
    }

    private static final class PagesAdapter extends BaseAdapter {

        private final Context mContext;

        private final BrowserPdfDocumentPages mPages;

        private final int mWidthPixels;

        private PagesAdapter(Context context, BrowserPdfDocumentPages pages, int widthPixels) {
            this.mContext = context;
            this.mPages = pages;
            this.mWidthPixels = widthPixels;
        }

        @Override
        public int getCount() {
            return mPages.count();
        }

        @Override
        public Integer getItem(int position) {
            return position;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View reusablePageView, ViewGroup parent) {
            ImageView pageImage = reusablePageView instanceof ImageView
                ? (ImageView) reusablePageView : newPageImage();
            pageImage.setImageBitmap(mPages.render(position, mWidthPixels));
            return pageImage;
        }

        private ImageView newPageImage() {
            ImageView pageImage = new ImageView(mContext);
            pageImage.setAdjustViewBounds(true);
            pageImage.setLayoutParams(new AbsListView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            return pageImage;
        }
    }
}
