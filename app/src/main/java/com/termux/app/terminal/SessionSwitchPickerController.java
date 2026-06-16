package com.termux.app.terminal;

import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.os.Build;
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

import java.util.List;

public class SessionSwitchPickerController {

    static final long COMMIT_DELAY_MILLISECONDS = 1500;

    private static final float BACKGROUND_BLUR_RADIUS_PIXELS = 24f;
    private static final String STORY_INDENT = "  ";
    private static final String SESSION_INDENT = "    ";
    private static final float STORY_RELATIVE_SIZE = 0.85f;
    private static final int STORY_TEXT_COLOR = 0xB3FFFFFF;
    private static final int HIGHLIGHT_BACKGROUND_ALPHA = 0x66;
    private static final int HIGHLIGHTED_SESSION_TEXT_COLOR = 0xFFFFFFFF;

    private final TermuxActivity mActivity;
    private final View mOverlayView;
    private final View mDimScrimView;
    private final TextView mStructureView;
    private final View mBlurTargetView;
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final Runnable mCommitRunnable = this::commitAndHide;

    private boolean mShowing;
    private int mHighlightedSessionIndex = -1;

    public SessionSwitchPickerController(@NonNull TermuxActivity activity) {
        this.mActivity = activity;
        this.mOverlayView = activity.findViewById(R.id.session_switch_picker_overlay);
        this.mDimScrimView = activity.findViewById(R.id.session_switch_picker_dim_scrim);
        this.mStructureView = activity.findViewById(R.id.session_switch_picker_structure);
        this.mBlurTargetView = activity.findViewById(R.id.main_content_with_session_name_bar);
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
        List<Integer> visibleSessionIndexes = listController.getVisibleSessionIndexes();
        mHighlightedSessionIndex = VolumeKeyPickerStep.nextHighlightedSessionIndex(
            mShowing, mHighlightedSessionIndex, currentSessionIndex, visibleSessionIndexes, forward);
        if (!mShowing) {
            mShowing = true;
            applyBackgroundBlur();
        }
        renderStructure(listController);
        mOverlayView.setVisibility(View.VISIBLE);
        scheduleCommit();
    }

    public void commitAndHide() {
        if (!mShowing) {
            return;
        }
        int targetSessionIndex = mHighlightedSessionIndex;
        hide();
        if (targetSessionIndex >= 0) {
            mActivity.getTermuxTerminalSessionClient().switchToSession(targetSessionIndex);
        }
    }

    private void hide() {
        mShowing = false;
        mHighlightedSessionIndex = -1;
        mHandler.removeCallbacks(mCommitRunnable);
        mOverlayView.setVisibility(View.GONE);
        clearBackgroundBlur();
    }

    private void scheduleCommit() {
        mHandler.removeCallbacks(mCommitRunnable);
        mHandler.postDelayed(mCommitRunnable, COMMIT_DELAY_MILLISECONDS);
    }

    private void renderStructure(@NonNull TermuxSessionsListViewController listController) {
        List<SessionPickerOverlayLine> lines = SessionPickerOverlayRenderModel.build(
            listController.getVisibleRows(), listController.getSessionDisplayNames(), mHighlightedSessionIndex);
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
                    builder.append(SESSION_INDENT).append(line.getText());
                    if (line.isHighlighted()) {
                        int highlightBackground = (HIGHLIGHT_BACKGROUND_ALPHA << 24) | (highlightColor & 0x00FFFFFF);
                        builder.setSpan(new BackgroundColorSpan(highlightBackground), start, builder.length(),
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        builder.setSpan(new StyleSpan(Typeface.BOLD), start, builder.length(),
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        builder.setSpan(new ForegroundColorSpan(HIGHLIGHTED_SESSION_TEXT_COLOR), start, builder.length(),
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                    break;
            }
        }
        return builder;
    }

    private void applyBackgroundBlur() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && mBlurTargetView != null) {
            mBlurTargetView.setRenderEffect(RenderEffect.createBlurEffect(
                BACKGROUND_BLUR_RADIUS_PIXELS, BACKGROUND_BLUR_RADIUS_PIXELS, Shader.TileMode.CLAMP));
            mDimScrimView.setVisibility(View.GONE);
        } else {
            mDimScrimView.setVisibility(View.VISIBLE);
        }
    }

    private void clearBackgroundBlur() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && mBlurTargetView != null) {
            mBlurTargetView.setRenderEffect(null);
        }
        mDimScrimView.setVisibility(View.GONE);
    }
}
