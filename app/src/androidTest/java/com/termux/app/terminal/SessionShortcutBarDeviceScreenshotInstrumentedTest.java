package com.termux.app.terminal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Environment;
import android.view.ContextThemeWrapper;
import android.view.View;

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
    public void renderedControlBarPlacesShortcutsOnAFullWidthRowBelowTheControls() throws Exception {
        Context appContext = ApplicationProvider.getApplicationContext();
        Context context = new ContextThemeWrapper(appContext, R.style.Theme_TermuxActivity_DayNight_NoActionBar);

        View controlBar = View.inflate(context, R.layout.session_list_bottom_sheet_control_bar, null);
        ShortcutFlowLayout shortcutsContainer =
            controlBar.findViewById(R.id.session_list_bottom_sheet_shortcuts_container);

        Set<String> alwaysNaSessionNames = new LinkedHashSet<>(Arrays.asList("inbox", "review"));
        List<SessionDefinitionEntry> entries = Arrays.asList(
            new SessionDefinitionEntry("alpha", "storyA",
                java.util.Collections.singletonList("https://example.test/a1")),
            new SessionDefinitionEntry("beta", "storyB",
                java.util.Collections.singletonList("https://example.test/b1")));

        SessionShortcutBarPlanner planner =
            new SessionShortcutBarPlanner(new DefaultProjectManagerSessionPlanner());
        List<SessionShortcut> rightToLeftShortcuts =
            planner.planRightToLeftShortcuts(alwaysNaSessionNames, entries);
        Set<String> presentSessionNames = new LinkedHashSet<>(
            Arrays.asList("inbox", "review", "alphapm", "betapm"));
        List<SessionShortcut> renderOrderShortcuts =
            SessionShortcutBarPlanner.renderOrderPresentShortcuts(rightToLeftShortcuts, presentSessionNames);

        assertEquals(Arrays.asList("betapm", "alphapm", "review", "inbox"),
            targetSessionNames(renderOrderShortcuts));

        for (SessionShortcut shortcut : renderOrderShortcuts) {
            shortcutsContainer.addView(
                SessionListBottomSheetController.newShortcutButtonView(context, shortcut.getLabel()));
        }

        int widthSpec = View.MeasureSpec.makeMeasureSpec(BAR_WIDTH, View.MeasureSpec.EXACTLY);
        int heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        controlBar.measure(widthSpec, heightSpec);
        int barHeight = Math.max(1, controlBar.getMeasuredHeight());
        controlBar.layout(0, 0, BAR_WIDTH, barHeight);

        Bitmap bitmap = Bitmap.createBitmap(BAR_WIDTH, barHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(SURFACE_COLOR);
        controlBar.draw(canvas);

        View controlsGroup = controlBar.findViewById(R.id.session_list_bottom_sheet_controls_group);
        int controlsGroupBottomInBar = topInBar(controlBar, controlsGroup) + controlsGroup.getHeight();
        assertTrue("existing controls must sit at the top-left of the bar",
            controlsGroup.getLeft() < BAR_WIDTH / 2);
        int shortcutsTopInBar = topInBar(controlBar, shortcutsContainer);
        assertTrue("session shortcuts must sit on their own row below the controls",
            shortcutsTopInBar >= controlsGroupBottomInBar);
        int shortcutsLeftInBar = leftInBar(controlBar, shortcutsContainer);
        assertTrue("the session shortcut row must span the full width and start at the left edge",
            shortcutsLeftInBar <= controlBar.getPaddingLeft());
        int visibleRightEdge = BAR_WIDTH - controlBar.getPaddingRight();
        for (int index = 0; index < shortcutsContainer.getChildCount(); index++) {
            View shortcut = shortcutsContainer.getChildAt(index);
            assertTrue("every shortcut right edge must stay within the visible bar width",
                shortcutsLeftInBar + shortcut.getRight() <= visibleRightEdge);
        }
        assertEquals("all four present shortcuts must be rendered",
            4, shortcutsContainer.getChildCount());

        File outDir = sharedScreenshotDirectory();
        File out = new File(outDir, "session-shortcut-bar.png");
        try (FileOutputStream stream = new FileOutputStream(out)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        }
        assertTrue(out.exists() && out.length() > 0);
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
