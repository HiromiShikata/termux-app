package com.termux.app.ownercall;

import androidx.annotation.NonNull;

import com.termux.shared.termux.data.TermuxUrlUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class OwnerCallBodyDisplayText {

    private static final String OWNER_CALL_MARKER = "🔴";
    private static final String COPY_MARKER_START = "<copy>";
    private static final String COPY_MARKER_END = "</copy>";
    private static final Pattern QUOTE_MARKER = Pattern.compile(" {0,3}> ?");

    @NonNull
    private final String mText;

    @NonNull
    private final List<OwnerCallBodyRange> mCopyableRanges;

    @NonNull
    private final List<OwnerCallBodyRange> mUrlRanges;

    @NonNull
    private final List<OwnerCallBodyRange> mQuotedLines;

    private OwnerCallBodyDisplayText(@NonNull String text,
                                     @NonNull List<OwnerCallBodyRange> copyableRanges,
                                     @NonNull List<OwnerCallBodyRange> urlRanges,
                                     @NonNull List<OwnerCallBodyRange> quotedLines) {
        mText = text;
        mCopyableRanges = Collections.unmodifiableList(copyableRanges);
        mUrlRanges = Collections.unmodifiableList(urlRanges);
        mQuotedLines = Collections.unmodifiableList(quotedLines);
    }

    @NonNull
    public static OwnerCallBodyDisplayText of(@NonNull String body) {
        String withoutMarkerLine = withoutLeadingOwnerCallMarkerLine(body);
        StringBuilder text = new StringBuilder();
        List<OwnerCallBodyRange> copyableRanges = new ArrayList<>();
        int cursor = 0;
        while (cursor < withoutMarkerLine.length()) {
            int start = withoutMarkerLine.indexOf(COPY_MARKER_START, cursor);
            if (start < 0) {
                break;
            }
            int contentStart = start + COPY_MARKER_START.length();
            int contentEnd = withoutMarkerLine.indexOf(COPY_MARKER_END, contentStart);
            if (contentEnd < 0) {
                break;
            }
            text.append(withoutMarkerLine, cursor, start);
            String copied = withoutSurroundingNewlines(
                withoutMarkerLine.substring(contentStart, contentEnd));
            int rangeStart = text.length();
            text.append(copied);
            copyableRanges.add(new OwnerCallBodyRange(rangeStart, text.length(), copied));
            cursor = contentEnd + COPY_MARKER_END.length();
        }
        text.append(withoutMarkerLine, cursor, withoutMarkerLine.length());
        QuoteStripping quoteStripping = QuoteStripping.of(text.toString());
        List<OwnerCallBodyRange> displayedCopyableRanges = new ArrayList<>();
        for (OwnerCallBodyRange range : copyableRanges) {
            int start = quoteStripping.mapIndex(range.getStartIndex());
            int end = quoteStripping.mapIndex(range.getEndIndex());
            displayedCopyableRanges.add(new OwnerCallBodyRange(start, end,
                quoteStripping.getText().substring(start, end)));
        }
        String displayed = quoteStripping.getText();
        return new OwnerCallBodyDisplayText(displayed, displayedCopyableRanges,
            urlRangesIn(displayed), quoteStripping.getQuotedLines());
    }

    @NonNull
    public String getText() {
        return mText;
    }

    @NonNull
    public List<OwnerCallBodyRange> getCopyableRanges() {
        return mCopyableRanges;
    }

    @NonNull
    public List<OwnerCallBodyRange> getUrlRanges() {
        return mUrlRanges;
    }

    @NonNull
    public List<OwnerCallBodyRange> getQuotedLines() {
        return mQuotedLines;
    }

    @NonNull
    private static List<OwnerCallBodyRange> urlRangesIn(@NonNull String text) {
        List<OwnerCallBodyRange> ranges = new ArrayList<>();
        Matcher matcher = TermuxUrlUtils.getUrlMatchRegex().matcher(text);
        while (matcher.find()) {
            ranges.add(new OwnerCallBodyRange(matcher.start(), matcher.end(), matcher.group()));
        }
        return ranges;
    }

    @NonNull
    private static String withoutLeadingOwnerCallMarkerLine(@NonNull String body) {
        int firstLineEnd = body.indexOf('\n');
        String firstLine = firstLineEnd < 0 ? body : body.substring(0, firstLineEnd);
        if (!OWNER_CALL_MARKER.equals(firstLine.trim())) {
            return body;
        }
        if (firstLineEnd < 0) {
            return "";
        }
        int cursor = firstLineEnd + 1;
        while (cursor < body.length()) {
            int lineEnd = body.indexOf('\n', cursor);
            String line = lineEnd < 0 ? body.substring(cursor) : body.substring(cursor, lineEnd);
            if (!line.trim().isEmpty()) {
                break;
            }
            if (lineEnd < 0) {
                return "";
            }
            cursor = lineEnd + 1;
        }
        return body.substring(cursor);
    }

    @NonNull
    private static String withoutSurroundingNewlines(@NonNull String text) {
        int start = 0;
        int end = text.length();
        while (start < end && text.charAt(start) == '\n') {
            start++;
        }
        while (end > start && text.charAt(end - 1) == '\n') {
            end--;
        }
        return text.substring(start, end);
    }

    private static final class QuoteStripping {

        @NonNull
        private final String mText;

        @NonNull
        private final List<OwnerCallBodyRange> mQuotedLines;

        @NonNull
        private final List<int[]> mRemovals;

        private QuoteStripping(@NonNull String text, @NonNull List<OwnerCallBodyRange> quotedLines,
                               @NonNull List<int[]> removals) {
            mText = text;
            mQuotedLines = quotedLines;
            mRemovals = removals;
        }

        @NonNull
        static QuoteStripping of(@NonNull String markedText) {
            StringBuilder stripped = new StringBuilder(markedText.length());
            List<OwnerCallBodyRange> quotedLines = new ArrayList<>();
            List<int[]> removals = new ArrayList<>();
            int lineStart = 0;
            while (true) {
                int newline = markedText.indexOf('\n', lineStart);
                int lineEnd = newline < 0 ? markedText.length() : newline;
                Matcher marker = QUOTE_MARKER.matcher(markedText).region(lineStart, lineEnd);
                boolean quoted = marker.lookingAt();
                int contentStart = lineStart;
                if (quoted) {
                    contentStart = marker.end();
                    removals.add(new int[] {lineStart, contentStart - lineStart});
                }
                int displayedLineStart = stripped.length();
                stripped.append(markedText, contentStart, lineEnd);
                if (quoted) {
                    quotedLines.add(new OwnerCallBodyRange(displayedLineStart, stripped.length(),
                        stripped.substring(displayedLineStart)));
                }
                if (newline < 0) {
                    break;
                }
                stripped.append('\n');
                lineStart = newline + 1;
            }
            return new QuoteStripping(stripped.toString(), quotedLines, removals);
        }

        @NonNull
        String getText() {
            return mText;
        }

        @NonNull
        List<OwnerCallBodyRange> getQuotedLines() {
            return Collections.unmodifiableList(mQuotedLines);
        }

        int mapIndex(int markedIndex) {
            int removed = 0;
            for (int[] removal : mRemovals) {
                if (removal[0] < markedIndex) {
                    removed += Math.min(markedIndex - removal[0], removal[1]);
                }
            }
            return markedIndex - removed;
        }
    }
}
