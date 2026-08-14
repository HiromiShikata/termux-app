package com.termux.app.ownercall;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class OwnerCallDocumentParser {

    private static final String DOCUMENT_DELIMITER = "---";
    private static final String SESSION_NAME_FIELD = "sessionName:";
    private static final String CALLED_AT_FIELD = "calledAt:";
    private static final String BODY_FIELD = "body:";
    private static final String BODY_INDENTATION = "  ";
    private static final char LINE_SEPARATOR = '\n';
    private static final char CARRIAGE_RETURN = '\r';
    private static final char DOUBLE_QUOTE = '"';

    @NonNull
    public List<OwnerCall> parse(@Nullable String document) {
        List<OwnerCall> calls = new ArrayList<>();
        if (document == null || document.isEmpty()) {
            return calls;
        }

        String sessionName = null;
        String calledAt = null;
        List<String> bodyLines = null;
        boolean readingBody = false;
        for (String rawLine : document.split(String.valueOf(LINE_SEPARATOR), -1)) {
            String line = withoutCarriageReturn(rawLine);
            if (DOCUMENT_DELIMITER.equals(line)) {
                addCall(calls, sessionName, calledAt, bodyLines);
                sessionName = null;
                calledAt = null;
                bodyLines = null;
                readingBody = false;
                continue;
            }
            if (readingBody) {
                if (line.trim().isEmpty()) {
                    bodyLines.add("");
                    continue;
                }
                if (line.startsWith(BODY_INDENTATION)) {
                    bodyLines.add(line.substring(BODY_INDENTATION.length()));
                    continue;
                }
                readingBody = false;
            }
            if (line.startsWith(SESSION_NAME_FIELD)) {
                sessionName = scalarOf(line, SESSION_NAME_FIELD);
            } else if (line.startsWith(CALLED_AT_FIELD)) {
                calledAt = scalarOf(line, CALLED_AT_FIELD);
            } else if (line.startsWith(BODY_FIELD)) {
                bodyLines = new ArrayList<>();
                readingBody = true;
            }
        }
        addCall(calls, sessionName, calledAt, bodyLines);
        return calls;
    }

    private static void addCall(@NonNull List<OwnerCall> calls, @Nullable String sessionName,
                                @Nullable String calledAt, @Nullable List<String> bodyLines) {
        if (sessionName == null || calledAt == null || bodyLines == null) {
            return;
        }
        calls.add(new OwnerCall(sessionName, calledAt, joinBody(bodyLines)));
    }

    @NonNull
    private static String joinBody(@NonNull List<String> bodyLines) {
        int lastLine = bodyLines.size();
        while (lastLine > 0 && bodyLines.get(lastLine - 1).isEmpty()) {
            lastLine--;
        }
        StringBuilder body = new StringBuilder();
        for (int index = 0; index < lastLine; index++) {
            if (index > 0) {
                body.append(LINE_SEPARATOR);
            }
            body.append(bodyLines.get(index));
        }
        return body.toString();
    }

    @NonNull
    private static String scalarOf(@NonNull String line, @NonNull String field) {
        String value = line.substring(field.length()).trim();
        if (value.length() >= 2 && value.charAt(0) == DOUBLE_QUOTE
            && value.charAt(value.length() - 1) == DOUBLE_QUOTE) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    @NonNull
    private static String withoutCarriageReturn(@NonNull String line) {
        if (!line.isEmpty() && line.charAt(line.length() - 1) == CARRIAGE_RETURN) {
            return line.substring(0, line.length() - 1);
        }
        return line;
    }
}
