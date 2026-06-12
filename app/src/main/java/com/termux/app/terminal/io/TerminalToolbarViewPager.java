package com.termux.app.terminal.io;

import android.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.termux.R;
import com.termux.app.TermuxActivity;
import com.termux.shared.logger.Logger;
import com.termux.shared.termux.extrakeys.ExtraKeysView;
import com.termux.terminal.TerminalSession;

import java.util.ArrayDeque;
import java.util.Deque;

public class TerminalToolbarViewPager {

    public static class PageAdapter extends PagerAdapter {

        final TermuxActivity mActivity;
        String mSavedTextInput;

        static final int SUBMITTED_TEXT_INPUT_HISTORY_SIZE = 5;
        private final Deque<String> mSubmittedTextInputHistory = new ArrayDeque<>(SUBMITTED_TEXT_INPUT_HISTORY_SIZE);

        public PageAdapter(TermuxActivity activity, String savedTextInput) {
            this.mActivity = activity;
            this.mSavedTextInput = savedTextInput;
        }

        void addSubmittedTextInputToHistory(String submittedTextInput) {
            if (submittedTextInput == null || submittedTextInput.length() == 0) return;
            mSubmittedTextInputHistory.remove(submittedTextInput);
            mSubmittedTextInputHistory.addFirst(submittedTextInput);
            while (mSubmittedTextInputHistory.size() > SUBMITTED_TEXT_INPUT_HISTORY_SIZE) {
                mSubmittedTextInputHistory.removeLast();
            }
        }

        void showSubmittedTextInputHistory(final EditText editText) {
            if (mSubmittedTextInputHistory.isEmpty()) {
                Logger.showToast(mActivity, mActivity.getString(R.string.msg_toolbar_text_input_history_empty), true);
                return;
            }

            final CharSequence[] history = mSubmittedTextInputHistory.toArray(new CharSequence[0]);
            new AlertDialog.Builder(mActivity)
                .setTitle(R.string.title_toolbar_text_input_history_dialog)
                .setItems(history, (dialog, which) -> {
                    editText.setText(history[which]);
                    editText.setSelection(editText.getText().length());
                })
                .show();
        }

        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
            return view == object;
        }

        @NonNull
        @Override
        public Object instantiateItem(@NonNull ViewGroup collection, int position) {
            LayoutInflater inflater = LayoutInflater.from(mActivity);
            View layout;
            if (position == 0) {
                layout = inflater.inflate(R.layout.view_terminal_toolbar_extra_keys, collection, false);
                ExtraKeysView extraKeysView = (ExtraKeysView) layout;
                extraKeysView.setExtraKeysViewClient(mActivity.getTermuxTerminalExtraKeys());
                extraKeysView.setButtonTextAllCaps(mActivity.getProperties().shouldExtraKeysTextBeAllCaps());
                mActivity.setExtraKeysView(extraKeysView);
                extraKeysView.reload(mActivity.getTermuxTerminalExtraKeys().getExtraKeysInfo(),
                    mActivity.getTerminalToolbarDefaultHeight());

                // apply extra keys fix if enabled in prefs
                if (mActivity.getProperties().isUsingFullScreen() && mActivity.getProperties().isUsingFullScreenWorkAround()) {
                    FullScreenWorkAround.apply(mActivity);
                }

            } else {
                layout = inflater.inflate(R.layout.view_terminal_toolbar_text_input, collection, false);
                final EditText editText = layout.findViewById(R.id.terminal_toolbar_text_input);

                if (mSavedTextInput != null) {
                    editText.setText(mSavedTextInput);
                    mSavedTextInput = null;
                }

                editText.setOnEditorActionListener((v, actionId, event) -> {
                    TerminalSession session = mActivity.getCurrentSession();
                    if (session != null) {
                        String submittedTextInput = editText.getText().toString();
                        addSubmittedTextInputToHistory(submittedTextInput);
                        if (session.isRunning()) {
                            String textToSend = submittedTextInput;
                            if (textToSend.length() == 0) textToSend = "\r";
                            session.write(textToSend);
                        } else {
                            mActivity.getTermuxTerminalSessionClient().removeFinishedSession(session);
                        }
                        editText.setText("");
                    }
                    return true;
                });

                editText.setOnLongClickListener(v -> {
                    showSubmittedTextInputHistory(editText);
                    return true;
                });
            }
            collection.addView(layout);
            return layout;
        }

        @Override
        public void destroyItem(@NonNull ViewGroup collection, int position, @NonNull Object view) {
            collection.removeView((View) view);
        }

    }



    public static class OnPageChangeListener extends ViewPager.SimpleOnPageChangeListener {

        final TermuxActivity mActivity;
        final ViewPager mTerminalToolbarViewPager;

        public OnPageChangeListener(TermuxActivity activity, ViewPager viewPager) {
            this.mActivity = activity;
            this.mTerminalToolbarViewPager = viewPager;
        }

        @Override
        public void onPageSelected(int position) {
            if (position == 0) {
                mActivity.getTerminalView().requestFocus();
            } else {
                final EditText editText = mTerminalToolbarViewPager.findViewById(R.id.terminal_toolbar_text_input);
                if (editText != null) editText.requestFocus();
            }
        }

    }

}
