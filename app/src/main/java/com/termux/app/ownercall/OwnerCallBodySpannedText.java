package com.termux.app.ownercall;

import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ClickableSpan;
import android.text.style.UnderlineSpan;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class OwnerCallBodySpannedText {

    public interface OwnerCallBodyTapActions {

        void onCopyableTextTapped(@NonNull String text);

        void onUrlTapped(@NonNull String url);
    }

    private OwnerCallBodySpannedText() {
    }

    @NonNull
    public static CharSequence of(@NonNull OwnerCallBodyDisplayText displayText,
                                  @Nullable OwnerCallBodyTapActions actions) {
        SpannableString spanned = new SpannableString(displayText.getText());
        if (actions == null) {
            return spanned;
        }
        for (OwnerCallBodyRange range : displayText.getCopyableRanges()) {
            applyTapSpan(spanned, range, () -> actions.onCopyableTextTapped(range.getText()));
        }
        for (OwnerCallBodyRange range : displayText.getUrlRanges()) {
            applyTapSpan(spanned, range, () -> actions.onUrlTapped(range.getText()));
        }
        return spanned;
    }

    private static void applyTapSpan(@NonNull SpannableString spanned,
                                     @NonNull OwnerCallBodyRange range,
                                     @NonNull Runnable onTapped) {
        spanned.setSpan(new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                onTapped.run();
            }
        }, range.getStartIndex(), range.getEndIndex(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        spanned.setSpan(new UnderlineSpan(), range.getStartIndex(), range.getEndIndex(),
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }
}
