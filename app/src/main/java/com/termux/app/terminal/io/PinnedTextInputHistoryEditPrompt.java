package com.termux.app.terminal.io;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.NonNull;

import com.termux.R;
import com.termux.shared.interact.DialogUtils;

final class PinnedTextInputHistoryEditPrompt {

    private final Context mContext;
    private final SubmittedTextInputHistory mHistory;
    private final Runnable mOnEdited;

    PinnedTextInputHistoryEditPrompt(@NonNull Context context,
                                     @NonNull SubmittedTextInputHistory history,
                                     @NonNull Runnable onEdited) {
        mContext = context;
        mHistory = history;
        mOnEdited = onEdited;
    }

    @NonNull
    AlertDialog promptEdit(@NonNull String currentEntry) {
        View content = LayoutInflater.from(mContext)
            .inflate(R.layout.dialog_toolbar_text_input_history_edit, null, false);
        EditText input = content.findViewById(R.id.toolbar_text_input_history_edit_input);
        input.setText(currentEntry);
        input.setSelection(input.getText().length());
        return DialogUtils.showDismissibleOnTouchOutside(new AlertDialog.Builder(mContext)
            .setTitle(R.string.title_toolbar_text_input_history_edit_dialog)
            .setView(content)
            .setPositiveButton(R.string.action_toolbar_text_input_history_edit_save,
                (dialog, which) -> applyEdit(currentEntry, input.getText().toString()))
            .setNegativeButton(android.R.string.cancel, null));
    }

    private void applyEdit(@NonNull String currentEntry, @NonNull String newEntry) {
        if (mHistory.editPinnedEntry(currentEntry, newEntry)) {
            mOnEdited.run();
        }
    }
}
