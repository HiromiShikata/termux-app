package com.termux.app.browser;

import androidx.annotation.NonNull;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class BrowserFindInPageControllerTest {

    private FakeFindTarget mTarget;
    private FakeView mView;
    private ImmediateDebouncer mDebouncer;
    private BrowserFindInPageController mController;

    @Before
    public void setUp() {
        mTarget = new FakeFindTarget();
        mView = new FakeView();
        mDebouncer = new ImmediateDebouncer();
        mController = new BrowserFindInPageController(mTarget, mView, mDebouncer);
    }

    @Test
    public void openShowsBarFocusesInputAndClearsCounter() {
        mController.open();

        Assert.assertTrue(mController.isOpen());
        Assert.assertEquals(1, mView.showCount);
        Assert.assertEquals(1, mView.focusCount);
        Assert.assertEquals("", mView.lastCounter);
    }

    @Test
    public void queryChangeRunsFindAllAsyncAfterDebounce() {
        mController.open();

        mController.onQueryChanged("hello");

        Assert.assertEquals(1, mTarget.findAllQueries.size());
        Assert.assertEquals("hello", mTarget.findAllQueries.get(0));
    }

    @Test
    public void blankQueryClearsMatchesInsteadOfSearching() {
        mController.open();

        mController.onQueryChanged("   ");

        Assert.assertTrue(mTarget.findAllQueries.isEmpty());
        Assert.assertEquals(1, mTarget.clearCount);
        Assert.assertEquals("", mView.lastCounter);
    }

    @Test
    public void findNextAndPreviousDelegateDirectionToTarget() {
        mController.open();
        mController.onQueryChanged("term");

        mController.findNext();
        mController.findPrevious();

        Assert.assertEquals(2, mTarget.findNextCalls.size());
        Assert.assertEquals(Boolean.TRUE, mTarget.findNextCalls.get(0));
        Assert.assertEquals(Boolean.FALSE, mTarget.findNextCalls.get(1));
    }

    @Test
    public void findNextIsNoOpWhenQueryBlank() {
        mController.open();

        mController.findNext();

        Assert.assertTrue(mTarget.findNextCalls.isEmpty());
    }

    @Test
    public void listenerResultUpdatesCounterFromOrdinalAndTotal() {
        mController.open();
        mController.onQueryChanged("term");

        mController.onFindResultReceived(2, 12, true);

        Assert.assertEquals("3/12", mView.lastCounter);
    }

    @Test
    public void listenerResultIgnoredWhileStillCounting() {
        mController.open();
        mController.onQueryChanged("term");
        mView.lastCounter = "sentinel";

        mController.onFindResultReceived(0, 5, false);

        Assert.assertEquals("sentinel", mView.lastCounter);
    }

    @Test
    public void closeClearsMatchesHidesBarAndResetsState() {
        mController.open();
        mController.onQueryChanged("term");

        mController.close();

        Assert.assertFalse(mController.isOpen());
        Assert.assertEquals(1, mView.hideCount);
        Assert.assertTrue(mTarget.clearCount >= 1);
        Assert.assertEquals("", mController.getQuery());
    }

    @Test
    public void pageOrTabChangeClearsMatchesAndHidesBarWhenOpen() {
        mController.open();
        mController.onQueryChanged("term");
        int clearsBeforeChange = mTarget.clearCount;

        mController.onPageOrTabChanged();

        Assert.assertFalse(mController.isOpen());
        Assert.assertEquals(1, mView.hideCount);
        Assert.assertEquals(clearsBeforeChange + 1, mTarget.clearCount);
    }

    @Test
    public void pageOrTabChangeIsNoOpWhenClosed() {
        mController.onPageOrTabChanged();

        Assert.assertEquals(0, mView.hideCount);
        Assert.assertEquals(0, mTarget.clearCount);
    }

    @Test
    public void queryChangeIgnoredWhenBarClosed() {
        mController.onQueryChanged("hello");

        Assert.assertTrue(mTarget.findAllQueries.isEmpty());
    }

    private static final class FakeFindTarget implements BrowserFindInPageController.FindTarget {
        final List<String> findAllQueries = new ArrayList<>();
        final List<Boolean> findNextCalls = new ArrayList<>();
        int clearCount;

        @Override
        public void findAll(@NonNull String query) {
            findAllQueries.add(query);
        }

        @Override
        public void findNext(boolean forward) {
            findNextCalls.add(forward);
        }

        @Override
        public void clearMatches() {
            clearCount++;
        }
    }

    private static final class FakeView implements BrowserFindInPageController.View {
        int showCount;
        int hideCount;
        int focusCount;
        String lastCounter = null;

        @Override
        public void showFindBar() {
            showCount++;
        }

        @Override
        public void hideFindBar() {
            hideCount++;
        }

        @Override
        public void focusQueryInputAndShowKeyboard() {
            focusCount++;
        }

        @Override
        public void updateMatchCounter(@NonNull String counterText) {
            lastCounter = counterText;
        }
    }

    private static final class ImmediateDebouncer implements BrowserFindInPageController.Debouncer {
        @Override
        public void schedule(@NonNull Runnable task) {
            task.run();
        }

        @Override
        public void cancel() {
        }
    }
}
