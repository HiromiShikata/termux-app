package com.termux.app.activities;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.termux.shared.interact.ShareUtils;

public class LongClickCopyPreference extends Preference {

    @Nullable
    private String copyText;

    @Nullable
    private CharSequence copyConfirmationToast;

    public LongClickCopyPreference(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public LongClickCopyPreference(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setCopyText(@Nullable String copyText) {
        this.copyText = copyText;
    }

    public void setCopyConfirmationToast(@Nullable CharSequence copyConfirmationToast) {
        this.copyConfirmationToast = copyConfirmationToast;
    }

    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        holder.itemView.setOnLongClickListener(view -> {
            if (copyText == null) return false;
            ShareUtils.copyTextToClipboard(getContext(), copyText,
                copyConfirmationToast == null ? null : copyConfirmationToast.toString());
            return true;
        });
    }
}
