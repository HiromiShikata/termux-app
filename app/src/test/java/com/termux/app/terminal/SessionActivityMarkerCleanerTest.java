package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class SessionActivityMarkerCleanerTest {

    @Test
    public void clearsBellNotificationAndOutputActivityForViewedSession() {
        SessionBellNotificationStore bellNotificationStore = new SessionBellNotificationStore();
        SessionOutputActivityStore outputActivityStore = new SessionOutputActivityStore();
        bellNotificationStore.recordBell("viewed-handle", 1_000L);
        outputActivityStore.markOutputActivity("viewed-handle");

        boolean cleared = SessionActivityMarkerCleaner.clearActivityMarkers(
            bellNotificationStore, outputActivityStore, "viewed-handle");

        Assert.assertTrue(cleared);
        Assert.assertFalse(bellNotificationStore.hasPendingNotification("viewed-handle"));
        Assert.assertFalse(outputActivityStore.hasOutputActivity("viewed-handle"));
    }

    @Test
    public void clearsBellNotificationEvenWhenNoOutputActivityPresent() {
        SessionBellNotificationStore bellNotificationStore = new SessionBellNotificationStore();
        SessionOutputActivityStore outputActivityStore = new SessionOutputActivityStore();
        bellNotificationStore.recordBell("bell-only-handle", 2_000L);

        boolean cleared = SessionActivityMarkerCleaner.clearActivityMarkers(
            bellNotificationStore, outputActivityStore, "bell-only-handle");

        Assert.assertTrue(cleared);
        Assert.assertFalse(bellNotificationStore.hasPendingNotification("bell-only-handle"));
    }

    @Test
    public void purgesMarkersForRemovedSessionHandle() {
        SessionBellNotificationStore bellNotificationStore = new SessionBellNotificationStore();
        SessionOutputActivityStore outputActivityStore = new SessionOutputActivityStore();
        bellNotificationStore.recordBell("removed-handle", 3_000L);
        outputActivityStore.markOutputActivity("removed-handle");
        bellNotificationStore.recordBell("surviving-handle", 4_000L);

        SessionActivityMarkerCleaner.clearActivityMarkers(
            bellNotificationStore, outputActivityStore, "removed-handle");

        Assert.assertFalse(bellNotificationStore.hasPendingNotification("removed-handle"));
        Assert.assertFalse(outputActivityStore.hasOutputActivity("removed-handle"));
        Assert.assertTrue(bellNotificationStore.hasPendingNotification("surviving-handle"));
    }

    @Test
    public void reportsNothingClearedWhenSessionHasNoMarkers() {
        SessionBellNotificationStore bellNotificationStore = new SessionBellNotificationStore();
        SessionOutputActivityStore outputActivityStore = new SessionOutputActivityStore();

        boolean cleared = SessionActivityMarkerCleaner.clearActivityMarkers(
            bellNotificationStore, outputActivityStore, "unmarked-handle");

        Assert.assertFalse(cleared);
    }

    @Test
    public void reportsNothingClearedForNullHandleAndLeavesStoresUntouched() {
        SessionBellNotificationStore bellNotificationStore = new SessionBellNotificationStore();
        SessionOutputActivityStore outputActivityStore = new SessionOutputActivityStore();
        bellNotificationStore.recordBell("other-handle", 5_000L);

        boolean cleared = SessionActivityMarkerCleaner.clearActivityMarkers(
            bellNotificationStore, outputActivityStore, null);

        Assert.assertFalse(cleared);
        Assert.assertTrue(bellNotificationStore.hasPendingNotification("other-handle"));
    }

    @Test
    public void clearedMarkerStaysClearedWhenSessionHasNoNewBackgroundOutput() {
        SessionBellNotificationStore bellNotificationStore = new SessionBellNotificationStore();
        SessionOutputActivityStore outputActivityStore = new SessionOutputActivityStore();
        bellNotificationStore.recordBell("viewed-handle", 6_000L);
        outputActivityStore.markOutputActivity("viewed-handle");

        SessionActivityMarkerCleaner.clearActivityMarkers(
            bellNotificationStore, outputActivityStore, "viewed-handle");

        Assert.assertFalse(TermuxSessionsListViewController.hasNewActivityIndicator(
            bellNotificationStore.getBellArrivalTimeMillis("viewed-handle"),
            outputActivityStore, "viewed-handle"));
    }

    @Test
    public void clearingViewedSessionMarkerLiftsNavigationRestrictionToOtherSessions() {
        SessionBellNotificationStore bellNotificationStore = new SessionBellNotificationStore();
        SessionOutputActivityStore outputActivityStore = new SessionOutputActivityStore();
        List<String> handlesByIndex = Arrays.asList("handle-0", "handle-1", "handle-2");
        bellNotificationStore.recordBell("handle-1", 7_000L);

        List<Integer> navigableSessionIndexes = Arrays.asList(0, 1, 2);
        Assert.assertEquals(Arrays.asList(1),
            BellMarkedSessionNavigationFilter.bellRestrictedNavigableIndexes(
                navigableSessionIndexes, markedSessionIndexes(handlesByIndex, bellNotificationStore, outputActivityStore)));

        SessionActivityMarkerCleaner.clearActivityMarkers(
            bellNotificationStore, outputActivityStore, "handle-1");

        Assert.assertEquals(Arrays.asList(0, 1, 2),
            BellMarkedSessionNavigationFilter.bellRestrictedNavigableIndexes(
                navigableSessionIndexes, markedSessionIndexes(handlesByIndex, bellNotificationStore, outputActivityStore)));
    }

    private static Set<Integer> markedSessionIndexes(List<String> handlesByIndex,
                                                     SessionBellNotificationStore bellNotificationStore,
                                                     SessionOutputActivityStore outputActivityStore) {
        Set<Integer> markedSessionIndexes = new LinkedHashSet<>();
        for (int sessionIndex = 0; sessionIndex < handlesByIndex.size(); sessionIndex++) {
            String sessionHandle = handlesByIndex.get(sessionIndex);
            if (TermuxSessionsListViewController.hasNewActivityIndicator(
                bellNotificationStore.getBellArrivalTimeMillis(sessionHandle), outputActivityStore, sessionHandle)) {
                markedSessionIndexes.add(sessionIndex);
            }
        }
        return markedSessionIndexes;
    }
}
