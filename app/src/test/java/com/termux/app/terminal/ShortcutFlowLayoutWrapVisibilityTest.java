package com.termux.app.terminal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.termux.R;
import com.termux.app.sessiondefinition.DefaultProjectManagerSessionPlanner;
import com.termux.app.sessiondefinition.SessionDefinitionEntry;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@RunWith(RobolectricTestRunner.class)
public class ShortcutFlowLayoutWrapVisibilityTest {

    private static final int FIXED_CHILD_WIDTH_PIXELS = 100;
    private static final int FIXED_CHILD_HEIGHT_PIXELS = 40;
    private static final int HORIZONTAL_MARGIN_PIXELS = 1;
    private static final int TOP_MARGIN_PIXELS = 3;

    @Test
    public void everyChildIncludingTheLastStaysInsideBoundsWhenTheRowOverflows() {
        Context context = RuntimeEnvironment.getApplication();
        ShortcutFlowLayout flowLayout = new ShortcutFlowLayout(context);
        for (int index = 0; index < 8; index++) {
            flowLayout.addView(fixedSizeChild(context));
        }
        int narrowWidth = 250;

        measureAndLayout(flowLayout, narrowWidth);

        int rowHeight = FIXED_CHILD_HEIGHT_PIXELS + TOP_MARGIN_PIXELS;
        assertTrue("the bar must wrap onto more than one row when a single row overflows",
            flowLayout.getMeasuredHeight() > rowHeight);
        assertEveryChildIsInsideBounds(flowLayout);
        View firstChild = flowLayout.getChildAt(0);
        View lastChild = flowLayout.getChildAt(flowLayout.getChildCount() - 1);
        assertTrue("the last child (rendered where the always-NA shortcut sits) must wrap onto a lower row",
            lastChild.getTop() > firstChild.getTop());
    }

    @Test
    public void alwaysNaSecretaryShortcutStaysVisibleWithManyProjectButtons() {
        Context context = new ContextThemeWrapper(RuntimeEnvironment.getApplication(),
            R.style.Theme_TermuxActivity_DayNight_NoActionBar);
        List<SessionShortcut> renderOrderShortcuts = renderOrderShortcutsWithSecretaryAndManyProjects();

        ShortcutFlowLayout flowLayout = new ShortcutFlowLayout(context);
        Set<String> presentLabels = new LinkedHashSet<>();
        for (SessionShortcut shortcut : renderOrderShortcuts) {
            flowLayout.addView(SessionListBottomSheetController.newShortcutButtonView(context,
                shortcut.getLabel()));
            presentLabels.add(shortcut.getLabel());
        }
        assertTrue("the always-NA secretary shortcut must be part of the rendered bar",
            presentLabels.contains("secretary"));

        int singleRowNaturalWidth = measureSingleRowNaturalWidth(flowLayout);
        int constrainedWidth = singleRowNaturalWidth / 2;
        measureAndLayout(flowLayout, constrainedWidth);

        assertTrue("the projects plus secretary must be too wide for one row so wrapping is exercised",
            singleRowNaturalWidth > constrainedWidth);
        TextView secretaryButton = findButtonByText(flowLayout, "secretary");
        assertNotNull("the secretary button must exist in the bar", secretaryButton);
        assertChildIsInsideBounds(flowLayout, secretaryButton);
        assertEveryChildIsInsideBounds(flowLayout);
        int firstButtonRowHeight = flowLayout.getChildAt(0).getMeasuredHeight() + TOP_MARGIN_PIXELS;
        assertTrue("the bar must wrap onto additional rows rather than drop overflow buttons",
            flowLayout.getMeasuredHeight() > firstButtonRowHeight);
    }

    private List<SessionShortcut> renderOrderShortcutsWithSecretaryAndManyProjects() {
        List<SessionDefinitionEntry> entries = new ArrayList<>();
        Set<String> presentSessionNames = new LinkedHashSet<>();
        presentSessionNames.add("secretary");
        for (int index = 0; index < 12; index++) {
            String projectLabel = "project" + index;
            entries.add(new SessionDefinitionEntry(projectLabel, "story",
                Collections.singletonList("https://example.test/" + index)));
            presentSessionNames.add(projectLabel + "pm");
        }
        Set<String> alwaysNaSessionNames = new LinkedHashSet<>();
        alwaysNaSessionNames.add("secretary");

        SessionShortcutBarPlanner planner =
            new SessionShortcutBarPlanner(new DefaultProjectManagerSessionPlanner());
        List<SessionShortcut> rightToLeftShortcuts =
            planner.planRightToLeftShortcuts(alwaysNaSessionNames, entries);
        return SessionShortcutBarPlanner.renderOrderPresentShortcuts(rightToLeftShortcuts,
            presentSessionNames);
    }

    private static View fixedSizeChild(Context context) {
        View child = new View(context);
        ViewGroup.MarginLayoutParams layoutParams =
            new ViewGroup.MarginLayoutParams(FIXED_CHILD_WIDTH_PIXELS, FIXED_CHILD_HEIGHT_PIXELS);
        layoutParams.leftMargin = HORIZONTAL_MARGIN_PIXELS;
        layoutParams.rightMargin = HORIZONTAL_MARGIN_PIXELS;
        layoutParams.topMargin = TOP_MARGIN_PIXELS;
        child.setLayoutParams(layoutParams);
        return child;
    }

    private static int measureSingleRowNaturalWidth(ShortcutFlowLayout flowLayout) {
        int unspecified = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        flowLayout.measure(unspecified, unspecified);
        return flowLayout.getMeasuredWidth();
    }

    private static void measureAndLayout(ShortcutFlowLayout flowLayout, int exactWidthPixels) {
        int widthSpec = View.MeasureSpec.makeMeasureSpec(exactWidthPixels, View.MeasureSpec.EXACTLY);
        int heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        flowLayout.measure(widthSpec, heightSpec);
        flowLayout.layout(0, 0, flowLayout.getMeasuredWidth(), flowLayout.getMeasuredHeight());
    }

    private static void assertEveryChildIsInsideBounds(ShortcutFlowLayout flowLayout) {
        for (int index = 0; index < flowLayout.getChildCount(); index++) {
            assertChildIsInsideBounds(flowLayout, flowLayout.getChildAt(index));
        }
    }

    private static void assertChildIsInsideBounds(ShortcutFlowLayout flowLayout, View child) {
        assertTrue("child left edge must not be clipped", child.getLeft() >= 0);
        assertTrue("child top edge must not be clipped", child.getTop() >= 0);
        assertTrue("child right edge must stay within the bar width",
            child.getRight() <= flowLayout.getMeasuredWidth());
        assertTrue("child bottom edge must stay within the bar height",
            child.getBottom() <= flowLayout.getMeasuredHeight());
    }

    @Nullable
    private static TextView findButtonByText(ShortcutFlowLayout flowLayout, String text) {
        for (int index = 0; index < flowLayout.getChildCount(); index++) {
            View child = flowLayout.getChildAt(index);
            if (child instanceof TextView && text.contentEquals(((TextView) child).getText())) {
                return (TextView) child;
            }
        }
        return null;
    }

    @Test
    public void renderOrderPlacesTheAlwaysNaSecretaryShortcutLastAmongManyProjects() {
        List<SessionShortcut> renderOrderShortcuts = renderOrderShortcutsWithSecretaryAndManyProjects();
        SessionShortcut lastShortcut = renderOrderShortcuts.get(renderOrderShortcuts.size() - 1);
        assertEquals("secretary", lastShortcut.getLabel());
    }
}
