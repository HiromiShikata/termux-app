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
        Identity identity = new Identity(sessionName, callCycleKey);
        if (mIdentities.contains(identity)) return false;
        mIdentities.add(identity);
        while (mIdentities.size() > MAX_IDENTITIES) mIdentities.remove(0);
        persist();
        return true;
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
                .append(encode(identity.mCallCycleKey)).append(RECORD_SEPARATOR);
        }
        mStorage.write(builder.toString());
    }

    @Nullable
    private static Identity decode(@NonNull String record) {
        int separatorIndex = record.indexOf(FIELD_SEPARATOR);
        if (separatorIndex < 0) return null;
        return new Identity(unescape(record.substring(0, separatorIndex)),
            unescape(record.substring(separatorIndex + 1)));
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

        Identity(String sessionName, String callCycleKey) {
            mSessionName = sessionName;
            mCallCycleKey = callCycleKey;
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof Identity)) return false;
            Identity identity = (Identity) other;
            return mSessionName.equals(identity.mSessionName) && mCallCycleKey.equals(identity.mCallCycleKey);
        }

        @Override
        public int hashCode() {
            return 31 * mSessionName.hashCode() + mCallCycleKey.hashCode();
        }
    }
}
