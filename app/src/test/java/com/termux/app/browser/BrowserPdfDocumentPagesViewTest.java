package com.termux.app.browser;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListAdapter;

import androidx.annotation.NonNull;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class BrowserPdfDocumentPagesViewTest {

    private static final int PAGE_WIDTH_PIXELS = 720;

    private static final int PAGE_COUNT = 3;

    private static final class ThreePageDocument implements BrowserPdfDocumentPages {

        private final List<Integer> renderedPageIndexes = new ArrayList<>();

        @Override
        public int count() {
            return PAGE_COUNT;
        }

        @NonNull
        @Override
        public Bitmap render(int pageIndex, int widthPixels) {
            renderedPageIndexes.add(pageIndex);
            return Bitmap.createBitmap(widthPixels, widthPixels, Bitmap.Config.ARGB_8888);
        }

        @Override
        public void close() {
        }
    }

    private Activity activity;

    private ThreePageDocument document;

    private View documentView;

    @Before
    public void setUp() {
        activity = Robolectric.buildActivity(Activity.class).create().get();
        document = new ThreePageDocument();
        documentView = BrowserPdfDocumentPagesView.create(activity, document, PAGE_WIDTH_PIXELS);
    }

    private AdapterView<?> pageListIn(View view) {
        if (view instanceof AdapterView) return (AdapterView<?>) view;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int childIndex = 0; childIndex < group.getChildCount(); childIndex++) {
                AdapterView<?> found = pageListIn(group.getChildAt(childIndex));
                if (found != null) return found;
            }
        }
        return null;
    }

    private ListAdapter pageAdapter() {
        AdapterView<?> pageList = pageListIn(documentView);
        Assert.assertNotNull("a document is shown one page at a time as the reader scrolls, which"
            + " needs a list of pages rather than a fixed set of views", pageList);
        return (ListAdapter) pageList.getAdapter();
    }

    @Test
    public void everyPageOfTheDocumentIsAvailableToShow() {
        Assert.assertEquals("a document the user opened is only readable when every page of it can"
            + " be reached", PAGE_COUNT, pageAdapter().getCount());
    }

    @Test
    public void aPageIsRenderedAtTheWidthItIsShownAt() {
        View pageView = pageAdapter().getView(1, null, (ViewGroup) pageListIn(documentView));

        Assert.assertEquals("the page the reader scrolled to is the page that gets rendered",
            Collections.singletonList(1), document.renderedPageIndexes);
        Bitmap renderedPage = ((BitmapDrawable) ((ImageView) pageView).getDrawable()).getBitmap();
        Assert.assertEquals("a page rendered at another width is either unreadable or wastes memory",
            PAGE_WIDTH_PIXELS, renderedPage.getWidth());
    }

    @Test
    public void aPageIsRenderedOnlyWhenItIsAboutToBeShown() {
        Assert.assertTrue("rendering every page of a long document at once holds one bitmap per page"
                + " in a heap that is capped at a few hundred megabytes, which exhausts it",
            document.renderedPageIndexes.isEmpty());
    }
}
