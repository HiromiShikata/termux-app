package com.termux.app.terminal;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public final class CallToUserAlertLedger {

    public interface Storage {
        @Nullable
        String read();

        void write(@NonNull String value);
    }

    static final int MAX_IDENTITIES = 1000;

    private static final char FIELD_SEPARATOR = '\t';
    private static final char RECORD_SEPARATOR = '\n';
    private static final char ESCAPE = '\\';

    private final Storage mStorage;
    private final List<Identity> mIdentities = new ArrayList<>();

    public CallToUserAlertLedger(@NonNull Storage storage) {
        mStorage = storage;
        String stored = storage.read();
        if (stored == null || stored.isEmpty()) return;
        for (String record : stored.split(String.valueOf(RECORD_SEPARATOR))) {
            Identity identity = decode(record);
            if (identity != null) mIdentities.add(identity);
        }
    }

    public boolean markAlertedIfNew(@NonNull String sessionName, @NonNull String callCycleKey) {
        return markAlertedIfNew(sessionName, callCycleKey, callCycleKey);
    }

    public boolean markAlertedIfNew(@NonNull String sessionName, @NonNull String callCycleKey,
                                    @NonNull String tagValue) {
        Identity identity = new Identity(sessionName, callCycleKey, tagValue);
        if (isBareKey(callCycleKey, tagValue)) {
            if (hasAlertedTagValue(sessionName, tagValue)) return false;
        } else if (mIdentities.contains(identity)) {
            return false;
        }
        mIdentities.add(identity);
        while (mIdentities.size() > MAX_IDENTITIES) mIdentities.remove(0);
        persist();
        return true;
    }

    private static boolean isBareKey(@NonNull String callCycleKey, @NonNull String tagValue) {
        return callCycleKey.equals(tagValue);
    }

    private boolean hasAlertedTagValue(@NonNull String sessionName, @NonNull String tagValue) {
        for (Identity identity : mIdentities) {
            if (identity.mSessionName.equals(sessionName) && identity.mTagValue.equals(tagValue)) return true;
        }
        return false;
    }

    public void pruneToSessionNames(@NonNull Set<String> liveSessionNames) {
        boolean removed = false;
        Iterator<Identity> iterator = mIdentities.iterator();
        while (iterator.hasNext()) {
            if (!liveSessionNames.contains(iterator.next().mSessionName)) {
                iterator.remove();
                removed = true;
            }
        }
        if (removed) persist();
    }

    int size() {
        return mIdentities.size();
    }

    private void persist() {
        StringBuilder builder = new StringBuilder();
        for (Identity identity : mIdentities) {
            builder.append(encode(identity.mSessionName)).append(FIELD_SEPARATOR)
                .append(encode(identity.mCallCycleKey)).append(FIELD_SEPARATOR)
                .append(encode(identity.mTagValue)).append(RECORD_SEPARATOR);
        }
        mStorage.write(builder.toString());
    }

    @Nullable
    private static Identity decode(@NonNull String record) {
        String[] fields = record.split(String.valueOf(FIELD_SEPARATOR), -1);
        if (fields.length != 3) return null;
        return new Identity(unescape(fields[0]), unescape(fields[1]), unescape(fields[2]));
    }

    @NonNull
    private static String encode(@NonNull String field) {
        StringBuilder builder = new StringBuilder(field.length());
        for (int i = 0; i < field.length(); i++) {
            char character = field.charAt(i);
            if (character == ESCAPE) {
                builder.append(ESCAPE).append(ESCAPE);
            } else if (character == FIELD_SEPARATOR) {
                builder.append(ESCAPE).append('t');
            } else if (character == RECORD_SEPARATOR) {
                builder.append(ESCAPE).append('n');
            } else {
                builder.append(character);
            }
        }
        return builder.toString();
    }

    @NonNull
    private static String unescape(@NonNull String encoded) {
        StringBuilder builder = new StringBuilder(encoded.length());
        for (int i = 0; i < encoded.length(); i++) {
            char character = encoded.charAt(i);
            if (character == ESCAPE && i + 1 < encoded.length()) {
                char next = encoded.charAt(++i);
                if (next == 't') {
                    builder.append(FIELD_SEPARATOR);
                } else if (next == 'n') {
                    builder.append(RECORD_SEPARATOR);
                } else {
                    builder.append(next);
                }
            } else {
                builder.append(character);
            }
        }
        return builder.toString();
    }

    private static final class Identity {
        final String mSessionName;
        final String mCallCycleKey;
        final String mTagValue;

        Identity(String sessionName, String callCycleKey, String tagValue) {
            mSessionName = sessionName;
            mCallCycleKey = callCycleKey;
            mTagValue = tagValue;
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof Identity)) return false;
            Identity identity = (Identity) other;
            return mSessionName.equals(identity.mSessionName)
                && mCallCycleKey.equals(identity.mCallCycleKey)
                && mTagValue.equals(identity.mTagValue);
        }

        @Override
        public int hashCode() {
            return (31 * (31 * mSessionName.hashCode() + mCallCycleKey.hashCode())) + mTagValue.hashCode();
        }
    }
}
