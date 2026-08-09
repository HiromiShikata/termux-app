package com.termux.app.copytag;

import java.util.ArrayList;
import java.util.List;

public final class CopyTagBlocksOnScreen {

    private static final String OPENING_TAG = "<copy>";

    private static final String CLOSING_TAG = "</copy>";

    private final List<ScreenRow> rows;

    private final int firstRowNumber;

    public CopyTagBlocksOnScreen(List<ScreenRow> rows, int firstRowNumber) {
        this.rows = rows == null ? new ArrayList<>() : new ArrayList<>(rows);
        this.firstRowNumber = firstRowNumber;
    }

    public String contentOfTheBlockCoveringRow(int rowNumber) {
        int index = rowNumber - firstRowNumber;
        if (index < 0 || index >= rows.size()) return null;

        int openingIndex = openingIndexAtOrAbove(index);
        if (openingIndex < 0) return null;

        int closingIndex = closingIndexAtOrBelow(openingIndex);
        if (closingIndex < 0 || closingIndex < index) return null;

        return joinedContentBetween(openingIndex, closingIndex);
    }

    private int openingIndexAtOrAbove(int index) {
        for (int i = index; i >= 0; i--) {
            String text = rows.get(i).text;
            if (text.contains(OPENING_TAG)) return i;
            if (i < index && text.contains(CLOSING_TAG)) return -1;
        }
        return -1;
    }

    private int closingIndexAtOrBelow(int openingIndex) {
        for (int i = openingIndex; i < rows.size(); i++) {
            String text = rows.get(i).text;
            int searchFrom = i == openingIndex ? text.indexOf(OPENING_TAG) + OPENING_TAG.length() : 0;
            if (text.indexOf(CLOSING_TAG, searchFrom) >= 0) return i;
        }
        return -1;
    }

    private String joinedContentBetween(int openingIndex, int closingIndex) {
        StringBuilder joined = new StringBuilder();
        for (int i = openingIndex; i <= closingIndex; i++) {
            ScreenRow row = rows.get(i);
            joined.append(contentOfRow(row.text, i == openingIndex, i == closingIndex));
            if (i < closingIndex && !row.continuesOnTheNextRow) joined.append('\n');
        }
        return trimmedOfSurroundingNewlines(joined.toString());
    }

    private String contentOfRow(String text, boolean holdsTheOpeningTag, boolean holdsTheClosingTag) {
        int from = 0;
        if (holdsTheOpeningTag) from = text.indexOf(OPENING_TAG) + OPENING_TAG.length();
        int to = text.length();
        if (holdsTheClosingTag) {
            int closingAt = text.indexOf(CLOSING_TAG, from);
            if (closingAt >= 0) to = closingAt;
        }
        return from <= to ? text.substring(from, to) : "";
    }

    private String trimmedOfSurroundingNewlines(String content) {
        int start = 0;
        int end = content.length();
        while (start < end && isNewline(content.charAt(start))) start++;
        while (end > start && isNewline(content.charAt(end - 1))) end--;
        return content.substring(start, end);
    }

    private boolean isNewline(char character) {
        return character == '\n' || character == '\r';
    }
}
