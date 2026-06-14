package com.termux.app.terminal.io;

import android.widget.ImageButton;

import androidx.annotation.NonNull;

import com.termux.R;
import com.termux.app.TermuxActivity;
import com.termux.shared.logger.Logger;
import com.termux.shared.shell.command.ExecutionCommand;
import com.termux.shared.shell.command.runner.app.AppShell;
import com.termux.shared.termux.TermuxConstants;
import com.termux.shared.termux.shell.command.environment.TermuxShellEnvironment;

import java.util.HashMap;

public class TmuxMouseModeToggleController {

    private static final String ENV_TMUX_TMPDIR = "TMUX_TMPDIR";
    private static final String TMUX_SOCKET_DIRECTORY_PATH = TermuxConstants.TERMUX_VAR_PREFIX_DIR_PATH + "/run";

    private final TermuxActivity mActivity;
    private final ImageButton mButton;

    public TmuxMouseModeToggleController(@NonNull TermuxActivity activity, @NonNull ImageButton button) {
        this.mActivity = activity;
        this.mButton = button;

        applyAppearance(mActivity.getPreferences().isTmuxMouseModeEnabled());
        mButton.setOnClickListener(view -> new Thread(this::toggleOnServer).start());
        new Thread(this::syncAppearanceFromServer).start();
    }

    private void syncAppearanceFromServer() {
        Boolean current = readServerMouseMode();
        if (current == null) return;
        mActivity.getPreferences().setTmuxMouseModeEnabled(current);
        mActivity.runOnUiThread(() -> applyAppearance(current));
    }

    private void toggleOnServer() {
        Boolean current = readServerMouseMode();
        if (current == null) {
            mActivity.runOnUiThread(() -> Logger.showToast(mActivity,
                mActivity.getString(R.string.msg_tmux_mouse_mode_no_server), false));
            return;
        }
        boolean target = TmuxMouseModeController.nextState(current);
        runTmuxCommand(TmuxMouseModeController.setMouseArguments(target));
        Boolean applied = readServerMouseMode();
        boolean enabled = applied != null ? applied : target;
        mActivity.getPreferences().setTmuxMouseModeEnabled(enabled);
        mActivity.runOnUiThread(() -> {
            applyAppearance(enabled);
            Logger.showToast(mActivity, mActivity.getString(enabled
                ? R.string.msg_tmux_mouse_mode_on : R.string.msg_tmux_mouse_mode_off), false);
        });
    }

    private Boolean readServerMouseMode() {
        return TmuxMouseModeController.parseMouseState(runTmuxCommand(TmuxMouseModeController.SHOW_MOUSE_ARGUMENTS));
    }

    private void applyAppearance(boolean enabled) {
        int tint = mActivity.getResources().getColor(enabled
            ? android.R.color.holo_blue_light : android.R.color.white);
        mButton.setColorFilter(tint);
        mButton.setAlpha(enabled ? 1f : 0.5f);
    }

    private String runTmuxCommand(String[] arguments) {
        ExecutionCommand executionCommand = new ExecutionCommand(-1,
            TermuxConstants.TERMUX_BIN_PREFIX_DIR_PATH + "/tmux", arguments, null,
            null, ExecutionCommand.Runner.APP_SHELL.getName(), false);
        executionCommand.commandLabel = "tmux mouse-mode toggle";
        executionCommand.backgroundCustomLogLevel = Logger.LOG_LEVEL_OFF;
        HashMap<String, String> additionalEnvironment = new HashMap<>();
        additionalEnvironment.put(ENV_TMUX_TMPDIR, TMUX_SOCKET_DIRECTORY_PATH);
        AppShell appShell = AppShell.execute(mActivity, executionCommand, null,
            new TermuxShellEnvironment(), additionalEnvironment, true);
        if (appShell == null || !executionCommand.isSuccessful()
            || executionCommand.resultData.exitCode == null || executionCommand.resultData.exitCode != 0) {
            return null;
        }
        return executionCommand.resultData.stdout.toString();
    }
}
