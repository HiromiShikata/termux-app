package com.termux.app.terminal.io;

import android.widget.EditText;

import androidx.annotation.NonNull;

final class HistoryEntryRestore {

    private HistoryEntryRestore() {
    }

    static void into(@NonNull EditText editText, @NonNull String entry) {
        editText.setText(entry);
        editText.setSelection(editText.getText().length());
    }
}
