package com.termux.app.diagnostics;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ScrollbarViewCensus {

    public static final int MAX_REPORTED_CLASSES = 8;

    public interface ViewNode {

        boolean hasScrollbars();

        @NonNull
        String getClassName();

        @NonNull
        List<ViewNode> getChildren();
    }

    private final int mScrollbarViewCount;

    @NonNull
    private final List<ScrollbarViewCensusEntry> mBusiestClasses;

    private ScrollbarViewCensus(int scrollbarViewCount, @NonNull List<ScrollbarViewCensusEntry> busiestClasses) {
        mScrollbarViewCount = scrollbarViewCount;
        mBusiestClasses = Collections.unmodifiableList(new ArrayList<>(busiestClasses));
    }

    @NonNull
    public static ScrollbarViewCensus empty() {
        return new ScrollbarViewCensus(0, new ArrayList<>());
    }

    @NonNull
    public static ScrollbarViewCensus take(@NonNull ViewNode root) {
        Map<String, Integer> countsByClassName = new LinkedHashMap<>();
        int scrollbarViewCount = count(root, countsByClassName);
        return new ScrollbarViewCensus(scrollbarViewCount, busiestClasses(countsByClassName));
    }

    public int getScrollbarViewCount() {
        return mScrollbarViewCount;
    }

    @NonNull
    public List<ScrollbarViewCensusEntry> getBusiestClasses() {
        return mBusiestClasses;
    }

    private static int count(@NonNull ViewNode node, @NonNull Map<String, Integer> countsByClassName) {
        int scrollbarViewCount = 0;
        if (node.hasScrollbars()) {
            scrollbarViewCount++;
            String className = node.getClassName();
            Integer previousCount = countsByClassName.get(className);
            countsByClassName.put(className, previousCount == null ? 1 : previousCount + 1);
        }
        for (ViewNode child : node.getChildren()) {
            scrollbarViewCount += count(child, countsByClassName);
        }
        return scrollbarViewCount;
    }

    @NonNull
    private static List<ScrollbarViewCensusEntry> busiestClasses(@NonNull Map<String, Integer> countsByClassName) {
        List<ScrollbarViewCensusEntry> entries = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : countsByClassName.entrySet()) {
            entries.add(new ScrollbarViewCensusEntry(entry.getKey(), entry.getValue()));
        }
        Collections.sort(entries, (left, right) -> right.getViewCount() - left.getViewCount());
        if (entries.size() > MAX_REPORTED_CLASSES) {
            return new ArrayList<>(entries.subList(0, MAX_REPORTED_CLASSES));
        }
        return entries;
    }
}
