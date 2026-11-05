package com.termux.app.ownercall;

import android.text.method.LinkMovementMethod;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

public final class OwnerCallBodySegmentView {

    private static final float TABLE_VERTICAL_MARGIN_DP = 6f;

    private OwnerCallBodySegmentView() {
    }

    @NonNull
    public static View create(@NonNull TextView textStyle, @NonNull OwnerCallBodySegment segment,
                              @NonNull CharSequence displayed) {
        if (segment.isTable()) {
            return OwnerCallBodyTableView.create(textStyle, segment.getTable(), displayed);
        }
        TextView text = new TextView(textStyle.getContext());
        text.setTextColor(textStyle.getCurrentTextColor());
        text.setTextSize(TypedValue.COMPLEX_UNIT_PX, textStyle.getTextSize());
        text.setMovementMethod(LinkMovementMethod.getInstance());
        text.setText(displayed.subSequence(segment.getStartIndex(), segment.getEndIndex()));
        return text;
    }

    @NonNull
    public static LinearLayout.LayoutParams layoutParamsOf(@NonNull TextView textStyle,
                                                           @NonNull OwnerCallBodySegment segment) {
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        if (segment.isTable()) {
            DisplayMetrics metrics = textStyle.getResources().getDisplayMetrics();
            int tableMargin = Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, TABLE_VERTICAL_MARGIN_DP, metrics));
            layoutParams.setMargins(0, tableMargin, 0, tableMargin);
        }
        return layoutParams;
    }
}
