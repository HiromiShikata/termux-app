package com.termux.app.terminal.io;

import android.app.AlertDialog;
import android.content.Context;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewpager.widget.PagerAdapter;

import com.termux.R;
import com.termux.app.TermuxActivity;
import com.termux.app.terminal.MaxHeightScrollView;
import com.termux.app.terminal.SessionNewActivityStore;
import com.termux.app.terminal.SessionReplyTimeRecorder;
import com.termux.app.terminal.TermuxTerminalSessionActivityClient;
import com.termux.shared.interact.DialogUtils;
import com.termux.shared.logger.Logger;
import com.termux.shared.termux.extrakeys.ExtraKeysView;
import com.termux.shared.termux.settings.preferences.TermuxAppSharedPreferences;
import com.termux.terminal.TerminalSession;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TerminalToolbarViewPager {

    public static class PageAdapter extends PagerAdapter {

        final TermuxActivity mActivity;
        String mSavedTextInput;

        private final Map<TerminalSession, String> mSessionTextInputs = new HashMap<>();

        static final int SUBMITTED_TEXT_INPUT_HISTORY_SIZE = 5;
        private final PinnedTextInputHistorySerializer mPinnedTextInputHistorySerializer = new PinnedTextInputHistorySerializer();
        private final SubmittedTextInputHistory mSubmittedTextInputHistory;

        public PageAdapter(TermuxActivity activity, String savedTextInput) {
            this.mActivity = activity;
            this.mSavedTextInput = savedTextInput;
            this.mSubmittedTextInputHistory = new SubmittedTextInputHistory(
                SUBMITTED_TEXT_INPUT_HISTORY_SIZE, loadPersistedPinnedTextInputHistory(activity));
        }

        private List<String> loadPersistedPinnedTextInputHistory(TermuxActivity activity) {
            TermuxAppSharedPreferences preferences = activity.getPreferences();
            String serialized = preferences == null ? null : preferences.getPinnedTextInputHistory();
            return mPinnedTextInputHistorySerializer.deserialize(serialized);
        }

        private void persistPinnedTextInputHistory() {
            TermuxAppSharedPreferences preferences = mActivity.getPreferences();
            if (preferences == null) return;
            preferences.setPinnedTextInputHistory(
                mPinnedTextInputHistorySerializer.serialize(mSubmittedTextInputHistory.getPinnedEntries()));
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
            mSubmittedTextInputHistory.add(submittedTextInput);
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

        public boolean commitTextInputToTerminal(final EditText editText) {
            return writeTextInputToSession(editText, false);
        }

        private boolean writeTextInputToSession(final EditText editText, boolean submitWhenEmpty) {
            TerminalSession session = mActivity.getCurrentSession();
            boolean ownerContentSubmitted = false;
            if (session != null) {
                String submittedTextInput = editText.getText().toString();
                addSubmittedTextInputToHistory(submittedTextInput);
                if (session.isRunning()) {
                    if (ToolbarTextInputEncoder.hasContentToSend(submittedTextInput, submitWhenEmpty)) {
                        session.write(ToolbarTextInputEncoder.textToSend(submittedTextInput, submitWhenEmpty));
                        recordUserInputForSession(session);
                        ownerContentSubmitted = true;
                    }
                } else if (mActivity.getTermuxTerminalSessionClient().decideFinishedSessionEnterAction(session).isReconnect()) {
                    mActivity.getTermuxTerminalSessionClient().reconnectFinishedSessionInPlace(session, submittedTextInput);
                } else {
                    mActivity.getTermuxTerminalSessionClient().removeFinishedSession(session);
                }
                editText.setText("");
                mSessionTextInputs.remove(session);
            }
            return ownerContentSubmitted;
        }

        private void recordUserInputForSession(@NonNull TerminalSession session) {
            SessionNewActivityStore store = mActivity.getSessionNewActivityStore();
            if (store == null) return;
            boolean recorded = new SessionReplyTimeRecorder(store)
                .recordReplyOnSubmit(session, System.currentTimeMillis());
            if (!recorded) return;
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

            final SubmittedTextInputHistoryAdapter adapter = new SubmittedTextInputHistoryAdapter(
                mActivity, mSubmittedTextInputHistory, this::persistPinnedTextInputHistory);
            DialogUtils.showDismissibleOnTouchOutside(new AlertDialog.Builder(mActivity)
                .setTitle(R.string.title_toolbar_text_input_history_dialog)
                .setAdapter(adapter, (dialog, which) -> {
                    editText.setText(adapter.getItem(which));
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

    static final class SubmittedTextInputHistoryAdapter extends BaseAdapter {

        private final Context mContext;
        private final SubmittedTextInputHistory mHistory;
        private final Runnable mOnPinnedChanged;
        private final LayoutInflater mInflater;
        private List<String> mOrderedEntries;

        SubmittedTextInputHistoryAdapter(@NonNull Context context,
                                         @NonNull SubmittedTextInputHistory history,
                                         @NonNull Runnable onPinnedChanged) {
            mContext = context;
            mHistory = history;
            mOnPinnedChanged = onPinnedChanged;
            mInflater = LayoutInflater.from(context);
            mOrderedEntries = history.getOrderedEntries();
        }

        @Override
        public int getCount() {
            return mOrderedEntries.size();
        }

        @NonNull
        @Override
        public String getItem(int position) {
            return mOrderedEntries.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            View row = convertView != null
                ? convertView
                : mInflater.inflate(R.layout.item_toolbar_text_input_history, parent, false);
            final String entry = getItem(position);
            TextView entryView = row.findViewById(R.id.toolbar_text_input_history_entry);
            entryView.setText(entry);
            MaxHeightScrollView entryScroll =
                row.findViewById(R.id.toolbar_text_input_history_entry_scroll);
            HistoryEntryScrollBinder.bind(entryScroll, entryView);
            ImageButton pinButton = row.findViewById(R.id.toolbar_text_input_history_pin_button);
            boolean pinned = mHistory.isPinned(entry);
            pinButton.setImageResource(pinned
                ? R.drawable.ic_browser_bookmark_star_filled
                : R.drawable.ic_browser_bookmark_star_outline);
            pinButton.setContentDescription(mContext.getString(pinned
                ? R.string.action_toolbar_text_input_history_unpin
                : R.string.action_toolbar_text_input_history_pin));
            pinButton.setOnClickListener(v -> {
                mHistory.togglePin(entry);
                mOnPinnedChanged.run();
                refresh();
            });
            ImageButton editButton = row.findViewById(R.id.toolbar_text_input_history_edit_button);
            editButton.setVisibility(pinned ? View.VISIBLE : View.GONE);
            editButton.setContentDescription(
                mContext.getString(R.string.action_toolbar_text_input_history_edit));
            editButton.setOnClickListener(v -> new PinnedTextInputHistoryEditPrompt(
                mContext, mHistory, () -> {
                    mOnPinnedChanged.run();
                    refresh();
                }).promptEdit(entry));
            return row;
        }

        private void refresh() {
            mOrderedEntries = mHistory.getOrderedEntries();
            notifyDataSetChanged();
        }
    }

}
