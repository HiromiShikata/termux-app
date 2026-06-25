package com.termux.app.terminal;

import androidx.annotation.NonNull;

import java.util.List;

public final class CallToUserTagScrollLocator {

    public static final int NO_TAG_ROW = Integer.MIN_VALUE;

    private static final String OPEN_TAG = "<call-to-user>";

    private CallToUserTagScrollLocator() {
    }

    public static int mostRecentOpenTagExternalRow(@NonNull List<String> rowTexts,
                                                   int firstRowExternalIndex) {
        StringBuilder joined = new StringBuilder();
        int[] rowIndexByChar = charOffsetToRowIndex(rowTexts, joined);
        int openTagCharOffset = joined.lastIndexOf(OPEN_TAG);
        if (openTagCharOffset < 0) {
            return NO_TAG_ROW;
        }
        return firstRowExternalIndex + rowIndexByChar[openTagCharOffset];
    }

    private static int[] charOffsetToRowIndex(@NonNull List<String> rowTexts,
                                              @NonNull StringBuilder joined) {
        int totalLength = 0;
        for (String rowText : rowTexts) {
            totalLength += rowText.length();
        }
        int[] rowIndexByChar = new int[totalLength];
        int writtenChars = 0;
        for (int rowIndex = 0; rowIndex < rowTexts.size(); rowIndex++) {
            String rowText = rowTexts.get(rowIndex);
            for (int charIndex = 0; charIndex < rowText.length(); charIndex++) {
                rowIndexByChar[writtenChars + charIndex] = rowIndex;
            }
            joined.append(rowText);
            writtenChars += rowText.length();
        }
        return rowIndexByChar;
    }

    public static int clampTopRow(int targetExternalRow, int activeTranscriptRows) {
        if (targetExternalRow > 0) {
            return 0;
        }
        if (targetExternalRow < -activeTranscriptRows) {
            return -activeTranscriptRows;
        }
        return targetExternalRow;
    }

    public static int scrollTargetTopRow(@NonNull List<String> rowTexts,
                                         int firstRowExternalIndex,
                                         int activeTranscriptRows) {
        int targetExternalRow = mostRecentOpenTagExternalRow(rowTexts, firstRowExternalIndex);
        if (targetExternalRow == NO_TAG_ROW) {
            return NO_TAG_ROW;
        }
        return clampTopRow(targetExternalRow, activeTranscriptRows);
    }
}
