package com.termux.app.apkupdate;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.termux.R;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.GraphicsMode;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

@RunWith(RobolectricTestRunner.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class ApkUpdateFloatingIndicatorRenderTest {

    @Test
    public void rendersIconOnlyFloatingUpdateButton() throws IOException {
        View root = inflateActivityLayout();
        FloatingActionButton indicator = root.findViewById(R.id.apk_update_floating_indicator);

        indicator.setContentDescription(indicator.getContext().getString(
            R.string.apk_update_floating_indicator_content_description, "0.121.0"));
        indicator.setVisibility(View.VISIBLE);

        Assert.assertEquals(View.VISIBLE, indicator.getVisibility());
        Assert.assertNotNull(indicator.getContentDescription());

        Bitmap bitmap = renderToBitmap(indicator);
        File output = new File(System.getProperty("java.io.tmpdir"),
            "apk-update-floating-indicator-icon-only.png");
        try (FileOutputStream stream = new FileOutputStream(output)) {
            Assert.assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream));
        }
        Assert.assertTrue("rendered icon-only floating update button screenshot must be written",
            output.exists() && output.length() > 0);
        Assert.assertTrue(bitmap.getWidth() > 0 && bitmap.getHeight() > 0);
    }

    private static Bitmap renderToBitmap(View view) {
        view.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        int width = Math.max(1, view.getMeasuredWidth());
        int height = Math.max(1, view.getMeasuredHeight());
        view.layout(0, 0, width, height);
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(0xFF101010);
        view.draw(canvas);
        return bitmap;
    }

    private static View inflateActivityLayout() {
        Context context = new ContextThemeWrapper(RuntimeEnvironment.getApplication(),
            R.style.Theme_TermuxActivity_DayNight_NoActionBar);
        return LayoutInflater.from(context)
            .inflate(R.layout.activity_termux, new FrameLayout(context), false);
    }
}
