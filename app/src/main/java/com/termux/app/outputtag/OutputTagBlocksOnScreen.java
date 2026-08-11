package com.termux.app.outputtag;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class OutputTagBlocksOnScreen {

    private final String openingTag;

    private final String closingTag;

    private final ScreenRows rows;

    private final int topmostRowNumber;

    private final int bottommostRowNumber;

    private final Map<Integer, ScreenRow> alreadyRead = new HashMap<>();

    public OutputTagBlocksOnScreen(String tagName, ScreenRows rows, int topmostRowNumber, int bottommostRowNumber) {
        this.openingTag = "<" + tagName + ">";
        this.closingTag = "</" + tagName + ">";
        this.rows = rows;
        this.topmostRowNumber = topmostRowNumber;
        this.bottommostRowNumber = bottommostRowNumber;
    }

    public static OutputTagBlocksOnScreen of(String tagName, List<ScreenRow> rows, int firstRowNumber) {
        List<ScreenRow> copied = rows == null ? new ArrayList<>() : new ArrayList<>(rows);
        ScreenRows readable = rowNumber -> {
            int index = rowNumber - firstRowNumber;
            return index < 0 || index >= copied.size() ? null : copied.get(index);
        };
        return new OutputTagBlocksOnScreen(tagName, readable, firstRowNumber, firstRowNumber + copied.size() - 1);
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
            if (text.contains(openingTag)) return row;
            if (row < rowNumber && text.contains(closingTag)) return Integer.MIN_VALUE;
        }
        return Integer.MIN_VALUE;
    }

    private int closingRowAtOrBelow(int openingRowNumber) {
        for (int row = openingRowNumber; row <= bottommostRowNumber; row++) {
            String text = textAt(row);
            int searchFrom = row == openingRowNumber ? text.indexOf(openingTag) + openingTag.length() : 0;
            if (text.indexOf(closingTag, searchFrom) >= 0) return row;
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
        if (holdsTheOpeningTag) from = text.indexOf(openingTag) + openingTag.length();
        int to = text.length();
        if (holdsTheClosingTag) {
            int closingAt = text.indexOf(closingTag, from);
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
