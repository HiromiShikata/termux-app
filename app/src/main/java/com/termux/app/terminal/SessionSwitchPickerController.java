package com.termux.app.terminal;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.text.Layout;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.method.ScrollingMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.text.style.LeadingMarginSpan;
import android.text.style.LineBackgroundSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
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

    private static final String STORY_PREFIX = "▸ ";
    private static final float STORY_RELATIVE_SIZE = 0.7f;
    private static final float SPACER_RELATIVE_SIZE = 0.4f;
    private static final float SECONDARY_RELATIVE_SIZE = 0.65f;
    private static final int TRANSPARENT_COLOR = 0x00000000;
    private static final int STORY_TEXT_COLOR = 0x99FFFFFF;
    private static final int SECONDARY_TEXT_COLOR = 0x99FFFFFF;
    private static final int HIGHLIGHT_BACKGROUND_ALPHA = 0x66;
    private static final int HIGHLIGHTED_SESSION_TEXT_COLOR = 0xFFFFFFFF;
    private static final float SESSION_INDENT_DP = 12f;
    private static final float OVERLAY_FIXED_WIDTH_DP = 240f;
    private static final float OVERLAY_FIXED_HEIGHT_DP = 320f;

    private final TermuxActivity mActivity;
    private final View mOverlayView;
    private final TextView mStructureView;
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final Runnable mCommitRunnable = this::commitAndHide;
    private final Runnable mHideRunnable = this::hide;

    private boolean mShowing;
    private int mHighlightedSessionIndex = -1;
    private int mHighlightedTextOffset = -1;

    public SessionSwitchPickerController(@NonNull TermuxActivity activity) {
        this.mActivity = activity;
        this.mOverlayView = activity.findViewById(R.id.session_switch_picker_overlay);
        this.mStructureView = activity.findViewById(R.id.session_switch_picker_structure);
        this.mStructureView.setMovementMethod(new ScrollingMovementMethod());
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
        List<Integer> orderedSessionIndexes = listController.getOrderedSessionIndexes();
        List<Integer> navigableSessionIndexes = listController.getNavigationCandidateSessionIndexes();
        VolumeKeyPickerMoveDecision decision = VolumeKeyPickerMoveDecision.decide(
            isPreviewFirstEnabled(), mShowing, mHighlightedSessionIndex, currentSessionIndex,
            orderedSessionIndexes, navigableSessionIndexes, forward);
        mHighlightedSessionIndex = decision.getHighlightedSessionIndex();
        mShowing = true;
        VolumeKeyPickerPresentation.present(
            decision,
            this::switchToHighlightedSession,
            () -> renderStructure(listController),
            () -> mOverlayView.setVisibility(View.VISIBLE),
            this::scheduleHide,
            this::scheduleCommit);
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
        mHighlightedTextOffset = -1;
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
            listController.getVisibleRows(), listController.getSessionRows(), mHighlightedSessionIndex);
        mStructureView.setText(buildStructureText(lines));
        applyFixedOverlaySize();
        scrollHighlightedLineIntoView();
    }

    private void scrollHighlightedLineIntoView() {
        final int highlightedTextOffset = mHighlightedTextOffset;
        if (highlightedTextOffset < 0) {
            return;
        }
        mStructureView.post(() -> {
            Layout layout = mStructureView.getLayout();
            if (layout == null || highlightedTextOffset > layout.getText().length()) {
                return;
            }
            int highlightedLine = layout.getLineForOffset(highlightedTextOffset);
            int highlightedLineTop = layout.getLineTop(highlightedLine);
            int highlightedLineBottom = layout.getLineBottom(highlightedLine);
            int viewportHeight = mStructureView.getHeight()
                - mStructureView.getPaddingTop() - mStructureView.getPaddingBottom();
            int contentHeight = layout.getHeight();
            int targetScrollY = SessionPickerScrollOffset.targetScrollY(
                highlightedLineTop, highlightedLineBottom, viewportHeight, contentHeight,
                mStructureView.getScrollY());
            mStructureView.scrollTo(mStructureView.getScrollX(), targetScrollY);
        });
    }

    private void applyFixedOverlaySize() {
        DisplayMetrics displayMetrics = mActivity.getResources().getDisplayMetrics();
        int fixedWidthPixels = overlayWidthPx(displayMetrics);
        int fixedHeightPixels = overlayHeightPx(displayMetrics);
        ViewGroup.LayoutParams layoutParams = mStructureView.getLayoutParams();
        if (layoutParams.width != fixedWidthPixels || layoutParams.height != fixedHeightPixels) {
            layoutParams.width = fixedWidthPixels;
            layoutParams.height = fixedHeightPixels;
            mStructureView.setLayoutParams(layoutParams);
        }
    }

    static int overlayWidthPx(@NonNull DisplayMetrics displayMetrics) {
        return dimensionPx(OVERLAY_FIXED_WIDTH_DP, displayMetrics);
    }

    static int overlayHeightPx(@NonNull DisplayMetrics displayMetrics) {
        return dimensionPx(OVERLAY_FIXED_HEIGHT_DP, displayMetrics);
    }

    static int overlayHeightPxForLineCount(int lineCount, @NonNull DisplayMetrics displayMetrics) {
        return overlayHeightPx(displayMetrics);
    }

    static int overlayWidthPxForLineCount(int lineCount, @NonNull DisplayMetrics displayMetrics) {
        return overlayWidthPx(displayMetrics);
    }

    private static int dimensionPx(float dp, @NonNull DisplayMetrics displayMetrics) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, displayMetrics));
    }

    private CharSequence buildStructureText(@NonNull List<SessionPickerOverlayLine> lines) {
        SpannableStringBuilder builder = new SpannableStringBuilder();
        mHighlightedTextOffset = -1;
        int highlightColor = ContextCompat.getColor(mActivity, R.color.session_active_indicator);
        int sessionIndentPixels = Math.round(TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, SESSION_INDENT_DP, mActivity.getResources().getDisplayMetrics()));
        for (int lineIndex = 0; lineIndex < lines.size(); lineIndex++) {
            SessionPickerOverlayLine line = lines.get(lineIndex);
            if (lineIndex > 0) {
                builder.append('\n');
            }
            int start = builder.length();
            if (line.isHighlighted()) {
                mHighlightedTextOffset = start;
            }
            switch (line.getKind()) {
                case PROJECT:
                    builder.append(line.getText());
                    builder.setSpan(new StyleSpan(Typeface.BOLD), start, builder.length(),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    break;
                case STORY:
                    builder.append(STORY_PREFIX).append(line.getText());
                    builder.setSpan(new RelativeSizeSpan(STORY_RELATIVE_SIZE), start, builder.length(),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    builder.setSpan(new ForegroundColorSpan(STORY_TEXT_COLOR), start, builder.length(),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    break;
                case SPACER:
                    builder.append(' ');
                    builder.setSpan(new RelativeSizeSpan(SPACER_RELATIVE_SIZE), start, builder.length(),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    break;
                case SESSION:
                default:
                    appendSessionLine(builder, line, start, highlightColor, sessionIndentPixels);
                    break;
            }
        }
        return builder;
    }

    @NonNull
    static String bellMarkSlotText() {
        return SessionRow.BELL_MARK;
    }

    static boolean isBellMarkSlotVisible(boolean marked) {
        return marked;
    }

    @NonNull
    static String newActivityLabelSlotText(@NonNull String newActivityLabel) {
        if (newActivityLabel.isEmpty()) {
            return "";
        }
        return SessionRow.NEW_ACTIVITY_LABEL_PREFIX + newActivityLabel;
    }

    @NonNull
    static String pickerStructurePlainText(@NonNull List<SessionPickerOverlayLine> lines) {
        StringBuilder text = new StringBuilder();
        for (int lineIndex = 0; lineIndex < lines.size(); lineIndex++) {
            SessionPickerOverlayLine line = lines.get(lineIndex);
            if (lineIndex > 0) {
                text.append('\n');
            }
            switch (line.getKind()) {
                case PROJECT:
                    text.append(line.getText());
                    break;
                case STORY:
                    text.append(STORY_PREFIX).append(line.getText());
                    break;
                case SPACER:
                    text.append(' ');
                    break;
                case SESSION:
                default:
                    text.append(bellMarkSlotText()).append(line.getText())
                        .append(newActivityLabelSlotText(line.getNewActivityLabel()));
                    if (!line.getSecondaryText().isEmpty()) {
                        text.append('\n').append(line.getSecondaryText());
                    }
                    break;
            }
        }
        return text.toString();
    }

    private void appendSessionLine(@NonNull SpannableStringBuilder builder, @NonNull SessionPickerOverlayLine line,
                                   int start, int highlightColor, int sessionIndentPixels) {
        int bellMarkStart = builder.length();
        builder.append(bellMarkSlotText());
        if (!isBellMarkSlotVisible(line.isMarked())) {
            builder.setSpan(new ForegroundColorSpan(TRANSPARENT_COLOR), bellMarkStart, builder.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        int nameStart = builder.length();
        builder.append(line.getText());
        builder.setSpan(new RelativeSizeSpan(SessionRow.SESSION_NAME_RELATIVE_SIZE), nameStart, builder.length(),
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        if (line.isCurrent()) {
            builder.setSpan(new StyleSpan(Typeface.BOLD), nameStart, builder.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            builder.setSpan(new ForegroundColorSpan(highlightColor), nameStart, builder.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        String newActivityLabel = line.getNewActivityLabel();
        if (!newActivityLabel.isEmpty()) {
            int ageLabelStart = builder.length();
            builder.append(newActivityLabelSlotText(newActivityLabel));
            builder.setSpan(new RelativeSizeSpan(SECONDARY_RELATIVE_SIZE), ageLabelStart, builder.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            builder.setSpan(new ForegroundColorSpan(SECONDARY_TEXT_COLOR), ageLabelStart, builder.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        if (line.isHighlighted()) {
            builder.setSpan(new StyleSpan(Typeface.BOLD), start, builder.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            builder.setSpan(new ForegroundColorSpan(HIGHLIGHTED_SESSION_TEXT_COLOR), start, builder.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        String secondaryText = line.getSecondaryText();
        if (!secondaryText.isEmpty()) {
            builder.append('\n');
            int secondaryStart = builder.length();
            builder.append(secondaryText);
            builder.setSpan(new RelativeSizeSpan(SECONDARY_RELATIVE_SIZE), secondaryStart, builder.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            builder.setSpan(new ForegroundColorSpan(SECONDARY_TEXT_COLOR), secondaryStart, builder.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        builder.setSpan(new LeadingMarginSpan.Standard(sessionIndentPixels), start, builder.length(),
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        if (line.isHighlighted()) {
            int highlightBackground = (HIGHLIGHT_BACKGROUND_ALPHA << 24) | (highlightColor & 0x00FFFFFF);
            builder.setSpan(new FullWidthHighlightSpan(highlightBackground, mStructureView.getPaddingLeft(),
                mStructureView.getPaddingRight()), start, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    private static final class FullWidthHighlightSpan implements LineBackgroundSpan {

        private final Paint mFillPaint = new Paint();
        private final int mPaddingLeft;
        private final int mPaddingRight;

        FullWidthHighlightSpan(int color, int paddingLeft, int paddingRight) {
            mFillPaint.setColor(color);
            mFillPaint.setStyle(Paint.Style.FILL);
            mPaddingLeft = paddingLeft;
            mPaddingRight = paddingRight;
        }

        @Override
        public void drawBackground(@NonNull Canvas canvas, @NonNull Paint paint, int left, int right,
                                   int top, int baseline, int bottom, @NonNull CharSequence text,
                                   int start, int end, int lineNumber) {
            canvas.drawRect(left - mPaddingLeft, top, right + mPaddingRight, bottom, mFillPaint);
        }
    }
}
