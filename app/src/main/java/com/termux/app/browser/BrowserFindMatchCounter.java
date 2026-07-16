package com.termux.app.browser;

import androidx.annotation.NonNull;

public final class BrowserFindMatchCounter {

    public static final BrowserFindMatchCounter EMPTY = new BrowserFindMatchCounter(0, 0);

    private final int activeMatchOrdinal;

    private final int numberOfMatches;

    public BrowserFindMatchCounter(int activeMatchOrdinal, int numberOfMatches) {
        this.activeMatchOrdinal = Math.max(activeMatchOrdinal, 0);
        this.numberOfMatches = Math.max(numberOfMatches, 0);
    }

    @NonNull
    public static BrowserFindMatchCounter fromListenerResult(int activeMatchOrdinal, int numberOfMatches) {
        if (numberOfMatches <= 0) return EMPTY;
        return new BrowserFindMatchCounter(activeMatchOrdinal + 1, numberOfMatches);
    }

    public int getActiveMatchOrdinal() {
        return activeMatchOrdinal;
    }

    public int getNumberOfMatches() {
        return numberOfMatches;
    }

    public boolean hasMatches() {
        return numberOfMatches > 0;
    }

    @NonNull
    public String formatForQuery(@NonNull String query) {
        if (query.trim().isEmpty()) return "";
        if (numberOfMatches <= 0) return "0/0";
        return activeMatchOrdinal + "/" + numberOfMatches;
    }
}
