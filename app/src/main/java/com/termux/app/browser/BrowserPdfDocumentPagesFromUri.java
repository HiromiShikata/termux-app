package com.termux.app.browser;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

import androidx.annotation.NonNull;

import com.termux.shared.logger.Logger;

import java.io.IOException;

public final class BrowserPdfDocumentPagesFromUri implements BrowserPdfDocumentPages {

    private static final String LOG_TAG = "BrowserPdfDocumentPagesFromUri";

    private final ParcelFileDescriptor mDocumentFileDescriptor;

    private final PdfRenderer mDocumentRenderer;

    public BrowserPdfDocumentPagesFromUri(@NonNull ContentResolver contentResolver, @NonNull Uri document)
        throws IOException {
        ParcelFileDescriptor documentFileDescriptor = contentResolver.openFileDescriptor(document, "r");
        if (documentFileDescriptor == null) {
            throw new IOException("The downloaded document could not be opened for reading");
        }
        this.mDocumentFileDescriptor = documentFileDescriptor;
        this.mDocumentRenderer = new PdfRenderer(documentFileDescriptor);
    }

    @Override
    public int count() {
        return mDocumentRenderer.getPageCount();
    }

    @NonNull
    @Override
    public Bitmap render(int pageIndex, int widthPixels) {
        PdfRenderer.Page page = mDocumentRenderer.openPage(pageIndex);
        try {
            int pageWidth = Math.max(1, page.getWidth());
            int heightPixels = Math.max(1, (int) ((long) widthPixels * page.getHeight() / pageWidth));
            Bitmap pageBitmap = Bitmap.createBitmap(widthPixels, heightPixels, Bitmap.Config.ARGB_8888);
            pageBitmap.eraseColor(Color.WHITE);
            page.render(pageBitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
            return pageBitmap;
        } finally {
            page.close();
        }
    }

    @Override
    public void close() {
        mDocumentRenderer.close();
        try {
            mDocumentFileDescriptor.close();
        } catch (IOException e) {
            Logger.logStackTraceWithMessage(LOG_TAG, "Failed to close the displayed document", e);
        }
    }
}
