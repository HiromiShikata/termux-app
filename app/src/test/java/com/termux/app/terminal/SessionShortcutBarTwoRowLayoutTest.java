package com.termux.app.terminal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.termux.R;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class SessionShortcutBarTwoRowLayoutTest {

    private static final int BAR_WIDTH_PIXELS = 720;

    @Test
    public void theAlwaysOnRowSitsAboveTheProjectManagerRow() {
        Context context = themedContext();
        ViewGroup controlBar = inflateControlBar(context);
        ShortcutFlowLayout alwaysSessionRow = alwaysSessionRow(controlBar);
        ShortcutFlowLayout projectManagerRow = projectManagerRow(controlBar);
        addShortcutButtons(context, alwaysSessionRow, 2);
        addShortcutButtons(context, projectManagerRow, 4);

        measureAndLayout(controlBar, BAR_WIDTH_PIXELS);

        assertTrue("the owner asked for the always-on session shortcuts on the upper row and the project"
                + " manager session shortcuts on the lower one, so a single flowing row is not what was"
                + " asked for", projectManagerRow.getTop() >= alwaysSessionRow.getBottom());
    }

    @Test
    public void eachRowSpansTheFullBarWidthSoNeitherSharesALineWithTheOther() {
        Context context = themedContext();
        ViewGroup controlBar = inflateControlBar(context);
        ShortcutFlowLayout alwaysSessionRow = alwaysSessionRow(controlBar);
        ShortcutFlowLayout projectManagerRow = projectManagerRow(controlBar);
        addShortcutButtons(context, alwaysSessionRow, 2);
        addShortcutButtons(context, projectManagerRow, 4);

        measureAndLayout(controlBar, BAR_WIDTH_PIXELS);

        int barContentWidth = controlBar.getWidth() - controlBar.getPaddingLeft()
            - controlBar.getPaddingRight();
        assertTrue(alwaysSessionRow.getWidth() >= barContentWidth);
        assertTrue(projectManagerRow.getWidth() >= barContentWidth);
    }

    @Test
    public void bothRowsSitBelowTheControlButtons() {
        Context context = themedContext();
        ViewGroup controlBar = inflateControlBar(context);
        View controlsGroup = controlBar.findViewById(R.id.session_list_bottom_sheet_controls_group);
        ShortcutFlowLayout alwaysSessionRow = alwaysSessionRow(controlBar);
        ShortcutFlowLayout projectManagerRow = projectManagerRow(controlBar);
        addShortcutButtons(context, alwaysSessionRow, 2);
        addShortcutButtons(context, projectManagerRow, 4);

        measureAndLayout(controlBar, BAR_WIDTH_PIXELS);

        assertTrue(alwaysSessionRow.getTop() >= controlsGroup.getBottom());
        assertTrue(projectManagerRow.getTop() >= controlsGroup.getBottom());
    }

    @Test
    public void anEmptyRowTakesNoHeightSoTheBarDoesNotGrowAGapWhereNoShortcutIs() {
        Context context = themedContext();
        ViewGroup controlBar = inflateControlBar(context);
        ShortcutFlowLayout alwaysSessionRow = alwaysSessionRow(controlBar);
        ShortcutFlowLayout projectManagerRow = projectManagerRow(controlBar);
        addShortcutButtons(context, projectManagerRow, 4);

        measureAndLayout(controlBar, BAR_WIDTH_PIXELS);

        assertEquals("a row holding nothing must not push the row below it down", 0,
            alwaysSessionRow.getHeight());
    }

    private static ShortcutFlowLayout alwaysSessionRow(ViewGroup controlBar) {
        ShortcutFlowLayout row =
            controlBar.findViewById(R.id.session_list_bottom_sheet_always_session_shortcuts_container);
        assertNotNull("the upper row has to exist for the always-on session shortcuts to have a row of"
            + " their own", row);
        return row;
    }

    private static ShortcutFlowLayout projectManagerRow(ViewGroup controlBar) {
        ShortcutFlowLayout row = controlBar.findViewById(
            R.id.session_list_bottom_sheet_project_manager_session_shortcuts_container);
        assertNotNull("the lower row has to exist for the project manager session shortcuts to have a"
            + " row of their own", row);
        return row;
    }

    private static Context themedContext() {
        return new ContextThemeWrapper(RuntimeEnvironment.getApplication(),
            R.style.Theme_TermuxActivity_DayNight_NoActionBar);
    }

    private static ViewGroup inflateControlBar(Context context) {
        return (ViewGroup) LayoutInflater.from(context)
            .inflate(R.layout.session_list_bottom_sheet_control_bar, null);
    }

    private static void addShortcutButtons(Context context, ShortcutFlowLayout row, int buttonCount) {
        for (int index = 0; index < buttonCount; index++) {
            row.addView(SessionListBottomSheetController.newShortcutButtonView(context,
                "shortcut" + index));
        }
    }

    private static void measureAndLayout(ViewGroup controlBar, int exactWidthPixels) {
        int widthSpec = View.MeasureSpec.makeMeasureSpec(exactWidthPixels, View.MeasureSpec.EXACTLY);
        int heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        controlBar.measure(widthSpec, heightSpec);
        controlBar.layout(0, 0, controlBar.getMeasuredWidth(), controlBar.getMeasuredHeight());
    }
}
