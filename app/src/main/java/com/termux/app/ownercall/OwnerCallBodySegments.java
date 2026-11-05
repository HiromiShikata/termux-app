package com.termux.app.ownercall;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

public final class OwnerCallBodySegments {

    private static final Pattern DELIMITER_CELL = Pattern.compile(":?-+:?");

    private static final class Line {

        private final int mStartIndex;
        private final int mEndIndex;

        Line(int startIndex, int endIndex) {
            mStartIndex = startIndex;
            mEndIndex = endIndex;
        }
    }

    private static final class ParsedTable {

        private final OwnerCallBodyTable mTable;
        private final int mLastLineIndex;

        ParsedTable(OwnerCallBodyTable table, int lastLineIndex) {
            mTable = table;
            mLastLineIndex = lastLineIndex;
        }
    }

    private OwnerCallBodySegments() {
    }

    @NonNull
    public static List<OwnerCallBodySegment> of(@NonNull OwnerCallBodyDisplayText displayText) {
        String text = displayText.getText();
        List<Line> lines = linesOf(text);
        List<OwnerCallBodySegment> segments = new ArrayList<>();
        int textStart = 0;
        boolean foundTable = false;
        int lineIndex = 0;
        while (lineIndex < lines.size()) {
            ParsedTable parsed = lineIndex + 1 < lines.size()
                ? tableStartingAt(text, lines, lineIndex) : null;
            if (parsed == null) {
                lineIndex++;
                continue;
            }
            foundTable = true;
            Line first = lines.get(lineIndex);
            Line last = lines.get(parsed.mLastLineIndex);
            int textEnd = first.mStartIndex;
            while (textEnd > textStart && text.charAt(textEnd - 1) == '\n') {
                textEnd--;
            }
            addTextSegment(segments, textStart, textEnd);
            segments.add(OwnerCallBodySegment.table(first.mStartIndex, last.mEndIndex,
                parsed.mTable));
            textStart = last.mEndIndex;
            while (textStart < text.length() && text.charAt(textStart) == '\n') {
                textStart++;
            }
            lineIndex = parsed.mLastLineIndex + 1;
        }
        if (!foundTable) {
            return Collections.singletonList(OwnerCallBodySegment.text(0, text.length()));
        }
        addTextSegment(segments, textStart, text.length());
        return segments;
    }

    private static void addTextSegment(@NonNull List<OwnerCallBodySegment> segments,
                                       int startIndex, int endIndex) {
        if (endIndex > startIndex) {
            segments.add(OwnerCallBodySegment.text(startIndex, endIndex));
        }
    }

    @NonNull
    private static List<Line> linesOf(@NonNull String text) {
        List<Line> lines = new ArrayList<>();
        int start = 0;
        while (true) {
            int end = text.indexOf('\n', start);
            if (end < 0) {
                lines.add(new Line(start, text.length()));
                return lines;
            }
            lines.add(new Line(start, end));
            start = end + 1;
        }
    }

    @Nullable
    private static ParsedTable tableStartingAt(@NonNull String text,
                                               @NonNull List<Line> lines, int headerIndex) {
        Line header = lines.get(headerIndex);
        Line delimiter = lines.get(headerIndex + 1);
        if (!containsPipe(text, header) || !containsPipe(text, delimiter)) {
            return null;
        }
        List<OwnerCallBodyRange> headerCells = cellsOf(text, header);
        List<OwnerCallBodyRange> delimiterCells = cellsOf(text, delimiter);
        if (headerCells.size() != delimiterCells.size()) {
            return null;
        }
        List<OwnerCallBodyTableAlignment> alignments = new ArrayList<>();
        for (OwnerCallBodyRange delimiterCell : delimiterCells) {
            if (!DELIMITER_CELL.matcher(delimiterCell.getText()).matches()) {
                return null;
            }
            alignments.add(alignmentOf(delimiterCell.getText()));
        }
        List<List<OwnerCallBodyRange>> rows = new ArrayList<>();
        int lastLineIndex = headerIndex + 1;
        for (int index = headerIndex + 2; index < lines.size(); index++) {
            Line line = lines.get(index);
            if (!containsPipe(text, line)) {
                break;
            }
            rows.add(paddedRow(text, line, headerCells.size()));
            lastLineIndex = index;
        }
        return new ParsedTable(new OwnerCallBodyTable(headerCells, alignments, rows), lastLineIndex);
    }

    private static boolean containsPipe(@NonNull String text, @NonNull Line line) {
        return text.substring(line.mStartIndex, line.mEndIndex).indexOf('|') >= 0;
    }

    @NonNull
    private static List<OwnerCallBodyRange> paddedRow(@NonNull String text, @NonNull Line line,
                                                      int columnCount) {
        List<OwnerCallBodyRange> cells = new ArrayList<>(cellsOf(text, line));
        while (cells.size() < columnCount) {
            cells.add(new OwnerCallBodyRange(line.mEndIndex, line.mEndIndex, ""));
        }
        return new ArrayList<>(cells.subList(0, columnCount));
    }

    @NonNull
    private static OwnerCallBodyTableAlignment alignmentOf(@NonNull String delimiterCell) {
        boolean startsWithColon = delimiterCell.startsWith(":");
        boolean endsWithColon = delimiterCell.endsWith(":");
        if (startsWithColon && endsWithColon) {
            return OwnerCallBodyTableAlignment.CENTER;
        }
        if (endsWithColon) {
            return OwnerCallBodyTableAlignment.RIGHT;
        }
        return OwnerCallBodyTableAlignment.LEFT;
    }

    @NonNull
    private static List<OwnerCallBodyRange> cellsOf(@NonNull String text, @NonNull Line line) {
        int start = line.mStartIndex;
        int end = line.mEndIndex;
        while (start < end && Character.isWhitespace(text.charAt(start))) {
            start++;
        }
        while (end > start && Character.isWhitespace(text.charAt(end - 1))) {
            end--;
        }
        if (start < end && text.charAt(start) == '|') {
            start++;
        }
        if (end > start && text.charAt(end - 1) == '|' && !isEscapedPipe(text, end - 1, start)) {
            end--;
        }
        List<OwnerCallBodyRange> cells = new ArrayList<>();
        int cellStart = start;
        for (int index = start; index <= end; index++) {
            boolean separator = index < end && text.charAt(index) == '|'
                && !isEscapedPipe(text, index, start);
            if (index == end || separator) {
                cells.add(trimmedRange(text, cellStart, index));
                cellStart = index + 1;
            }
        }
        return cells;
    }

    private static boolean isEscapedPipe(@NonNull String text, int pipeIndex, int lowerBound) {
        return pipeIndex > lowerBound && text.charAt(pipeIndex - 1) == '\\';
    }

    @NonNull
    private static OwnerCallBodyRange trimmedRange(@NonNull String text, int start, int end) {
        int trimmedStart = start;
        int trimmedEnd = end;
        while (trimmedStart < trimmedEnd && Character.isWhitespace(text.charAt(trimmedStart))) {
            trimmedStart++;
        }
        while (trimmedEnd > trimmedStart && Character.isWhitespace(text.charAt(trimmedEnd - 1))) {
            trimmedEnd--;
        }
        return new OwnerCallBodyRange(trimmedStart, trimmedEnd,
            text.substring(trimmedStart, trimmedEnd));
    }
}
