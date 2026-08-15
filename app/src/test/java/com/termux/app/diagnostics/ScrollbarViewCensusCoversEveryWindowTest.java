package com.termux.app.diagnostics;

import androidx.annotation.NonNull;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ScrollbarViewCensusCoversEveryWindowTest {

    private static final class CensusNode implements ScrollbarViewCensus.ViewNode {

        private final String mClassName;

        private final boolean mCanHoldScrollbarFadeCallback;

        private final List<ScrollbarViewCensus.ViewNode> mChildren;

        private CensusNode(String className, boolean canHoldScrollbarFadeCallback,
                           ScrollbarViewCensus.ViewNode... children) {
            mClassName = className;
            mCanHoldScrollbarFadeCallback = canHoldScrollbarFadeCallback;
            mChildren = new ArrayList<>(Arrays.asList(children));
        }

        @Override
        public boolean canHoldScrollbarFadeCallback() {
            return mCanHoldScrollbarFadeCallback;
        }

        @Override
        @NonNull
        public String getClassName() {
            return mClassName;
        }

        @Override
        @NonNull
        public List<ScrollbarViewCensus.ViewNode> getChildren() {
            return mChildren;
        }
    }

    private static ScrollbarViewCensus.ViewNode windowHolding(String... classNames) {
        List<ScrollbarViewCensus.ViewNode> children = new ArrayList<>();
        for (String className : classNames) {
            children.add(new CensusNode(className, true));
        }
        return new CensusNode("android.widget.FrameLayout", false,
            children.toArray(new ScrollbarViewCensus.ViewNode[0]));
    }

    @Test
    public void theViewsOfEveryWindowAreCountedTogetherRatherThanOnlyTheFirst() {
        ScrollbarViewCensus census = ScrollbarViewCensus.take(Arrays.asList(
            windowHolding("com.termux.view.TerminalView", "android.widget.ListView"),
            windowHolding("com.termux.view.TerminalView"),
            windowHolding("android.webkit.WebView")), 0);

        Assert.assertEquals("a fade callback is posted to the one main looper by every view in the"
                + " process, so a count taken from one window answers a different question from the one"
                + " the pending callback lines raise",
            4, census.getScrollbarViewCount());
    }

    @Test
    public void theBusiestClassesAreNamedAcrossTheWindowsTogether() {
        ScrollbarViewCensus census = ScrollbarViewCensus.take(Arrays.asList(
            windowHolding("com.termux.view.TerminalView", "android.widget.ListView"),
            windowHolding("com.termux.view.TerminalView"),
            windowHolding("com.termux.view.TerminalView")), 0);

        Assert.assertFalse("a reader cannot attribute callbacks to a class the reading does not name",
            census.getBusiestClasses().isEmpty());
        ScrollbarViewCensusEntry busiest = census.getBusiestClasses().get(0);
        Assert.assertEquals("com.termux.view.TerminalView", busiest.getClassName());
        Assert.assertEquals("a class holding one view in each of three windows holds three views in the"
                + " process, and a per-window count would report one", 3, busiest.getViewCount());
    }

    @Test
    public void theNumberOfWindowsTheCountCoversIsCarriedWithIt() {
        ScrollbarViewCensus census = ScrollbarViewCensus.take(Arrays.asList(
            windowHolding("com.termux.view.TerminalView"),
            windowHolding("com.termux.view.TerminalView")), 0);

        Assert.assertEquals("a total with no window count behind it cannot be told apart from the"
            + " single-window total that could not explain the pending callbacks", 2, census.getWindowCount());
    }

    @Test
    public void aWindowWhoseViewsCanNoLongerBeReachedIsCountedRatherThanDroppedSilently() {
        ScrollbarViewCensus census = ScrollbarViewCensus.take(Collections.singletonList(
            windowHolding("com.termux.view.TerminalView")), 4);

        Assert.assertEquals("the windows that were released are exactly the ones whose views the count"
                + " cannot see, so leaving them out unstated reads as a complete count",
            4, census.getWindowsNoLongerReachableCount());
        Assert.assertEquals(1, census.getWindowCount());
    }

    @Test
    public void aCensusTakenBeforeAnyWindowExistedReportsZeroWindowsRatherThanNothing() {
        ScrollbarViewCensus census = ScrollbarViewCensus.empty();

        Assert.assertEquals(0, census.getWindowCount());
        Assert.assertEquals(0, census.getWindowsNoLongerReachableCount());
        Assert.assertEquals(0, census.getScrollbarViewCount());
    }
}
