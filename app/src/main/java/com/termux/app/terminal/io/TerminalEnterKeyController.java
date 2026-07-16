package com.termux.app.terminal.io;

import android.widget.EditText;
import android.widget.ImageButton;

import androidx.annotation.NonNull;

import com.termux.app.TermuxActivity;
import com.termux.app.terminal.SessionNewActivityStore;
import com.termux.app.terminal.SessionReplyTimeRecorder;
import com.termux.app.terminal.TermuxTerminalSessionActivityClient;
import com.termux.terminal.TerminalEmulator;
import com.termux.terminal.TerminalSession;
import com.termux.view.TerminalView;

public class TerminalEnterKeyController {

    private final TermuxActivity mActivity;
    private final ImageButton mButton;

    public TerminalEnterKeyController(@NonNull TermuxActivity activity, @NonNull ImageButton button) {
        this.mActivity = activity;
        this.mButton = button;
        mButton.setOnClickListener(view -> send());
    }

    private void send() {
        finishComposingText();
        boolean ownerContentSubmitted = commitToolbarTextInput();
        sendEnter();
        if (SendButtonReplySubmitDecision.shouldRecordReply(ownerContentSubmitted)) {
            recordReplyOnSubmit();
        }
    }

    private void finishComposingText() {
        TerminalView terminalView = mActivity.getTerminalView();
        if (terminalView != null) terminalView.finishComposingTextToTerminal();
    }

    private boolean commitToolbarTextInput() {
        if (!mActivity.isTerminalToolbarTextInputViewSelected()) return false;
        EditText editText = mActivity.getTerminalToolbarTextInput();
        if (editText == null) return false;
        TerminalToolbarViewPager.PageAdapter adapter = mActivity.getTerminalToolbarViewPagerAdapter();
        if (adapter == null) return false;
        return adapter.commitTextInputToTerminal(editText);
    }

    private void sendEnter() {
        TerminalSession session = mActivity.getCurrentSession();
        if (session == null || !session.isRunning()) return;
        TerminalEmulator emulator = session.getEmulator();
        if (emulator == null) return;
        session.write(TerminalEnterKeyEncoder.enterSequence(
            emulator.isCursorKeysApplicationMode(), emulator.isKeypadApplicationMode()));
    }

    private void recordReplyOnSubmit() {
        TerminalSession session = mActivity.getCurrentSession();
        if (session == null || !session.isRunning()) return;
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
}
