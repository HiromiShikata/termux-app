package com.termux.app.handedness;

import android.content.Context;
import android.text.method.TransformationMethod;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.termux.R;
import com.termux.shared.termux.extrakeys.ExtraKeysConstants;
import com.termux.shared.termux.extrakeys.ExtraKeysInfo;
import com.termux.shared.termux.extrakeys.ExtraKeysView;
import com.termux.shared.termux.settings.properties.TermuxPropertyConstants;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class ExtraKeysUnmeasuredTextSizeTest {

    private static final int WINDOW_WIDTH_PX = 1080;
    private static final int WINDOW_HEIGHT_PX = 1920;
    private static final float TOOLBAR_ROW_HEIGHT_DP = 32f;
    private static final double TEXT_SIZE_TOLERANCE_PX = 0.01d;

    @Test
    @Config(qualifiers = "w411dp-h914dp-420dpi")
    public void textSizeIsUnchangedWhenLayoutRunsWithoutMeasure() throws Exception {
        ExtraKeysView extraKeysView = inflateExtraKeysView();
        List<TextView> labels = labelsOf(extraKeysView);
        List<Float> recordedTextSizesPx = textSizesOf(labels);

        for (TextView label : labels) {
            Assert.assertEquals("measured width of label \"" + renderedLabel(label) + "\" before layout",
                0, label.getMeasuredWidth());
        }

        extraKeysView.requestLayout();
        extraKeysView.layout(0, 0, 0, 0);

        assertTextSizesUnchanged(labels, recordedTextSizesPx);
    }

    @Test
    @Config(qualifiers = "w411dp-h914dp-420dpi")
    public void textSizeIsUnchangedWhenButtonAvailableWidthIsNotPositive() throws Exception {
        ExtraKeysView extraKeysView = inflateExtraKeysView();
        extraKeysView.measure(View.MeasureSpec.makeMeasureSpec(WINDOW_WIDTH_PX, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(WINDOW_HEIGHT_PX, View.MeasureSpec.EXACTLY));
        List<TextView> labels = labelsOf(extraKeysView);
        for (TextView label : labels) {
            Assert.assertTrue("measured width of label \"" + renderedLabel(label) + "\" should be positive after measure",
                label.getMeasuredWidth() > 0);
        }
        List<Float> recordedTextSizesPx = textSizesOf(labels);

        for (TextView label : labels) {
            label.setPadding(label.getMeasuredWidth(), 0, 0, 0);
        }

        extraKeysView.requestLayout();
        extraKeysView.layout(0, 0, WINDOW_WIDTH_PX, WINDOW_HEIGHT_PX);

        assertTextSizesUnchanged(labels, recordedTextSizesPx);
    }

    private static void assertTextSizesUnchanged(List<TextView> labels, List<Float> recordedTextSizesPx) {
        for (int index = 0; index < labels.size(); index++) {
            TextView label = labels.get(index);
            Assert.assertEquals("text size of label \"" + renderedLabel(label) + "\" after layout",
                (double) recordedTextSizesPx.get(index), (double) label.getTextSize(), TEXT_SIZE_TOLERANCE_PX);
        }
    }

    private static ExtraKeysView inflateExtraKeysView() throws Exception {
        Context context = new ContextThemeWrapper(RuntimeEnvironment.getApplication(),
            R.style.Theme_TermuxActivity_DayNight_NoActionBar);
        ExtraKeysView extraKeysView = (ExtraKeysView) LayoutInflater.from(context)
            .inflate(R.layout.view_terminal_toolbar_extra_keys, null, false);
        ExtraKeysInfo info = new ExtraKeysInfo(TermuxPropertyConstants.DEFAULT_IVALUE_EXTRA_KEYS,
            TermuxPropertyConstants.DEFAULT_IVALUE_EXTRA_KEYS_STYLE, ExtraKeysConstants.CONTROL_CHARS_ALIASES);
        float rowHeightPx = TOOLBAR_ROW_HEIGHT_DP * context.getResources().getDisplayMetrics().density;
        extraKeysView.reload(info, rowHeightPx);
        return extraKeysView;
    }

    private static List<TextView> labelsOf(ExtraKeysView extraKeysView) {
        List<TextView> labels = new ArrayList<>();
        for (int index = 0; index < extraKeysView.getChildCount(); index++) {
            View child = extraKeysView.getChildAt(index);
            if (child instanceof TextView && ((TextView) child).getText().length() > 0) {
                labels.add((TextView) child);
            }
        }
        Assert.assertFalse("no Extra Keys label with text was found in the ExtraKeysView after reload",
            labels.isEmpty());
        return labels;
    }

    private static List<Float> textSizesOf(List<TextView> labels) {
        List<Float> textSizesPx = new ArrayList<>();
        for (TextView label : labels) {
            textSizesPx.add(label.getTextSize());
        }
        return textSizesPx;
    }

    private static String renderedLabel(TextView label) {
        CharSequence text = label.getText();
        TransformationMethod method = label.getTransformationMethod();
        CharSequence rendered = method == null ? null : method.getTransformation(text, label);
        return (rendered == null ? text : rendered).toString();
    }
}
