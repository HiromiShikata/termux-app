package com.termux.app.diagnostics;

import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class AndroidScrollbarViewNode implements ScrollbarViewCensus.ViewNode {

    @NonNull
    private final View mView;

    public AndroidScrollbarViewNode(@NonNull View view) {
        mView = view;
    }

    @Override
    public boolean canHoldScrollbarFadeCallback() {
        if (!mView.isScrollbarFadingEnabled()) return false;
        return mView.isVerticalScrollBarEnabled() || mView.isHorizontalScrollBarEnabled();
    }

    @Override
    @NonNull
    public String getClassName() {
        return mView.getClass().getSimpleName();
    }

    @Override
    @NonNull
    public List<ScrollbarViewCensus.ViewNode> getChildren() {
        if (!(mView instanceof ViewGroup)) return Collections.emptyList();
        ViewGroup group = (ViewGroup) mView;
        List<ScrollbarViewCensus.ViewNode> children = new ArrayList<>();
        for (int index = 0; index < group.getChildCount(); index++) {
            View child = group.getChildAt(index);
            if (child != null) children.add(new AndroidScrollbarViewNode(child));
        }
        return children;
    }
}
