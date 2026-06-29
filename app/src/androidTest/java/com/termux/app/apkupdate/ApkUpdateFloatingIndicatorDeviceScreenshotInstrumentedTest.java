package com.termux.app.apkupdate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Environment;
import android.view.ContextThemeWrapper;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.GrantPermissionRule;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.termux.R;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public class ApkUpdateFloatingIndicatorDeviceScreenshotInstrumentedTest {

    private static final String LATEST_VERSION_NAME = "0.121.0";

    @Rule
    public GrantPermissionRule writeExternalStoragePermissionRule =
        GrantPermissionRule.grant(android.Manifest.permission.WRITE_EXTERNAL_STORAGE);

    private static final class ButtonBackedIndicatorView
        implements ApkUpdateFloatingIndicatorController.IndicatorView {

        private final FloatingActionButton indicator;

        ButtonBackedIndicatorView(FloatingActionButton indicator) {
            this.indicator = indicator;
        }

        @Override
        public void showUpdateAvailable(String latestVersionName, Runnable onTapped) {
            indicator.setContentDescription("Update available, version " + latestVersionName);
            indicator.setOnClickListener(view -> onTapped.run());
            indicator.setVisibility(View.VISIBLE);
        }

        @Override
        public void hide() {
            indicator.setOnClickListener(null);
            indicator.setVisibility(View.GONE);
        }
    }

    private FloatingActionButton newIndicatorButton(Context context) {
        FloatingActionButton indicator = new FloatingActionButton(context);
        indicator.setImageResource(R.drawable.ic_apk_update_available);
        indicator.setVisibility(View.GONE);
        return indicator;
    }

    private void measureAndLayout(View view, int sizePx) {
        int spec = View.MeasureSpec.makeMeasureSpec(sizePx, View.MeasureSpec.EXACTLY);
        view.measure(spec, spec);
        view.layout(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());
    }

    @Test
    public void floatingUpdateButtonIsShownWhenUpdateIsAvailable() throws Exception {
        Context appContext = ApplicationProvider.getApplicationContext();
        Context context = new ContextThemeWrapper(appContext,
            com.google.android.material.R.style.Theme_MaterialComponents_DayNight);

        FloatingActionButton indicator = newIndicatorButton(context);
        ApkUpdateFloatingIndicatorController controller = new ApkUpdateFloatingIndicatorController(
            new ButtonBackedIndicatorView(indicator), availability -> { });

        controller.onUpdateAvailable(ApkUpdateAvailability.available(
            LATEST_VERSION_NAME, "https://example.com/arm64", "termux-app_arm64-v8a.apk"));

        assertEquals(View.VISIBLE, indicator.getVisibility());
        assertTrue(controller.hasPendingUpdate());

        int sizePx = 168;
        measureAndLayout(indicator, sizePx);
        Bitmap bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(0xFF121212);
        indicator.draw(canvas);

        int writtenCount = 0;
        for (File directory : screenshotTargetDirectories(appContext)) {
            File out = new File(directory, "apk-update-floating-button-shown-when-update-available.png");
            if (writeBitmapAsPng(bitmap, out)) {
                writtenCount++;
            }
        }
        assertTrue("the rendered floating update button screenshot must be written to at least one "
            + "location so it can be pulled as a CI artifact or inspected locally", writtenCount > 0);
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

    @Test
    public void floatingUpdateButtonIsHiddenWhenUpToDate() {
        Context appContext = ApplicationProvider.getApplicationContext();
        Context context = new ContextThemeWrapper(appContext,
            com.google.android.material.R.style.Theme_MaterialComponents_DayNight);

        FloatingActionButton indicator = newIndicatorButton(context);
        ApkUpdateFloatingIndicatorController controller = new ApkUpdateFloatingIndicatorController(
            new ButtonBackedIndicatorView(indicator), availability -> { });

        controller.onUpdateAvailable(ApkUpdateAvailability.upToDate(LATEST_VERSION_NAME));

        assertEquals(View.GONE, indicator.getVisibility());
    }
}
