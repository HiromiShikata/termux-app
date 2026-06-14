package com.termux.view;

import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import com.termux.terminal.TerminalSession;

/**
 * The interface for communication between {@link TerminalView} and its client. It allows for getting
 * various  configuration options from the client and for sending back data to the client like logs,
 * key events, both hardware and IME (which makes it different from that available with
 * {@link View#setOnKeyListener(View.OnKeyListener)}, etc. It must be set for the
 * {@link TerminalView} through {@link TerminalView#setTerminalViewClient(TerminalViewClient)}.
 */
public interface TerminalViewClient {

    /**
     * Callback function on scale events according to {@link ScaleGestureDetector#getScaleFactor()}.
     */
    float onScale(float scale);



    /**
     * On a single tap on the terminal if terminal mouse reporting not enabled.
     */
    void onSingleTapUp(MotionEvent e);

    boolean shouldBackButtonBeMappedToEscape();

    boolean shouldEnforceCharBasedInput();

    boolean shouldUseCtrlSpaceWorkaround();

    boolean isTerminalViewSelected();



    void copyModeChanged(boolean copyMode);



    boolean onKeyDown(int keyCode, KeyEvent e, TerminalSession session);

    boolean onKeyUp(int keyCode, KeyEvent e);

    boolean onLongPress(MotionEvent event);

    /**
     * Called when the user taps the "Open" action in the text-selection toolbar for a selected URL.
     * Implementers should open the given URL and return true if it was handled.
     *
     * @param url the selected text that matched a web URL.
     * @return true if the URL was handled by the client, false otherwise.
     */
    default boolean onOpenSelectedUrlRequested(String url) {
        return false;
    }

    /**
     * Whether a single tap on a terminal cell that is on a URL should open that URL in the in-app
     * browser. When this returns true and the tapped cell is on a URL, the tap opens the URL and is
     * consumed, taking precedence over forwarding the tap to a program with mouse tracking active.
     *
     * @return true if tap-to-open-URL is enabled, false otherwise.
     */
    default boolean isTapToOpenUrlEnabled() {
        return true;
    }



    boolean readControlKey();

    boolean readAltKey();

    boolean readShiftKey();

    boolean readFnKey();



    boolean onCodePoint(int codePoint, boolean ctrlDown, TerminalSession session);


    void onEmulatorSet();


    void logError(String tag, String message);

    void logWarn(String tag, String message);

    void logInfo(String tag, String message);

    void logDebug(String tag, String message);

    void logVerbose(String tag, String message);

    void logStackTraceWithMessage(String tag, String message, Exception e);

    void logStackTrace(String tag, Exception e);

}
