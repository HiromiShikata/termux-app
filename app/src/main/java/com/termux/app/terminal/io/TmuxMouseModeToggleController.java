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

public class TmuxMouseModeToggleController {

    private final TermuxActivity mActivity;
    private final ImageButton mButton;

    public TmuxMouseModeToggleController(@NonNull TermuxActivity activity, @NonNull ImageButton button) {
        this.mActivity = activity;
        this.mButton = button;

        initState();
        applyAppearance();
        mButton.setOnClickListener(view -> toggle());
    }

    private void initState() {
        String stdout = runTmuxCommandSynchronously(TmuxMouseModeController.SHOW_MOUSE_ARGUMENTS);
        boolean fallback = mActivity.getPreferences().isTmuxMouseModeEnabled();
        boolean enabled = TmuxMouseModeController.parseShowMouseOutput(stdout, fallback);
        mActivity.getPreferences().setTmuxMouseModeEnabled(enabled);
    }

    private void toggle() {
        boolean targetEnabled = TmuxMouseModeController.nextState(mActivity.getPreferences().isTmuxMouseModeEnabled());
        runTmuxCommandInBackground(TmuxMouseModeController.setMouseArguments(targetEnabled));
        mActivity.getPreferences().setTmuxMouseModeEnabled(targetEnabled);
        applyAppearance();
        Logger.showToast(mActivity, mActivity.getString(targetEnabled
            ? R.string.msg_tmux_mouse_mode_on : R.string.msg_tmux_mouse_mode_off), false);
    }

    private void applyAppearance() {
        boolean enabled = mActivity.getPreferences().isTmuxMouseModeEnabled();
        int tint = mActivity.getResources().getColor(enabled
            ? android.R.color.holo_blue_light : android.R.color.white);
        mButton.setColorFilter(tint);
        mButton.setAlpha(enabled ? 1f : 0.5f);
    }

    private String runTmuxCommandSynchronously(String[] arguments) {
        ExecutionCommand executionCommand = buildTmuxExecutionCommand(arguments);
        AppShell appShell = AppShell.execute(mActivity, executionCommand, null,
            new TermuxShellEnvironment(), null, true);
        if (appShell == null || !executionCommand.isSuccessful()
            || executionCommand.resultData.exitCode == null || executionCommand.resultData.exitCode != 0) {
            return null;
        }
        return executionCommand.resultData.stdout.toString();
    }

    private void runTmuxCommandInBackground(String[] arguments) {
        AppShell.execute(mActivity, buildTmuxExecutionCommand(arguments), null,
            new TermuxShellEnvironment(), null, false);
    }

    private ExecutionCommand buildTmuxExecutionCommand(String[] arguments) {
        ExecutionCommand executionCommand = new ExecutionCommand(-1,
            TermuxConstants.TERMUX_BIN_PREFIX_DIR_PATH + "/tmux", arguments, null,
            null, ExecutionCommand.Runner.APP_SHELL.getName(), false);
        executionCommand.commandLabel = "tmux mouse-mode toggle";
        executionCommand.backgroundCustomLogLevel = Logger.LOG_LEVEL_OFF;
        return executionCommand;
    }
}
