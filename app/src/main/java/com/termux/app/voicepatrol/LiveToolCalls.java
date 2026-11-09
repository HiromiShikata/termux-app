package com.termux.app.voicepatrol;

import org.json.JSONException;
import org.json.JSONObject;

public final class LiveToolCalls {

    public static final String SESSION_REPLY_SEND = "session_reply_send";
    public static final String SESSION_SKIP = "session_skip";
    public static final String OWNER_CALL_REPEAT = "owner_call_repeat";

    private static final String REPLY_TEXT_PROPERTY = "replyText";

    private LiveToolCalls() {
    }

    public static ReplyText replyTextOf(String argumentsJson) {
        try {
            Object replyText = new JSONObject(argumentsJson).opt(REPLY_TEXT_PROPERTY);
            if (replyText instanceof String) {
                return ReplyText.valid((String) replyText);
            }
            return ReplyText.invalid();
        } catch (JSONException notAnObjectWithReplyText) {
            return ReplyText.invalid();
        }
    }

    public static final class ReplyText {

        private final boolean mValid;
        private final String mValue;

        private ReplyText(boolean valid, String value) {
            mValid = valid;
            mValue = value;
        }

        static ReplyText valid(String value) {
            return new ReplyText(true, value);
        }

        static ReplyText invalid() {
            return new ReplyText(false, "");
        }

        public boolean isValid() {
            return mValid;
        }

        public String getValue() {
            return mValue;
        }
    }
}
