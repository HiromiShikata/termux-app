package com.termux.app.diagnostics;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ScrollbarViewCensusTest {

    private static final class FakeViewNode implements ScrollbarViewCensus.ViewNode {

        private final String mClassName;
        private final boolean mHasScrollbars;
        private final List<ScrollbarViewCensus.ViewNode> mChildren;

        FakeViewNode(String className, boolean hasScrollbars, ScrollbarViewCensus.ViewNode... children) {
            mClassName = className;
            mHasScrollbars = hasScrollbars;
            mChildren = new ArrayList<>(Arrays.asList(children));
        }

        @Override
        public boolean hasScrollbars() {
            return mHasScrollbars;
        }

        @Override
        public String getClassName() {
            return mClassName;
        }

        @Override
        public List<ScrollbarViewCensus.ViewNode> getChildren() {
            return mChildren;
        }
    }

    @Test
    public void countsEveryViewThatCanHoldAScrollbarFadeCallback() {
        ScrollbarViewCensus census = ScrollbarViewCensus.take(
            new FakeViewNode("FrameLayout", false,
                new FakeViewNode("TerminalView", true),
                new FakeViewNode("LinearLayout", false,
                    new FakeViewNode("WebView", true),
                    new FakeViewNode("WebView", true))));

        Assert.assertEquals(3, census.getScrollbarViewCount());
    }

    @Test
    public void ranksTheClassHoldingTheMostSuchViewsFirst() {
        ScrollbarViewCensus census = ScrollbarViewCensus.take(
            new FakeViewNode("FrameLayout", false,
                new FakeViewNode("TerminalView", true),
                new FakeViewNode("WebView", true),
                new FakeViewNode("WebView", true),
                new FakeViewNode("WebView", true)));

        List<ScrollbarViewCensusEntry> busiest = census.getBusiestClasses();
        Assert.assertEquals("WebView", busiest.get(0).getClassName());
        Assert.assertEquals(3, busiest.get(0).getViewCount());
        Assert.assertEquals("TerminalView", busiest.get(1).getClassName());
        Assert.assertEquals(1, busiest.get(1).getViewCount());
    }

    @Test
    public void aTreeWithNoScrollbarViewsCountsNothing() {
        ScrollbarViewCensus census = ScrollbarViewCensus.take(
            new FakeViewNode("FrameLayout", false, new FakeViewNode("TextView", false)));

        Assert.assertEquals(0, census.getScrollbarViewCount());
        Assert.assertTrue(census.getBusiestClasses().isEmpty());
    }

    @Test
    public void aRootThatItselfHoldsAScrollbarIsCounted() {
        ScrollbarViewCensus census = ScrollbarViewCensus.take(new FakeViewNode("ScrollView", true));

        Assert.assertEquals(1, census.getScrollbarViewCount());
        Assert.assertEquals("ScrollView", census.getBusiestClasses().get(0).getClassName());
    }

    @Test
    public void theReportedClassListIsBoundedSoTheReportSurvivesPasting() {
        List<ScrollbarViewCensus.ViewNode> children = new ArrayList<>();
        for (int index = 0; index < ScrollbarViewCensus.MAX_REPORTED_CLASSES + 4; index++) {
            children.add(new FakeViewNode("ViewClass" + index, true));
        }
        ScrollbarViewCensus census = ScrollbarViewCensus.take(
            new FakeViewNode("FrameLayout", false,
                children.toArray(new ScrollbarViewCensus.ViewNode[0])));

        Assert.assertEquals(ScrollbarViewCensus.MAX_REPORTED_CLASSES + 4, census.getScrollbarViewCount());
        Assert.assertEquals(ScrollbarViewCensus.MAX_REPORTED_CLASSES, census.getBusiestClasses().size());
    }
}
