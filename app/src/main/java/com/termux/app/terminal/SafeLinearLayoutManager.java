package com.termux.app.terminal;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.termux.shared.logger.Logger;

public class SafeLinearLayoutManager extends LinearLayoutManager {

    private static final String LOG_TAG = "SafeLinearLayoutManager";

    public SafeLinearLayoutManager(@NonNull Context context) {
        super(context);
        setItemPrefetchEnabled(false);
    }

    @Override
    public void onLayoutChildren(@NonNull RecyclerView.Recycler recycler, @NonNull RecyclerView.State state) {
        layoutChildrenSwallowingInconsistency(() -> super.onLayoutChildren(recycler, state));
    }

    static void layoutChildrenSwallowingInconsistency(@NonNull Runnable layoutPass) {
        try {
            layoutPass.run();
        } catch (IndexOutOfBoundsException exception) {
            Logger.logStackTraceWithMessage(LOG_TAG,
                "Recovered from a RecyclerView layout inconsistency; the next layout pass will rebuild the rows",
                exception);
        }
    }
}
