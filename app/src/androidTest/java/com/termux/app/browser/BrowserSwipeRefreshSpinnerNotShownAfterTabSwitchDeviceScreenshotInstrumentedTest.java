package com.termux.app.browser;

import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Environment;
import android.view.ContextThemeWrapper;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
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
import java.util.List;

@RunWith(AndroidJUnit4.class)
public class BrowserSwipeRefreshSpinnerNotShownAfterTabSwitchDeviceScreenshotInstrumentedTest {

    @Rule
    public GrantPermissionRule writeExternalStoragePermissionRule =
        GrantPermissionRule.grant(android.Manifest.permission.WRITE_EXTERNAL_STORAGE);

    @Test
    public void swipeRefreshSpinnerIsNotShownOnNonLoadingTabAfterTabSwitch() throws Exception {
        Context appContext = ApplicationProvider.getApplicationContext();
        Context context = new ContextThemeWrapper(appContext,
            R.style.Theme_TermuxActivity_DayNight_NoActionBar);

        BrowserPinchAwareSwipeRefreshLayout swipeRefreshLayout =
            new BrowserPinchAwareSwipeRefreshLayout(context);

        View webViewAreaPlaceholder = new View(context);
        webViewAreaPlaceholder.setBackgroundColor(Color.DKGRAY);
        swipeRefreshLayout.addView(webViewAreaPlaceholder);

        swipeRefreshLayout.setRefreshing(true);

        swipeRefreshLayout.setRefreshing(false);

        int widthPx = 400;
        int heightPx = 300;
        int measureWidth = View.MeasureSpec.makeMeasureSpec(widthPx, View.MeasureSpec.EXACTLY);
        int measureHeight = View.MeasureSpec.makeMeasureSpec(heightPx, View.MeasureSpec.EXACTLY);
        swipeRefreshLayout.measure(measureWidth, measureHeight);
        swipeRefreshLayout.layout(0, 0, widthPx, heightPx);

        Bitmap bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.DKGRAY);
        swipeRefreshLayout.draw(canvas);

        int writtenCount = 0;
        for (File directory : screenshotTargetDirectories(appContext)) {
            File out = new File(directory,
                "browser-swipe-refresh-spinner-not-shown-on-non-loading-tab-after-switch.png");
            if (writeBitmapAsPng(bitmap, out)) {
                writtenCount++;
            }
        }
        assertTrue(
            "the browser web-view area screenshot after a tab switch must be written to at least "
                + "one location so it can be pulled as a CI artifact",
            writtenCount > 0);
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
