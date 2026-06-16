package com.termux.app.terminal;

import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.termux.R;
import com.termux.app.TermuxActivity;
import com.termux.app.TermuxService;
import com.termux.shared.termux.settings.preferences.TermuxAppSharedPreferences;

import java.util.List;

public class SessionSwitchPickerController {

    static final long COMMIT_DELAY_MILLISECONDS = 1500;

    static final long INSTANT_MODE_HIDE_DELAY_MILLISECONDS = 1200;

    private static final String STORY_INDENT = "  ";
    private static final String SESSION_INDENT = "    ";
    private static final String SECONDARY_INDENT = "      ";
    private static final float STORY_RELATIVE_SIZE = 0.85f;
    private static final float SECONDARY_RELATIVE_SIZE = 0.65f;
    private static final String BELL_MARK = "🔔 ";
    private static final int STORY_TEXT_COLOR = 0xB3FFFFFF;
    private static final int SECONDARY_TEXT_COLOR = 0x99FFFFFF;
    private static final int HIGHLIGHT_BACKGROUND_ALPHA = 0x66;
    private static final int HIGHLIGHTED_SESSION_TEXT_COLOR = 0xFFFFFFFF;

    private final TermuxActivity mActivity;
    private final View mOverlayView;
    private final TextView mStructureView;
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final Runnable mCommitRunnable = this::commitAndHide;
    private final Runnable mHideRunnable = this::hide;

    private boolean mShowing;
    private int mHighlightedSessionIndex = -1;

    public SessionSwitchPickerController(@NonNull TermuxActivity activity) {
        this.mActivity = activity;
        this.mOverlayView = activity.findViewById(R.id.session_switch_picker_overlay);
        this.mStructureView = activity.findViewById(R.id.session_switch_picker_structure);
    }

    public boolean isShowing() {
        return mShowing;
    }

    public void onVolumeKeyDirection(boolean forward) {
        TermuxService service = mActivity.getTermuxService();
        TermuxSessionsListViewController listController = mActivity.getTermuxSessionListViewController();
        if (service == null || listController == null) {
            return;
        }
        int currentSessionIndex = service.getIndexOfSession(mActivity.getCurrentSession());
        List<Integer> navigableSessionIndexes = listController.getNavigableSessionIndexes();
        VolumeKeyPickerMoveDecision decision = VolumeKeyPickerMoveDecision.decide(
            isPreviewFirstEnabled(), mShowing, mHighlightedSessionIndex, currentSessionIndex,
            navigableSessionIndexes, forward);
        mHighlightedSessionIndex = decision.getHighlightedSessionIndex();
        mShowing = true;
        renderStructure(listController);
        mOverlayView.setVisibility(View.VISIBLE);
        if (decision.shouldSwitchImmediately()) {
            switchToHighlightedSession();
            scheduleHide();
        } else {
            scheduleCommit();
        }
    }

    public void commitAndHide() {
        if (!mShowing) {
            return;
        }
        int targetSessionIndex = mHighlightedSessionIndex;
        boolean previewFirst = isPreviewFirstEnabled();
        hide();
        if (previewFirst && targetSessionIndex >= 0) {
            mActivity.getTermuxTerminalSessionClient().switchToSession(targetSessionIndex);
        }
    }

    private void switchToHighlightedSession() {
        if (mHighlightedSessionIndex >= 0) {
            mActivity.getTermuxTerminalSessionClient().switchToSession(mHighlightedSessionIndex);
        }
    }

    private boolean isPreviewFirstEnabled() {
        TermuxAppSharedPreferences preferences = mActivity.getPreferences();
        return preferences != null && preferences.isSessionSwitchPreviewFirstEnabled();
    }

    private void hide() {
        mShowing = false;
        mHighlightedSessionIndex = -1;
        mHandler.removeCallbacks(mCommitRunnable);
        mHandler.removeCallbacks(mHideRunnable);
        mOverlayView.setVisibility(View.GONE);
    }

    private void scheduleCommit() {
        mHandler.removeCallbacks(mCommitRunnable);
        mHandler.removeCallbacks(mHideRunnable);
        mHandler.postDelayed(mCommitRunnable, COMMIT_DELAY_MILLISECONDS);
    }

    private void scheduleHide() {
        mHandler.removeCallbacks(mCommitRunnable);
        mHandler.removeCallbacks(mHideRunnable);
        mHandler.postDelayed(mHideRunnable, INSTANT_MODE_HIDE_DELAY_MILLISECONDS);
    }

    private void renderStructure(@NonNull TermuxSessionsListViewController listController) {
        List<SessionPickerOverlayLine> lines = SessionPickerOverlayRenderModel.build(
            listController.getVisibleRows(), listController.getSessionRawNames(),
            listController.getSessionTitles(), listController.getMarkedSessionIndexes(),
            mHighlightedSessionIndex);
        mStructureView.setText(buildStructureText(lines));
    }

    private CharSequence buildStructureText(@NonNull List<SessionPickerOverlayLine> lines) {
        SpannableStringBuilder builder = new SpannableStringBuilder();
        int highlightColor = ContextCompat.getColor(mActivity, R.color.session_active_indicator);
        for (int lineIndex = 0; lineIndex < lines.size(); lineIndex++) {
            SessionPickerOverlayLine line = lines.get(lineIndex);
            if (lineIndex > 0) {
                builder.append('\n');
            }
            int start = builder.length();
            switch (line.getKind()) {
                case PROJECT:
                    builder.append(line.getText());
                    builder.setSpan(new StyleSpan(Typeface.BOLD), start, builder.length(),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    break;
                case STORY:
                    builder.append(STORY_INDENT).append(line.getText());
                    builder.setSpan(new RelativeSizeSpan(STORY_RELATIVE_SIZE), start, builder.length(),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    builder.setSpan(new ForegroundColorSpan(STORY_TEXT_COLOR), start, builder.length(),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    break;
                case SESSION:
                default:
                    appendSessionLine(builder, line, start, highlightColor);
                    break;
            }
        }
        return builder;
    }

    private void appendSessionLine(@NonNull SpannableStringBuilder builder, @NonNull SessionPickerOverlayLine line,
                                   int start, int highlightColor) {
        builder.append(SESSION_INDENT);
        if (line.isMarked()) {
            builder.append(BELL_MARK);
        }
        builder.append(line.getText());
        if (line.isHighlighted()) {
            int highlightBackground = (HIGHLIGHT_BACKGROUND_ALPHA << 24) | (highlightColor & 0x00FFFFFF);
            builder.setSpan(new BackgroundColorSpan(highlightBackground), start, builder.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            builder.setSpan(new StyleSpan(Typeface.BOLD), start, builder.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            builder.setSpan(new ForegroundColorSpan(HIGHLIGHTED_SESSION_TEXT_COLOR), start, builder.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        String secondaryText = line.getSecondaryText();
        if (!secondaryText.isEmpty()) {
            builder.append('\n');
            int secondaryStart = builder.length();
            builder.append(SECONDARY_INDENT).append(secondaryText);
            builder.setSpan(new RelativeSizeSpan(SECONDARY_RELATIVE_SIZE), secondaryStart, builder.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            builder.setSpan(new ForegroundColorSpan(SECONDARY_TEXT_COLOR), secondaryStart, builder.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }
}
