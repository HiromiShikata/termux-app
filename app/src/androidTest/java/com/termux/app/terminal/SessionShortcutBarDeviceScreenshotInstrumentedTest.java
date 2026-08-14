package com.termux.app.terminal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Environment;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.ViewGroup;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.GrantPermissionRule;

import com.termux.R;
import com.termux.app.sessiondefinition.DefaultProjectManagerSessionPlanner;
import com.termux.app.sessiondefinition.SessionDefinitionEntry;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@RunWith(AndroidJUnit4.class)
public class SessionShortcutBarDeviceScreenshotInstrumentedTest {

    private static final int BAR_WIDTH = 1600;
    private static final int SURFACE_COLOR = 0xFF121212;

    @Rule
    public GrantPermissionRule writeExternalStoragePermissionRule =
        GrantPermissionRule.grant(android.Manifest.permission.WRITE_EXTERNAL_STORAGE);

    @Test
    public void renderedControlBarPlacesAlwaysSessionShortcutsAboveProjectManagerSessionShortcuts()
            throws Exception {
        Context appContext = ApplicationProvider.getApplicationContext();
        Context context = new ContextThemeWrapper(appContext, R.style.Theme_TermuxActivity_DayNight_NoActionBar);

        View controlBar = View.inflate(context, R.layout.session_list_bottom_sheet_control_bar, null);
        ShortcutFlowLayout alwaysSessionShortcutsContainer =
            controlBar.findViewById(R.id.session_list_bottom_sheet_always_session_shortcuts_container);
        ShortcutFlowLayout projectManagerSessionShortcutsContainer = controlBar.findViewById(
            R.id.session_list_bottom_sheet_project_manager_session_shortcuts_container);

        Set<String> alwaysNaSessionNames = new LinkedHashSet<>(Arrays.asList("inbox", "review"));
        List<SessionDefinitionEntry> entries = Arrays.asList(
            new SessionDefinitionEntry("alpha", "storyA",
                Collections.singletonList("https://example.test/a1")),
            new SessionDefinitionEntry("beta", "storyB",
                Collections.singletonList("https://example.test/b1")));

        SessionShortcutBarPlanner planner =
            new SessionShortcutBarPlanner(new DefaultProjectManagerSessionPlanner());
        SessionShortcutRows rightToLeftShortcutRows = planner.planRightToLeftShortcutRows(
            alwaysNaSessionNames, entries, Collections.emptyList());
        List<SessionShortcut> alwaysSessionRenderOrderShortcuts =
            SessionShortcutBarPlanner.renderOrderShortcuts(
                rightToLeftShortcutRows.getAlwaysSessionShortcuts());
        List<SessionShortcut> projectManagerRenderOrderShortcuts =
            SessionShortcutBarPlanner.renderOrderShortcuts(
                rightToLeftShortcutRows.getProjectManagerSessionShortcuts());

        assertEquals(Arrays.asList("review", "inbox"),
            targetSessionNames(alwaysSessionRenderOrderShortcuts));
        assertEquals(Arrays.asList("betapm", "alphapm"),
            targetSessionNames(projectManagerRenderOrderShortcuts));

        addShortcutButtons(context, alwaysSessionShortcutsContainer, alwaysSessionRenderOrderShortcuts);
        addShortcutButtons(context, projectManagerSessionShortcutsContainer,
            projectManagerRenderOrderShortcuts);

        int widthSpec = View.MeasureSpec.makeMeasureSpec(BAR_WIDTH, View.MeasureSpec.EXACTLY);
        int heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        controlBar.measure(widthSpec, heightSpec);
        int barHeight = Math.max(1, controlBar.getMeasuredHeight());
        controlBar.layout(0, 0, BAR_WIDTH, barHeight);

        Bitmap bitmap = Bitmap.createBitmap(BAR_WIDTH, barHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(SURFACE_COLOR);
        controlBar.draw(canvas);

        ViewGroup controlsGroup = controlBar.findViewById(R.id.session_list_bottom_sheet_controls_group);
        int controlsGroupBottomInBar = topInBar(controlBar, controlsGroup) + controlsGroup.getHeight();
        View firstControlButton = firstVisibleChild(controlsGroup);
        View lastControlButton = lastVisibleChild(controlsGroup);
        int controlsGroupRightEdge = controlsGroup.getWidth() - controlsGroup.getPaddingRight();
        assertTrue("the rightmost control icon button must reach the right edge of the controls row",
            lastControlButton.getRight() >= controlsGroupRightEdge - 1);
        assertTrue("the control icon buttons must be right-aligned, leaving free space on the left",
            firstControlButton.getLeft() > controlsGroup.getPaddingLeft());

        int alwaysSessionRowTopInBar = topInBar(controlBar, alwaysSessionShortcutsContainer);
        int projectManagerRowTopInBar = topInBar(controlBar, projectManagerSessionShortcutsContainer);
        assertTrue("the always-on session shortcuts must sit on their own row below the controls",
            alwaysSessionRowTopInBar >= controlsGroupBottomInBar);
        assertTrue("the project manager session shortcuts must sit on a row below the always-on session"
                + " shortcuts",
            projectManagerRowTopInBar
                >= alwaysSessionRowTopInBar + alwaysSessionShortcutsContainer.getHeight());

        assertRowFillsBarAndStaysWithinIt(controlBar, alwaysSessionShortcutsContainer);
        assertRowFillsBarAndStaysWithinIt(controlBar, projectManagerSessionShortcutsContainer);

        assertEquals("both always-on session shortcuts must be rendered on the upper row",
            2, alwaysSessionShortcutsContainer.getChildCount());
        assertEquals("both project manager session shortcuts must be rendered on the lower row",
            2, projectManagerSessionShortcutsContainer.getChildCount());

        File outDir = sharedScreenshotDirectory();
        File out = new File(outDir, "session-shortcut-bar.png");
        try (FileOutputStream stream = new FileOutputStream(out)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        }
        assertTrue(out.exists() && out.length() > 0);
    }

    private static void addShortcutButtons(Context context, ShortcutFlowLayout row,
                                           List<SessionShortcut> renderOrderShortcuts) {
        for (SessionShortcut shortcut : renderOrderShortcuts) {
            row.addView(SessionListBottomSheetController.newShortcutButtonView(context,
                shortcut.getLabel()));
        }
    }

    private static void assertRowFillsBarAndStaysWithinIt(View controlBar, ShortcutFlowLayout row) {
        assertTrue("a session shortcut row must span the full bar width",
            row.getWidth() >= BAR_WIDTH - controlBar.getPaddingLeft() - controlBar.getPaddingRight());
        int rowLeftInBar = leftInBar(controlBar, row);
        int visibleRightEdge = BAR_WIDTH - controlBar.getPaddingRight();
        for (int index = 0; index < row.getChildCount(); index++) {
            View shortcut = row.getChildAt(index);
            assertTrue("every shortcut right edge must stay within the visible bar width",
                rowLeftInBar + shortcut.getRight() <= visibleRightEdge);
        }
        View rightmostShortcut = row.getChildAt(row.getChildCount() - 1);
        int rightmostShortcutRightEdgeInBar = rowLeftInBar + rightmostShortcut.getRight();
        int shortcutRightMargin =
            ((ViewGroup.MarginLayoutParams) rightmostShortcut.getLayoutParams()).rightMargin;
        assertTrue("the rightmost shortcut of a row must be right-justified to end at the visible right edge",
            rightmostShortcutRightEdgeInBar >= visibleRightEdge - shortcutRightMargin - 1);
    }

    private static View firstVisibleChild(ViewGroup parent) {
        for (int index = 0; index < parent.getChildCount(); index++) {
            View child = parent.getChildAt(index);
            if (child.getVisibility() != View.GONE) {
                return child;
            }
        }
        throw new AssertionError("expected at least one visible control button");
    }

    private static View lastVisibleChild(ViewGroup parent) {
        for (int index = parent.getChildCount() - 1; index >= 0; index--) {
            View child = parent.getChildAt(index);
            if (child.getVisibility() != View.GONE) {
                return child;
            }
        }
        throw new AssertionError("expected at least one visible control button");
    }

    private static int leftInBar(View controlBar, View descendant) {
        int left = 0;
        View current = descendant;
        while (current != null && current != controlBar) {
            left += current.getLeft();
            Object parent = current.getParent();
            current = (parent instanceof View) ? (View) parent : null;
        }
        return left;
    }

    private static int topInBar(View controlBar, View descendant) {
        int top = 0;
        View current = descendant;
        while (current != null && current != controlBar) {
            top += current.getTop();
            Object parent = current.getParent();
            current = (parent instanceof View) ? (View) parent : null;
        }
        return top;
    }

    private static List<String> targetSessionNames(List<SessionShortcut> shortcuts) {
        List<String> result = new java.util.ArrayList<>();
        for (SessionShortcut shortcut : shortcuts) {
            result.add(shortcut.getTargetSessionName());
        }
        return result;
    }

    private static File sharedScreenshotDirectory() {
        File directory = new File(Environment.getExternalStorageDirectory(),
            "termux-instrumentation-screenshots");
        if (!directory.exists()) {
            assertTrue("shared screenshot directory must be creatable so the rendered PNG survives "
                + "the post-test APK uninstall and can be pulled as a CI artifact", directory.mkdirs());
        }
        return directory;
    }
}
