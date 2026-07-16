package com.termux.app.browser;

import androidx.annotation.NonNull;

public final class BrowserFindInPageController {

    public interface FindTarget {
        void findAll(@NonNull String query);

        void findNext(boolean forward);

        void clearMatches();
    }

    public interface View {
        void showFindBar();

        void hideFindBar();

        void focusQueryInputAndShowKeyboard();

        void updateMatchCounter(@NonNull String counterText);
    }

    public interface Debouncer {
        void schedule(@NonNull Runnable task);

        void cancel();
    }

    private final FindTarget mTarget;

    private final View mView;

    private final Debouncer mDebouncer;

    private boolean mOpen;

    @NonNull
    private String mQuery = "";

    public BrowserFindInPageController(
        @NonNull FindTarget target, @NonNull View view, @NonNull Debouncer debouncer) {
        this.mTarget = target;
        this.mView = view;
        this.mDebouncer = debouncer;
    }

    public boolean isOpen() {
        return mOpen;
    }

    @NonNull
    public String getQuery() {
        return mQuery;
    }

    public void open() {
        mOpen = true;
        mQuery = "";
        mView.showFindBar();
        mView.updateMatchCounter("");
        mView.focusQueryInputAndShowKeyboard();
    }

    public void onQueryChanged(@NonNull String query) {
        if (!mOpen) return;
        mQuery = query;
        mDebouncer.cancel();
        if (query.trim().isEmpty()) {
            mTarget.clearMatches();
            mView.updateMatchCounter("");
            return;
        }
        mDebouncer.schedule(() -> mTarget.findAll(mQuery));
    }

    public void submitQuery() {
        if (!mOpen) return;
        mDebouncer.cancel();
        if (mQuery.trim().isEmpty()) {
            mTarget.clearMatches();
            mView.updateMatchCounter("");
            return;
        }
        mTarget.findAll(mQuery);
    }

    public void findNext() {
        if (!mOpen || mQuery.trim().isEmpty()) return;
        mTarget.findNext(true);
    }

    public void findPrevious() {
        if (!mOpen || mQuery.trim().isEmpty()) return;
        mTarget.findNext(false);
    }

    public void onFindResultReceived(int activeMatchOrdinal, int numberOfMatches, boolean isDoneCounting) {
        if (!mOpen || !isDoneCounting) return;
        BrowserFindMatchCounter counter =
            BrowserFindMatchCounter.fromListenerResult(activeMatchOrdinal, numberOfMatches);
        mView.updateMatchCounter(counter.formatForQuery(mQuery));
    }

    public void close() {
        if (!mOpen) return;
        reset();
        mView.hideFindBar();
    }

    public void onPageOrTabChanged() {
        if (!mOpen) return;
        reset();
        mView.hideFindBar();
    }

    private void reset() {
        mDebouncer.cancel();
        mTarget.clearMatches();
        mView.updateMatchCounter("");
        mQuery = "";
        mOpen = false;
    }
}
