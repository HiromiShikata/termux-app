package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class HideHiddenSessionsToggleRebuildsRowsTest {

    private final SessionHierarchyBuilder builder = new SessionHierarchyBuilder();

    private static final String NA = "N/A";

    private static final class HideHiddenSessionsRowSource {

        private final SessionHierarchyBuilder hierarchyBuilder;
        private final List<String> sessionNames;
        private final Set<String> hiddenSessionNames;

        private boolean hideHiddenSessions;
        private List<SessionHierarchyRow> renderedRows;
        private int shownSessionCount;

        HideHiddenSessionsRowSource(SessionHierarchyBuilder hierarchyBuilder,
                                    List<String> sessionNames,
                                    Set<String> hiddenSessionNames) {
            this.hierarchyBuilder = hierarchyBuilder;
            this.sessionNames = sessionNames;
            this.hiddenSessionNames = hiddenSessionNames;
            rebuildRows();
        }

        boolean toggleHideHiddenSessions() {
            hideHiddenSessions = !hideHiddenSessions;
            refreshSessionList();
            return hideHiddenSessions;
        }

        private void refreshSessionList() {
            rebuildRows();
        }

        private void rebuildRows() {
            List<SessionHierarchyRow> allRows =
                hierarchyBuilder.build(sessionNames, Collections.emptyList(), NA);
            Set<String> effectiveHiddenSessionNames =
                hideHiddenSessions ? hiddenSessionNames : Collections.emptySet();
            renderedRows = SessionHierarchyBuilder.filterHiddenSessions(
                allRows, sessionNames, effectiveHiddenSessionNames);
            shownSessionCount = SessionHierarchyBuilder.shownSessionCount(
                allRows, sessionNames, effectiveHiddenSessionNames);
        }

        int renderedSessionCount() {
            return SessionHierarchyBuilder.totalSessionCount(renderedRows);
        }

        int shownSessionCount() {
            return shownSessionCount;
        }
    }

    @Test
    public void togglingHideOnRemovesHiddenRowsFromTheRenderedListSoTheRowSetChanges() {
        List<String> sessionNames = Arrays.asList("alpha", "beta", "gamma");
        Set<String> hiddenSessionNames = new HashSet<>(Collections.singletonList("beta"));
        HideHiddenSessionsRowSource rowSource =
            new HideHiddenSessionsRowSource(builder, sessionNames, hiddenSessionNames);

        Assert.assertEquals(3, rowSource.renderedSessionCount());

        rowSource.toggleHideHiddenSessions();

        Assert.assertEquals("toggling hide on must rebuild the rows so the hidden session row is dropped, "
                + "not merely re-bind the same rows",
            2, rowSource.renderedSessionCount());
    }

    @Test
    public void togglingHideOnRecomputesTheShownSessionCountSoTheHeaderFractionIsNotStale() {
        List<String> sessionNames = Arrays.asList("alpha", "beta", "gamma");
        Set<String> hiddenSessionNames = new HashSet<>(Collections.singletonList("beta"));
        HideHiddenSessionsRowSource rowSource =
            new HideHiddenSessionsRowSource(builder, sessionNames, hiddenSessionNames);

        Assert.assertEquals(3, rowSource.shownSessionCount());

        rowSource.toggleHideHiddenSessions();

        Assert.assertEquals("toggling hide on must rebuild the rows so the shown-session count is recomputed, "
                + "not left stale at the total",
            2, rowSource.shownSessionCount());
    }

    @Test
    public void togglingHideOffRestoresTheFullRowSetAndShownCount() {
        List<String> sessionNames = Arrays.asList("alpha", "beta", "gamma");
        Set<String> hiddenSessionNames = new HashSet<>(Collections.singletonList("beta"));
        HideHiddenSessionsRowSource rowSource =
            new HideHiddenSessionsRowSource(builder, sessionNames, hiddenSessionNames);

        rowSource.toggleHideHiddenSessions();
        rowSource.toggleHideHiddenSessions();

        Assert.assertEquals(3, rowSource.renderedSessionCount());
        Assert.assertEquals(3, rowSource.shownSessionCount());
    }
}
