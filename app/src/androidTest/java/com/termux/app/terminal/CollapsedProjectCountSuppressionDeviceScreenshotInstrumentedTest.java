package com.termux.app.terminal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.TextView;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.termux.R;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.termux.app.sessiondefinition.SessionDefinitionEntry;

@RunWith(AndroidJUnit4.class)
public class CollapsedProjectCountSuppressionDeviceScreenshotInstrumentedTest {

    private static final int SURFACE_COLOR = 0xFF121212;
    private static final int TEXT_COLOR = 0xFFE0E0E0;
    private static final int ROW_WIDTH = 720;
    private static final int ROW_HEIGHT = 64;
    private static final int ROW_GAP = 6;

    private static View inflateProjectHeaderRow(Context context, String titleText, boolean collapsed) {
        View row = View.inflate(context, R.layout.item_terminal_sessions_project_header, null);
        TextView indicator = row.findViewById(R.id.session_project_header_collapse_indicator);
        TextView title = row.findViewById(R.id.session_project_header_title);
        indicator.setText(collapsed ? "▸" : "▾");
        indicator.setTextColor(TEXT_COLOR);
        title.setText(titleText);
        title.setTextColor(TEXT_COLOR);
        measureAndLayout(row);
        return row;
    }

    private static TextView inflateHeaderTitle(Context context, String text) {
        TextView title = new TextView(context);
        title.setText(text);
        title.setTextColor(TEXT_COLOR);
        title.setPadding(12, 12, 12, 12);
        measureAndLayout(title);
        return title;
    }

    private static void measureAndLayout(View view) {
        int widthSpec = View.MeasureSpec.makeMeasureSpec(ROW_WIDTH, View.MeasureSpec.EXACTLY);
        int heightSpec = View.MeasureSpec.makeMeasureSpec(ROW_HEIGHT, View.MeasureSpec.EXACTLY);
        view.measure(widthSpec, heightSpec);
        view.layout(0, 0, ROW_WIDTH, ROW_HEIGHT);
    }

    @Test
    public void collapsedProjectShowsNoCountAndHeaderTotalCountsOnlyExpandedSessions() throws Exception {
        Context appContext = ApplicationProvider.getApplicationContext();
        Context context = new ContextThemeWrapper(appContext, R.style.Theme_TermuxActivity_DayNight_NoActionBar);

        SessionHierarchyBuilder builder = new SessionHierarchyBuilder();
        List<SessionDefinitionEntry> entries = Arrays.asList(
            new SessionDefinitionEntry("collapsedProject", "storyA",
                Arrays.asList("https://example.test/a1", "https://example.test/a2")),
            new SessionDefinitionEntry("expandedProject", "storyB",
                Collections.singletonList("https://example.test/b1")));
        List<String> sessionNames = Arrays.asList(
            "https://example.test/a1", "https://example.test/a2", "https://example.test/b1");
        List<SessionHierarchyRow> rows = builder.build(sessionNames, entries, "N/A");
        Set<String> collapsed = new LinkedHashSet<>(Collections.singletonList("collapsedProject"));
        Set<String> pendingCallSessionNames = new LinkedHashSet<>(
            Collections.singletonList("https://example.test/b1"));

        List<SessionHierarchyRow> countedRows =
            SessionHierarchyBuilder.filterCollapsedProjectSessions(rows, collapsed);
        int headerTotal = SessionHierarchyBuilder.totalSessionCount(countedRows);
        int headerPending = SessionHierarchyBuilder.pendingCallSessionCount(
            countedRows, sessionNames, pendingCallSessionNames);

        assertEquals("header total must count only the expanded project's session", 1, headerTotal);
        assertEquals("header numerator must count only the expanded project's pending call", 1, headerPending);

        String headerTitleText = SessionListBottomSheetController.sessionCountTitle(
            "Sessions", headerPending, headerTotal);
        String collapsedHeaderText = TermuxSessionsListViewController.projectHeaderTitle(
            "collapsedProject", 0, 2, true);
        String expandedHeaderText = TermuxSessionsListViewController.projectHeaderTitle(
            "expandedProject", headerPending, headerTotal, false);

        assertFalse("collapsed project header must not show a count fraction",
            collapsedHeaderText.contains("("));
        assertTrue("expanded project header must show its count fraction",
            expandedHeaderText.contains("(" + headerPending + "/" + headerTotal + ")"));
        assertTrue("bottom-sheet header total must reflect only expanded sessions",
            headerTitleText.endsWith("(1/1)"));

        TextView headerTitle = inflateHeaderTitle(context, headerTitleText);
        View collapsedRow = inflateProjectHeaderRow(context, collapsedHeaderText, true);
        View expandedRow = inflateProjectHeaderRow(context, expandedHeaderText, false);

        int totalHeight = ROW_HEIGHT * 3 + ROW_GAP * 2;
        Bitmap bitmap = Bitmap.createBitmap(ROW_WIDTH, totalHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(SURFACE_COLOR);
        drawAtRow(canvas, headerTitle, 0);
        drawAtRow(canvas, collapsedRow, ROW_HEIGHT + ROW_GAP);
        drawAtRow(canvas, expandedRow, (ROW_HEIGHT + ROW_GAP) * 2);

        File outDir = appContext.getExternalFilesDir(null);
        File out = new File(outDir, "collapsed-project-count-suppression.png");
        try (FileOutputStream stream = new FileOutputStream(out)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        }
        assertTrue(out.exists() && out.length() > 0);
    }

    private static void drawAtRow(Canvas canvas, View view, int offsetY) {
        canvas.save();
        canvas.translate(0, offsetY);
        view.draw(canvas);
        canvas.restore();
    }
}
