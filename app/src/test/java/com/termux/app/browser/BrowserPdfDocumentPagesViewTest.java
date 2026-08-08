package com.termux.app.browser;

import android.app.Activity;
import android.graphics.Bitmap;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class BrowserPdfDocumentPagesViewTest {

    private static final int PAGE_WIDTH_PIXELS = 720;

    private static final class ThreePageDocument implements BrowserPdfDocumentPages {

        private final List<Integer> renderedPageIndexes = new ArrayList<>();

        private boolean closed;

        @Override
        public int count() {
            return 3;
        }

        @NonNull
        @Override
        public Bitmap render(int pageIndex, int widthPixels) {
            renderedPageIndexes.add(pageIndex);
            return Bitmap.createBitmap(widthPixels, widthPixels, Bitmap.Config.ARGB_8888);
        }

        @Override
        public void close() {
            closed = true;
        }
    }

    private Activity activity;

    @Before
    public void setUp() {
        activity = Robolectric.buildActivity(Activity.class).create().get();
    }

    private List<ImageView> pageImagesIn(View view) {
        List<ImageView> pageImages = new ArrayList<>();
        if (view instanceof ImageView) {
            pageImages.add((ImageView) view);
            return pageImages;
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int childIndex = 0; childIndex < group.getChildCount(); childIndex++) {
                pageImages.addAll(pageImagesIn(group.getChildAt(childIndex)));
            }
        }
        return pageImages;
    }

    @Test
    public void everyPageOfTheDocumentIsShown() {
        ThreePageDocument document = new ThreePageDocument();

        View view = BrowserPdfDocumentPagesView.create(activity, document, PAGE_WIDTH_PIXELS);

        Assert.assertEquals("a document the user opened is only readable when all of its pages are"
                + " on screen, so every page must be rendered into the view",
            3, pageImagesIn(view).size());
    }

    @Test
    public void everyPageIsRenderedAtTheWidthItIsShownAt() {
        ThreePageDocument document = new ThreePageDocument();

        BrowserPdfDocumentPagesView.create(activity, document, PAGE_WIDTH_PIXELS);

        Assert.assertEquals("pages rendered at another width are either unreadable or waste memory",
            java.util.Arrays.asList(0, 1, 2), document.renderedPageIndexes);
    }
}
