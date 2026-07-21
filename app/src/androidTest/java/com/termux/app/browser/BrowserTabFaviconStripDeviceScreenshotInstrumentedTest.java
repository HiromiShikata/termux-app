package com.termux.app.browser;

import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Environment;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.GrantPermissionRule;

import com.termux.R;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public class BrowserTabFaviconStripDeviceScreenshotInstrumentedTest {

    private static final String SESSION = "session";

    @Rule
    public GrantPermissionRule writeExternalStoragePermissionRule =
        GrantPermissionRule.grant(android.Manifest.permission.WRITE_EXTERNAL_STORAGE);

    private static final class NoOpSelectionListener implements BrowserTabSelectionListener {
        @Nullable BrowserTab activeTab;

        @Override
        public void openTab(@NonNull BrowserTab tab) { }

        @Override
        public void closeTab(@NonNull BrowserTab tab) { }

        @Override
        public void promptNewTab() { }

        @Nullable
        @Override
        public BrowserTab getActiveTab() {
            return activeTab;
        }
    }

    @Test
    public void loadingIndicatorAppearsOnlyOnTheLoadingTab() throws Exception {
        Context appContext = ApplicationProvider.getApplicationContext();
        Context context = new ContextThemeWrapper(appContext,
            R.style.Theme_TermuxActivity_DayNight_NoActionBar);

        HorizontalScrollView scrollView = new HorizontalScrollView(context);
        LinearLayout container = new LinearLayout(context);
        scrollView.addView(container);
        NoOpSelectionListener listener = new NoOpSelectionListener();
        BrowserTab loadingTab = new BrowserTab(SESSION, "https://loading.example/");
        loadingTab.setLoading(true);
        BrowserTab idleTab = new BrowserTab(SESSION, "https://idle.example/");
        listener.activeTab = loadingTab;

        new BrowserTabFaviconStripController(scrollView, container, listener)
            .update(Arrays.asList(loadingTab, idleTab), loadingTab);

        int unspecified = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        scrollView.measure(unspecified, unspecified);
        scrollView.layout(0, 0, scrollView.getMeasuredWidth(), scrollView.getMeasuredHeight());

        float density = context.getResources().getDisplayMetrics().density;
        int marginPx = Math.round(12f * density);
        int width = scrollView.getMeasuredWidth() + marginPx * 2;
        int height = scrollView.getMeasuredHeight() + marginPx * 2;
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(0xFF202124);
        canvas.save();
        canvas.translate(marginPx, marginPx);
        scrollView.draw(canvas);
        canvas.restore();

        int writtenCount = 0;
        for (File directory : screenshotTargetDirectories(appContext)) {
            File out = new File(directory,
                "browser-tab-favicon-strip-loading-indicator-on-loading-tab-only.png");
            if (writeBitmapAsPng(bitmap, out)) {
                writtenCount++;
            }
        }
        assertTrue("the rendered favicon strip loading-indicator screenshot must be written to at "
            + "least one location so it can be pulled as a CI artifact or inspected locally",
            writtenCount > 0);
    }

    @Test
    public void faviconStripChipShowsSmallCloseButtonAtTheTopRightCorner() throws Exception {
        Context appContext = ApplicationProvider.getApplicationContext();
        Context context = new ContextThemeWrapper(appContext,
            R.style.Theme_TermuxActivity_DayNight_NoActionBar);

        HorizontalScrollView scrollView = new HorizontalScrollView(context);
        LinearLayout container = new LinearLayout(context);
        scrollView.addView(container);
        NoOpSelectionListener listener = new NoOpSelectionListener();
        BrowserTab firstTab = new BrowserTab(SESSION, "https://first.example/");
        BrowserTab secondTab = new BrowserTab(SESSION, "https://second.example/");

        new BrowserTabFaviconStripController(scrollView, container, listener)
            .update(Arrays.asList(firstTab, secondTab), firstTab);

        int unspecified = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        scrollView.measure(unspecified, unspecified);
        scrollView.layout(0, 0, scrollView.getMeasuredWidth(), scrollView.getMeasuredHeight());

        float density = context.getResources().getDisplayMetrics().density;
        int marginPx = Math.round(12f * density);
        int width = scrollView.getMeasuredWidth() + marginPx * 2;
        int height = scrollView.getMeasuredHeight() + marginPx * 2;
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(0xFF202124);
        canvas.save();
        canvas.translate(marginPx, marginPx);
        scrollView.draw(canvas);
        canvas.restore();

        int writtenCount = 0;
        for (File directory : screenshotTargetDirectories(appContext)) {
            File out = new File(directory,
                "browser-tab-favicon-strip-close-button-top-right-corner.png");
            if (writeBitmapAsPng(bitmap, out)) {
                writtenCount++;
            }
        }
        assertTrue("the rendered favicon strip screenshot must be written to at least one location "
            + "so it can be pulled as a CI artifact or inspected locally", writtenCount > 0);
    }

    private static List<File> screenshotTargetDirectories(@NonNull Context context) {
        List<File> directories = new ArrayList<>();
        File sharedDirectory = new File(Environment.getExternalStorageDirectory(),
            "termux-instrumentation-screenshots");
        if (sharedDirectory.exists() || sharedDirectory.mkdirs()) {
            directories.add(sharedDirectory);
        }
        File appExternalFilesDirectory = context.getExternalFilesDir(null);
        if (appExternalFilesDirectory != null
            && (appExternalFilesDirectory.exists() || appExternalFilesDirectory.mkdirs())) {
            directories.add(appExternalFilesDirectory);
        }
        return directories;
    }

    private static boolean writeBitmapAsPng(@NonNull Bitmap bitmap, @NonNull File file) {
        try (FileOutputStream stream = new FileOutputStream(file)) {
            return bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream) && file.length() > 0;
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }
}
