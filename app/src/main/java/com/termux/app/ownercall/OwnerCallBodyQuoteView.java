package com.termux.app.ownercall;

import android.content.Context;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.List;

public final class OwnerCallBodyQuoteView {

    public static final String TAG = "owner-call-body-quote";

    private static final float BAR_WIDTH_DP = 3f;
    private static final float CONTENT_START_PADDING_DP = 10f;
    private static final int BAR_COLOR = 0xFF888888;

    private OwnerCallBodyQuoteView() {
    }

    @NonNull
    public static LinearLayout create(@NonNull TextView textStyle,
                                      @NonNull List<OwnerCallBodySegment> quotedSegments,
                                      @NonNull CharSequence displayed) {
        Context context = textStyle.getContext();
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        LinearLayout quote = new LinearLayout(context);
        quote.setOrientation(LinearLayout.HORIZONTAL);
        quote.setTag(TAG);

        View bar = new View(context);
        bar.setBackgroundColor(BAR_COLOR);
        quote.addView(bar, new LinearLayout.LayoutParams(
            Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, BAR_WIDTH_DP,
                metrics)), ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);
        int contentPadding = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
            CONTENT_START_PADDING_DP, metrics));
        content.setPadding(contentPadding, 0, 0, 0);
        for (OwnerCallBodySegment segment : quotedSegments) {
            content.addView(OwnerCallBodySegmentView.create(textStyle, segment, displayed),
                OwnerCallBodySegmentView.layoutParamsOf(textStyle, segment));
        }
        quote.addView(content, new LinearLayout.LayoutParams(0,
            ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        return quote;
    }
}
