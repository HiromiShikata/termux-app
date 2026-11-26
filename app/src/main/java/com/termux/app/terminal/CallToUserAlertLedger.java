package com.termux.app.terminal;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

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
    private final List<String> mEncodedIdentities = new ArrayList<>();

    public CallToUserAlertLedger(@NonNull Storage storage) {
        mStorage = storage;
        String stored = storage.read();
        if (stored == null || stored.isEmpty()) return;
        for (String record : stored.split(String.valueOf(RECORD_SEPARATOR))) {
            if (!record.isEmpty()) mEncodedIdentities.add(record);
        }
    }

    public boolean markAlertedIfNew(@NonNull String sessionName, @NonNull String callCycleKey) {
        String encoded = encode(sessionName) + FIELD_SEPARATOR + encode(callCycleKey);
        if (mEncodedIdentities.contains(encoded)) return false;
        mEncodedIdentities.add(encoded);
        while (mEncodedIdentities.size() > MAX_IDENTITIES) mEncodedIdentities.remove(0);
        mStorage.write(joinRecords());
        return true;
    }

    @NonNull
    private String joinRecords() {
        StringBuilder builder = new StringBuilder();
        for (String encoded : mEncodedIdentities) {
            builder.append(encoded).append(RECORD_SEPARATOR);
        }
        return builder.toString();
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
}
