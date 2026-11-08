package com.termux.app.handedness;

import android.app.Activity;
import android.content.Context;
import android.text.method.TransformationMethod;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.termux.R;
import com.termux.shared.termux.extrakeys.ExtraKeysConstants;
import com.termux.shared.termux.extrakeys.ExtraKeysInfo;
import com.termux.shared.termux.extrakeys.ExtraKeysView;
import com.termux.shared.termux.settings.properties.TermuxPropertyConstants;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class ExtraKeysLabelFitTest {

    private static final int TOOLBAR_WIDTH_PX = 1080;
    private static final int WIDE_WIDTH_PX = 2160;
    private static final int WINDOW_HEIGHT_PX = 1920;
    private static final float TOOLBAR_ROW_HEIGHT_DP = 32f;
    private static final double LABEL_FIT_TOLERANCE_PX = 1d;
    private static final double TEXT_SIZE_TOLERANCE_PX = 0.01d;

    @Test
    @Config(qualifiers = "w411dp-h914dp-420dpi")
    public void everyExtraKeysLabelFitsItsButtonInRightHandedMode() throws Exception {
        assertEveryLabelFits(Handedness.RIGHT_HANDED);
    }

    @Test
    @Config(qualifiers = "w411dp-h914dp-420dpi")
    public void everyExtraKeysLabelFitsItsButtonInLeftHandedMode() throws Exception {
        assertEveryLabelFits(Handedness.LEFT_HANDED);
    }

    @Test
    @Config(qualifiers = "w822dp-h914dp-420dpi")
    public void extraKeysTextSizeStaysAtTheDefaultWhenThereIsEnoughWidth() throws Exception {
        View root = attachedActivityLayout();
        new TerminalToolbarHandednessPart(root).applyHandedness(Handedness.RIGHT_HANDED);
        populateExtraKeysPagesAndLayout(root, WIDE_WIDTH_PX);

        float defaultTextSizePx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,
            ExtraKeysView.BUTTON_TEXT_SIZE_SP, root.getResources().getDisplayMetrics());
        for (TextView label : extraKeysLabels(root)) {
            Assert.assertEquals("text size of label \"" + renderedLabel(label) + "\"",
                (double) defaultTextSizePx, (double) label.getTextSize(), TEXT_SIZE_TOLERANCE_PX);
        }
    }

    @Test
    public void extraKeysTextSizeIsUniformAcrossKeys() throws Exception {
        View root = attachedActivityLayout();
        new TerminalToolbarHandednessPart(root).applyHandedness(Handedness.RIGHT_HANDED);
        populateExtraKeysPagesAndLayout(root, TOOLBAR_WIDTH_PX);

        List<TextView> labels = extraKeysLabels(root);
        float sharedTextSizePx = labels.get(0).getTextSize();
        for (TextView label : labels) {
            Assert.assertEquals("text size of label \"" + renderedLabel(label) + "\"",
                (double) sharedTextSizePx, (double) label.getTextSize(), TEXT_SIZE_TOLERANCE_PX);
        }
    }

    private static void assertEveryLabelFits(Handedness handedness) throws Exception {
        View root = attachedActivityLayout();
        new TerminalToolbarHandednessPart(root).applyHandedness(handedness);
        populateExtraKeysPagesAndLayout(root, TOOLBAR_WIDTH_PX);

        DisplayMetrics metrics = root.getResources().getDisplayMetrics();
        float defaultTextSizePx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,
            ExtraKeysView.BUTTON_TEXT_SIZE_SP, metrics);
        for (List<TextView> pageLabels : extraKeysLabelsPerPage(root)) {
            float fittedTextSizePx = pageLabels.get(0).getTextSize();
            Assert.assertTrue("fitted text size " + fittedTextSizePx + "px of the page is not smaller than the default "
                    + defaultTextSizePx + "px in " + handedness,
                fittedTextSizePx < defaultTextSizePx);
            for (TextView label : pageLabels) {
                String text = renderedLabel(label);
                Assert.assertEquals("text size of label \"" + text + "\" in " + handedness + " differs from the page size",
                    (double) fittedTextSizePx, (double) label.getTextSize(), TEXT_SIZE_TOLERANCE_PX);
                float neededPx = label.getPaint().measureText(text);
                int availablePx = label.getMeasuredWidth() - label.getPaddingLeft() - label.getPaddingRight();
                Assert.assertTrue("label \"" + text + "\" needs " + neededPx + "px but its button leaves "
                        + availablePx + "px in " + handedness,
                    neededPx <= availablePx + LABEL_FIT_TOLERANCE_PX);
            }
        }
    }

    private static void populateExtraKeysPagesAndLayout(View root, int widthPx) throws Exception {
        ViewPager pager = root.findViewById(R.id.terminal_toolbar_view_pager);
        ExtraKeysInfo info = new ExtraKeysInfo(TermuxPropertyConstants.DEFAULT_IVALUE_EXTRA_KEYS,
            TermuxPropertyConstants.DEFAULT_IVALUE_EXTRA_KEYS_STYLE, ExtraKeysConstants.CONTROL_CHARS_ALIASES);
        float rowHeightPx = TOOLBAR_ROW_HEIGHT_DP * pager.getResources().getDisplayMetrics().density;
        pager.setAdapter(new ExtraKeysPageAdapter(info, rowHeightPx));
        pager.setCurrentItem(0, false);
        pager.setVisibility(View.VISIBLE);
        root.measure(View.MeasureSpec.makeMeasureSpec(widthPx, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(WINDOW_HEIGHT_PX, View.MeasureSpec.EXACTLY));
        root.layout(0, 0, widthPx, WINDOW_HEIGHT_PX);
    }

    private static List<TextView> extraKeysLabels(View root) {
        List<TextView> labels = new ArrayList<>();
        for (List<TextView> pageLabels : extraKeysLabelsPerPage(root)) {
            labels.addAll(pageLabels);
        }
        return labels;
    }

    private static List<List<TextView>> extraKeysLabelsPerPage(View root) {
        ViewPager pager = root.findViewById(R.id.terminal_toolbar_view_pager);
        List<ExtraKeysView> extraKeysViews = new ArrayList<>();
        collectExtraKeysViews(pager, extraKeysViews);
        Assert.assertFalse("no ExtraKeysView was instantiated under the toolbar pager after layout",
            extraKeysViews.isEmpty());

        List<List<TextView>> pages = new ArrayList<>();
        for (ExtraKeysView extraKeysView : extraKeysViews) {
            List<TextView> labels = new ArrayList<>();
            for (int index = 0; index < extraKeysView.getChildCount(); index++) {
                View child = extraKeysView.getChildAt(index);
                if (child instanceof TextView && ((TextView) child).getText().length() > 0) {
                    labels.add((TextView) child);
                }
            }
            Assert.assertFalse("no Extra Keys label with text was found in an ExtraKeysView under the toolbar pager",
                labels.isEmpty());
            pages.add(labels);
        }
        return pages;
    }

    private static void collectExtraKeysViews(ViewGroup group, List<ExtraKeysView> extraKeysViews) {
        for (int index = 0; index < group.getChildCount(); index++) {
            View child = group.getChildAt(index);
            if (child instanceof ExtraKeysView) {
                extraKeysViews.add((ExtraKeysView) child);
            }
            if (child instanceof ViewGroup) {
                collectExtraKeysViews((ViewGroup) child, extraKeysViews);
            }
        }
    }

    private static String renderedLabel(TextView label) {
        CharSequence text = label.getText();
        TransformationMethod method = label.getTransformationMethod();
        CharSequence rendered = method == null ? null : method.getTransformation(text, label);
        return (rendered == null ? text : rendered).toString();
    }

    private static View attachedActivityLayout() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        Context context = new ContextThemeWrapper(RuntimeEnvironment.getApplication(),
            R.style.Theme_TermuxActivity_DayNight_NoActionBar);
        View root = LayoutInflater.from(context)
            .inflate(R.layout.activity_termux, new FrameLayout(context), false);
        activity.setContentView(root);
        return root;
    }

    private static final class ExtraKeysPageAdapter extends PagerAdapter {
        private final ExtraKeysInfo info;
        private final float rowHeightPx;

        ExtraKeysPageAdapter(ExtraKeysInfo info, float rowHeightPx) {
            this.info = info;
            this.rowHeightPx = rowHeightPx;
        }

        @Override
        public int getCount() {
            return 1;
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public Object instantiateItem(ViewGroup collection, int position) {
            View layout = LayoutInflater.from(collection.getContext())
                .inflate(R.layout.view_terminal_toolbar_extra_keys, collection, false);
            ((ExtraKeysView) layout).reload(info, rowHeightPx);
            collection.addView(layout);
            return layout;
        }

        @Override
        public void destroyItem(ViewGroup collection, int position, Object object) {
            collection.removeView((View) object);
        }
    }
}
