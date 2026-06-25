package com.termux.app.terminal.io;

import android.app.AlertDialog;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewpager.widget.PagerAdapter;

import com.termux.R;
import com.termux.app.TermuxActivity;
import com.termux.app.terminal.SessionNewActivityStore;
import com.termux.app.terminal.TermuxTerminalSessionActivityClient;
import com.termux.shared.interact.DialogUtils;
import com.termux.shared.logger.Logger;
import com.termux.shared.termux.extrakeys.ExtraKeysView;
import com.termux.terminal.TerminalSession;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

public class TerminalToolbarViewPager {

    public static class PageAdapter extends PagerAdapter {

        final TermuxActivity mActivity;
        String mSavedTextInput;

        private final Map<TerminalSession, String> mSessionTextInputs = new HashMap<>();

        static final int SUBMITTED_TEXT_INPUT_HISTORY_SIZE = 5;
        private final Deque<String> mSubmittedTextInputHistory = new ArrayDeque<>(SUBMITTED_TEXT_INPUT_HISTORY_SIZE);

        public PageAdapter(TermuxActivity activity, String savedTextInput) {
            this.mActivity = activity;
            this.mSavedTextInput = savedTextInput;
        }

        public void saveTextInputForSession(@Nullable TerminalSession session) {
            if (session == null) return;
            EditText editText = mActivity.getTerminalToolbarTextInput();
            if (editText == null) return;
            mSessionTextInputs.put(session, editText.getText().toString());
        }

        public void restoreTextInputForSession(@Nullable TerminalSession session) {
            EditText editText = mActivity.getTerminalToolbarTextInput();
            if (editText == null) return;
            String text = session == null ? null : mSessionTextInputs.get(session);
            editText.setText(text == null ? "" : text);
        }

        public void removeTextInputForSession(@Nullable TerminalSession session) {
            if (session == null) return;
            mSessionTextInputs.remove(session);
        }

        void addSubmittedTextInputToHistory(String submittedTextInput) {
            if (submittedTextInput == null || submittedTextInput.length() == 0) return;
            mSubmittedTextInputHistory.remove(submittedTextInput);
            mSubmittedTextInputHistory.addFirst(submittedTextInput);
            while (mSubmittedTextInputHistory.size() > SUBMITTED_TEXT_INPUT_HISTORY_SIZE) {
                mSubmittedTextInputHistory.removeLast();
            }
        }

        public void setupTextInputRow(final View textInputRow) {
            final EditText editText = textInputRow.findViewById(R.id.terminal_toolbar_text_input);
            editText.setHorizontallyScrolling(false);
            editText.setMaxLines(3);

            if (mSavedTextInput != null) {
                editText.setText(mSavedTextInput);
                TerminalSession currentSession = mActivity.getCurrentSession();
                if (currentSession != null) mSessionTextInputs.put(currentSession, mSavedTextInput);
                mSavedTextInput = null;
            } else {
                TerminalSession currentSession = mActivity.getCurrentSession();
                String text = currentSession == null ? null : mSessionTextInputs.get(currentSession);
                if (text != null) editText.setText(text);
            }

            editText.setOnEditorActionListener((v, actionId, event) -> {
                submitTextInput(editText);
                return true;
            });

            editText.setOnKeyListener((v, keyCode, event) -> {
                if (keyCode == KeyEvent.KEYCODE_ENTER
                    && event.getAction() == KeyEvent.ACTION_DOWN
                    && event.hasNoModifiers()) {
                    submitTextInput(editText);
                    return true;
                }
                return false;
            });

            ImageButton historyButton = textInputRow.findViewById(R.id.terminal_toolbar_text_input_history_button);
            historyButton.setOnClickListener(v -> showSubmittedTextInputHistory(editText));

            ImageButton sendButton = textInputRow.findViewById(R.id.terminal_toolbar_enter_button);
            new TerminalEnterKeyController(mActivity, sendButton);
        }

        void submitTextInput(final EditText editText) {
            writeTextInputToSession(editText, true);
        }

        public void commitTextInputToTerminal(final EditText editText) {
            writeTextInputToSession(editText, false);
        }

        private void writeTextInputToSession(final EditText editText, boolean submitWhenEmpty) {
            TerminalSession session = mActivity.getCurrentSession();
            if (session != null) {
                String submittedTextInput = editText.getText().toString();
                addSubmittedTextInputToHistory(submittedTextInput);
                if (session.isRunning()) {
                    if (ToolbarTextInputEncoder.hasContentToSend(submittedTextInput, submitWhenEmpty)) {
                        session.write(ToolbarTextInputEncoder.textToSend(submittedTextInput, submitWhenEmpty));
                        recordUserInputForSession(session);
                    }
                } else {
                    mActivity.getTermuxTerminalSessionClient().removeFinishedSession(session);
                }
                editText.setText("");
                mSessionTextInputs.remove(session);
            }
        }

        private void recordUserInputForSession(@NonNull TerminalSession session) {
            if (session.mSessionName == null) return;
            SessionNewActivityStore store = mActivity.getSessionNewActivityStore();
            if (store == null) return;
            store.recordUserInput(session.mSessionName, System.currentTimeMillis());
            TermuxTerminalSessionActivityClient sessionClient =
                mActivity.getTermuxTerminalSessionClient();
            if (sessionClient != null && session == mActivity.getCurrentSession()) {
                sessionClient.updateSessionNameOverlay();
            }
        }

        void showSubmittedTextInputHistory(final EditText editText) {
            if (mSubmittedTextInputHistory.isEmpty()) {
                Logger.showToast(mActivity, mActivity.getString(R.string.msg_toolbar_text_input_history_empty), true);
                return;
            }

            final CharSequence[] history = mSubmittedTextInputHistory.toArray(new CharSequence[0]);
            ArrayAdapter<CharSequence> adapter = new ArrayAdapter<>(mActivity, R.layout.item_toolbar_text_input_history, history);
            DialogUtils.showDismissibleOnTouchOutside(new AlertDialog.Builder(mActivity)
                .setTitle(R.string.title_toolbar_text_input_history_dialog)
                .setAdapter(adapter, (dialog, which) -> {
                    editText.setText(history[which]);
                    editText.setSelection(editText.getText().length());
                }));
        }

        @Override
        public int getCount() {
            return 1;
        }

        @Override
        public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
            return view == object;
        }

        @NonNull
        @Override
        public Object instantiateItem(@NonNull ViewGroup collection, int position) {
            LayoutInflater inflater = LayoutInflater.from(mActivity);
            View layout = inflater.inflate(R.layout.view_terminal_toolbar_extra_keys, collection, false);
            ExtraKeysView extraKeysView = (ExtraKeysView) layout;
            extraKeysView.setExtraKeysViewClient(mActivity.getTermuxTerminalExtraKeys());
            extraKeysView.setButtonTextAllCaps(mActivity.getProperties().shouldExtraKeysTextBeAllCaps());
            mActivity.setExtraKeysView(extraKeysView);
            extraKeysView.reload(mActivity.getTermuxTerminalExtraKeys().getExtraKeysInfo(),
                mActivity.getTerminalToolbarDefaultHeight());

            if (mActivity.getProperties().isUsingFullScreen() && mActivity.getProperties().isUsingFullScreenWorkAround()) {
                FullScreenWorkAround.apply(mActivity);
            }

            collection.addView(layout);
            return layout;
        }

        @Override
        public void destroyItem(@NonNull ViewGroup collection, int position, @NonNull Object view) {
            collection.removeView((View) view);
        }

    }

}
