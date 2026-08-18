package com.termux.app.store;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONException;

public final class ParsedTextStore<T> {

    public interface TextAccess {

        @Nullable
        String read();

        void write(@NonNull String text);
    }

    public interface TextParser<T> {

        T parse(@Nullable String text) throws JSONException;
    }

    private final TextAccess mTextAccess;

    private volatile boolean mStoredTextUnparseable;

    public ParsedTextStore(@NonNull TextAccess textAccess) {
        mTextAccess = textAccess;
    }

    public T load(@NonNull TextParser<T> parser) throws JSONException {
        try {
            T parsed = parser.parse(mTextAccess.read());
            mStoredTextUnparseable = false;
            return parsed;
        } catch (JSONException e) {
            mStoredTextUnparseable = true;
            throw e;
        }
    }

    public boolean isStoredTextUnparseable() {
        return mStoredTextUnparseable;
    }

    public boolean write(@NonNull String text) {
        if (mStoredTextUnparseable) return false;
        mTextAccess.write(text);
        return true;
    }
}
