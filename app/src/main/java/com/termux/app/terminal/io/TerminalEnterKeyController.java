package com.termux.app.terminal.io;

import android.widget.EditText;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.termux.R;
import com.termux.app.TermuxActivity;
import com.termux.app.terminal.SessionGenuineReplyRecorder;
import com.termux.app.terminal.SessionNewActivityStore;
import com.termux.app.terminal.TermuxTerminalSessionActivityClient;
import com.termux.shared.logger.Logger;
import com.termux.terminal.TerminalEmulator;
import com.termux.terminal.TerminalSession;
import com.termux.view.TerminalView;

import java.util.HashMap;
import java.util.Map;

public class TerminalEnterKeyController {

    private final TermuxActivity mActivity;
    private final ImageButton mButton;
    private final Map<String, Long> mLastBareEnterTimeMillisBySessionName = new HashMap<>();

    public TerminalEnterKeyController(@NonNull TermuxActivity activity, @NonNull ImageButton button) {
        this.mActivity = activity;
        this.mButton = button;
        mButton.setOnClickListener(view -> submit(
            mActivity.getTerminalToolbarTextInput(), mActivity.getTerminalToolbarViewPagerAdapter()));
    }

    public boolean submit(@Nullable EditText editText,
                          @Nullable TerminalToolbarViewPager.PageAdapter adapter) {
        if (!inputReachesTheProgramReadingTheTerminal()) {
            if (reconnectAndDeliverAfterReconnect(editText)) return true;
            Logger.showToast(mActivity,
                mActivity.getString(R.string.msg_terminal_input_not_delivered), true);
            return false;
        }
        finishComposingText();
        boolean ownerContentSubmitted = commitToolbarTextInput(editText, adapter);
        sendEnter(ownerContentSubmitted);
        if (SendButtonReplySubmitDecision.shouldRecordReply(ownerContentSubmitted)) {
            recordReplyOnSubmit();
        }
        return ownerContentSubmitted;
    }

    private boolean reconnectAndDeliverAfterReconnect(@Nullable EditText editText) {
        TerminalSession session = mActivity.getCurrentSession();
        TermuxTerminalSessionActivityClient sessionClient = mActivity.getTermuxTerminalSessionClient();
        if (session == null || sessionClient == null) return false;
        String pendingInput = editText == null ? null : editText.getText().toString();
        boolean reconnected = sessionClient.reconnectFinishedSessionInPlace(session, pendingInput);
        if (!reconnected) return false;
        if (editText != null) editText.setText("");
        if (ReconnectSubmitReplyDecision.shouldRecordReply(reconnected,
                pendingInput == null ? "" : pendingInput)) {
            recordReplyOnSubmit();
        }
        return true;
    }

    private void finishComposingText() {
        TerminalView terminalView = mActivity.getTerminalView();
        if (terminalView != null) terminalView.finishComposingTextToTerminal();
    }

    private boolean commitToolbarTextInput(@Nullable EditText editText,
                                           @Nullable TerminalToolbarViewPager.PageAdapter adapter) {
        if (editText == null || adapter == null) return false;
        return adapter.commitTextInputToTerminal(editText);
    }

    private boolean inputReachesTheProgramReadingTheTerminal() {
        TerminalSession session = mActivity.getCurrentSession();
        return session != null && session.inputReachesTheProgramReadingTheTerminal();
    }

    private void sendEnter(boolean ownerContentSubmitted) {
        TerminalSession session = mActivity.getCurrentSession();
        if (session == null || !session.inputReachesTheProgramReadingTheTerminal()) return;
        TerminalEmulator emulator = session.getEmulator();
        if (emulator == null) return;
        if (!ownerContentSubmitted && !allowBareEnter(session)) return;
        session.write(TerminalEnterKeyEncoder.enterSequence(
            emulator.isCursorKeysApplicationMode(), emulator.isKeypadApplicationMode()));
    }

    private boolean allowBareEnter(@NonNull TerminalSession session) {
        String sessionName = session.mSessionName;
        if (sessionName == null) return true;
        SessionNewActivityStore store = mActivity.getSessionNewActivityStore();
        if (store == null) return true;
        if (!BareEnterWriteDecision.shouldWrite(mLastBareEnterTimeMillisBySessionName.get(sessionName),
                store.getLastOutputActivityTimeMillis(sessionName))) {
            return false;
        }
        mLastBareEnterTimeMillisBySessionName.put(sessionName, System.currentTimeMillis());
        return true;
    }

    private void recordReplyOnSubmit() {
        TerminalSession session = mActivity.getCurrentSession();
        if (session == null || !session.isRunning()) return;
        SessionNewActivityStore store = mActivity.getSessionNewActivityStore();
        if (store == null) return;
        new SessionGenuineReplyRecorder(
            store,
            sessionName -> mActivity.deleteAnsweredOwnerCallsOfSession(sessionName),
            () -> {
                TermuxTerminalSessionActivityClient sessionClient =
                    mActivity.getTermuxTerminalSessionClient();
                if (sessionClient != null) sessionClient.updateSessionNameOverlay();
            }
        ).recordReply(session, System.currentTimeMillis(), session == mActivity.getCurrentSession());
    }
}
