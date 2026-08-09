package com.termux.app.copytag;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class CopyTagBlocksOnScreen {

    private static final String OPENING_TAG = "<copy>";

    private static final String CLOSING_TAG = "</copy>";

    private final ScreenRows rows;

    private final int topmostRowNumber;

    private final int bottommostRowNumber;

    private final Map<Integer, ScreenRow> alreadyRead = new HashMap<>();

    public CopyTagBlocksOnScreen(ScreenRows rows, int topmostRowNumber, int bottommostRowNumber) {
        this.rows = rows;
        this.topmostRowNumber = topmostRowNumber;
        this.bottommostRowNumber = bottommostRowNumber;
    }

    public static CopyTagBlocksOnScreen of(List<ScreenRow> rows, int firstRowNumber) {
        List<ScreenRow> copied = rows == null ? new ArrayList<>() : new ArrayList<>(rows);
        ScreenRows readable = rowNumber -> {
            int index = rowNumber - firstRowNumber;
            return index < 0 || index >= copied.size() ? null : copied.get(index);
        };
        return new CopyTagBlocksOnScreen(readable, firstRowNumber, firstRowNumber + copied.size() - 1);
    }

    public String contentOfTheBlockCoveringRow(int rowNumber) {
        if (rowNumber < topmostRowNumber || rowNumber > bottommostRowNumber) return null;

        int openingRowNumber = openingRowAtOrAbove(rowNumber);
        if (openingRowNumber == Integer.MIN_VALUE) return null;

        int closingRowNumber = closingRowAtOrBelow(openingRowNumber);
        if (closingRowNumber == Integer.MIN_VALUE || closingRowNumber < rowNumber) return null;

        return joinedContentBetween(openingRowNumber, closingRowNumber);
    }

    private int openingRowAtOrAbove(int rowNumber) {
        for (int row = rowNumber; row >= topmostRowNumber; row--) {
            String text = textAt(row);
            if (text.contains(OPENING_TAG)) return row;
            if (row < rowNumber && text.contains(CLOSING_TAG)) return Integer.MIN_VALUE;
        }
        return Integer.MIN_VALUE;
    }

    private int closingRowAtOrBelow(int openingRowNumber) {
        for (int row = openingRowNumber; row <= bottommostRowNumber; row++) {
            String text = textAt(row);
            int searchFrom = row == openingRowNumber ? text.indexOf(OPENING_TAG) + OPENING_TAG.length() : 0;
            if (text.indexOf(CLOSING_TAG, searchFrom) >= 0) return row;
        }
        return Integer.MIN_VALUE;
    }

    private String joinedContentBetween(int openingRowNumber, int closingRowNumber) {
        StringBuilder joined = new StringBuilder();
        for (int row = openingRowNumber; row <= closingRowNumber; row++) {
            joined.append(contentOfRow(textAt(row), row == openingRowNumber, row == closingRowNumber));
            if (row < closingRowNumber && !wrapsOntoTheNextRow(row)) joined.append('\n');
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

    private String textAt(int rowNumber) {
        ScreenRow row = readRow(rowNumber);
        return row == null ? "" : row.text;
    }

    private boolean wrapsOntoTheNextRow(int rowNumber) {
        ScreenRow row = readRow(rowNumber);
        return row != null && row.continuesOnTheNextRow;
    }

    private ScreenRow readRow(int rowNumber) {
        if (alreadyRead.containsKey(rowNumber)) return alreadyRead.get(rowNumber);
        ScreenRow row = rows == null ? null : rows.rowAt(rowNumber);
        alreadyRead.put(rowNumber, row);
        return row;
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
