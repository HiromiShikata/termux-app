package com.termux.app.terminal;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

public final class AllSessionsStatuslineParser {

    @NonNull
    public List<ParsedStatuslineUpdate> parse(@NonNull List<SessionScreenText> sessionScreenTexts,
                                              long nowMillis,
                                              @NonNull TimeZone timeZone) {
        List<ParsedStatuslineUpdate> updates = new ArrayList<>();
        for (SessionScreenText sessionScreenText : sessionScreenTexts) {
            ClaudeStatuslineTimes statuslineTimes =
                ClaudeStatuslineTimes.parse(sessionScreenText.getScreenText(), nowMillis, timeZone);
            if (!statuslineTimes.hasAnyToken()) {
                continue;
            }
            updates.add(new ParsedStatuslineUpdate(sessionScreenText.getSessionName(),
                statuslineTimes.getCallTimeMillis(),
                statuslineTimes.getOutTimeMillis(),
                statuslineTimes.getReplyTimeMillis(),
                statuslineTimes.getSubagentCount()));
        }
        return updates;
    }

    public static final class SessionScreenText {

        @NonNull
        private final String mSessionName;

        @NonNull
        private final String mScreenText;

        public SessionScreenText(@NonNull String sessionName, @NonNull String screenText) {
            mSessionName = sessionName;
            mScreenText = screenText;
        }

        @NonNull
        public String getSessionName() {
            return mSessionName;
        }

        @NonNull
        public String getScreenText() {
            return mScreenText;
        }
    }
}
