package com.termux.app.ownercall;

import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.termux.R;

import java.util.List;

public final class OwnerCallDialogBinder {

    private static final float DISABLED_BUTTON_ALPHA = 0.3f;
    private static final float ENABLED_BUTTON_ALPHA = 1.0f;

    public interface OwnerCallDialogActions {

        void onPreviousCallRequested();

        void onNextCallRequested();

        void onDialogCloseRequested();
    }

    private OwnerCallDialogBinder() {
    }

    @NonNull
    public static OwnerCallDialogPaging bind(@NonNull View root,
                                             @NonNull List<OwnerCall> calls,
                                             int requestedIndex,
                                             long nowMillis,
                                             @NonNull OwnerCallDialogGeometry geometry,
                                             @Nullable OwnerCallDialogActions actions) {
        OwnerCallDialogPaging paging = OwnerCallDialogPaging.resolve(requestedIndex, calls.size());
        View dialog = root.findViewById(R.id.owner_call_dialog);
        if (dialog == null) {
            return paging;
        }
        if (calls.isEmpty()) {
            if (dialog.getVisibility() != View.GONE) {
                dialog.setVisibility(View.GONE);
            }
            return paging;
        }

        OwnerCall call = calls.get(paging.getIndex());
        applyGeometry(dialog, geometry);
        bindHeader(root, call, paging, nowMillis);
        bindBody(root, call);
        bindActions(root, call, paging, actions);
        dialog.setVisibility(View.VISIBLE);
        return paging;
    }

    private static void applyGeometry(@NonNull View dialog,
                                      @NonNull OwnerCallDialogGeometry geometry) {
        ViewGroup.MarginLayoutParams layoutParams =
            (ViewGroup.MarginLayoutParams) dialog.getLayoutParams();
        if (layoutParams.width == geometry.getWidthPixels()
            && layoutParams.height == geometry.getHeightPixels()
            && layoutParams.leftMargin == geometry.getLeftMarginPixels()
            && layoutParams.bottomMargin == geometry.getBottomMarginPixels()) {
            return;
        }
        layoutParams.width = geometry.getWidthPixels();
        layoutParams.height = geometry.getHeightPixels();
        layoutParams.leftMargin = geometry.getLeftMarginPixels();
        layoutParams.bottomMargin = geometry.getBottomMarginPixels();
        dialog.setLayoutParams(layoutParams);
    }

    private static void bindHeader(@NonNull View root, @NonNull OwnerCall call,
                                   @NonNull OwnerCallDialogPaging paging, long nowMillis) {
        setTextIfChanged(root.findViewById(R.id.owner_call_dialog_relative_time),
            OwnerCallCalledAt.describe(call.getCalledAt(), nowMillis));
        setTextIfChanged(root.findViewById(R.id.owner_call_dialog_position),
            paging.getPositionLabel());

        applyEnabledState(root.findViewById(R.id.owner_call_dialog_previous_button),
            paging.isPreviousEnabled());
        applyEnabledState(root.findViewById(R.id.owner_call_dialog_next_button),
            paging.isNextEnabled());
    }

    private static void applyEnabledState(@NonNull ImageButton button, boolean enabled) {
        button.setEnabled(enabled);
        button.setAlpha(enabled ? ENABLED_BUTTON_ALPHA : DISABLED_BUTTON_ALPHA);
    }

    private static void setTextIfChanged(@NonNull TextView view, @NonNull String text) {
        if (text.contentEquals(view.getText())) {
            return;
        }
        view.setText(text);
    }

    private static void bindBody(@NonNull View root, @NonNull OwnerCall call) {
        ScrollView bodyScroll = root.findViewById(R.id.owner_call_dialog_body_scroll);
        if (call.getCalledAt().equals(bodyScroll.getTag(R.id.owner_call_dialog_body_scroll))) {
            return;
        }
        TextView body = root.findViewById(R.id.owner_call_dialog_body);
        body.setText(call.getBody());
        bodyScroll.setTag(R.id.owner_call_dialog_body_scroll, call.getCalledAt());
        bodyScroll.scrollTo(0, 0);
    }

    private static void bindActions(@NonNull View root, @NonNull OwnerCall call,
                                    @NonNull OwnerCallDialogPaging paging,
                                    @Nullable OwnerCallDialogActions actions) {
        ImageButton previousButton = root.findViewById(R.id.owner_call_dialog_previous_button);
        ImageButton nextButton = root.findViewById(R.id.owner_call_dialog_next_button);
        ImageButton closeButton = root.findViewById(R.id.owner_call_dialog_close_button);
        if (actions == null) {
            previousButton.setOnClickListener(null);
            nextButton.setOnClickListener(null);
            closeButton.setOnClickListener(null);
            return;
        }
        previousButton.setOnClickListener(paging.isPreviousEnabled()
            ? view -> actions.onPreviousCallRequested() : null);
        nextButton.setOnClickListener(paging.isNextEnabled()
            ? view -> actions.onNextCallRequested() : null);
        closeButton.setOnClickListener(view -> actions.onDialogCloseRequested());
    }
}
