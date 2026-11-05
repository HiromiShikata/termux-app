package com.termux.app.ownercall;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.method.LinkMovementMethod;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.List;

public final class OwnerCallBodyTableView {

    private static final float CELL_PADDING_DP = 4f;
    private static final float BORDER_WIDTH_DP = 1f;
    private static final int BORDER_COLOR = 0xFF888888;

    private OwnerCallBodyTableView() {
    }

    @NonNull
    public static TableLayout create(@NonNull TextView textStyle,
                                     @NonNull OwnerCallBodyTable table,
                                     @NonNull CharSequence displayed) {
        TableLayout tableLayout = new TableLayout(textStyle.getContext());
        tableLayout.setStretchAllColumns(true);
        tableLayout.setShrinkAllColumns(true);
        tableLayout.addView(rowOf(textStyle, table.getHeaderCells(), table.getAlignments(),
            displayed, true));
        for (List<OwnerCallBodyRange> row : table.getRows()) {
            tableLayout.addView(rowOf(textStyle, row, table.getAlignments(), displayed, false));
        }
        return tableLayout;
    }

    @NonNull
    private static TableRow rowOf(@NonNull TextView textStyle,
                                  @NonNull List<OwnerCallBodyRange> cells,
                                  @NonNull List<OwnerCallBodyTableAlignment> alignments,
                                  @NonNull CharSequence displayed, boolean header) {
        TableRow row = new TableRow(textStyle.getContext());
        for (int columnIndex = 0; columnIndex < cells.size(); columnIndex++) {
            row.addView(cellOf(textStyle, cells.get(columnIndex),
                alignments.get(columnIndex), displayed, header));
        }
        return row;
    }

    @NonNull
    private static TextView cellOf(@NonNull TextView textStyle, @NonNull OwnerCallBodyRange cell,
                                   @NonNull OwnerCallBodyTableAlignment alignment,
                                   @NonNull CharSequence displayed, boolean header) {
        Context context = textStyle.getContext();
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        int padding = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
            CELL_PADDING_DP, metrics));
        GradientDrawable border = new GradientDrawable();
        border.setStroke(Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
            BORDER_WIDTH_DP, metrics)), BORDER_COLOR);

        TextView view = new TextView(context);
        view.setText(displayed.subSequence(cell.getStartIndex(), cell.getEndIndex()));
        view.setTextColor(textStyle.getCurrentTextColor());
        view.setTextSize(TypedValue.COMPLEX_UNIT_PX, textStyle.getTextSize());
        view.setTypeface(header ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
        view.setGravity(gravityOf(alignment));
        view.setBackground(border);
        view.setPadding(padding, padding, padding, padding);
        view.setMovementMethod(LinkMovementMethod.getInstance());
        return view;
    }

    private static int gravityOf(@NonNull OwnerCallBodyTableAlignment alignment) {
        switch (alignment) {
            case CENTER:
                return Gravity.CENTER_HORIZONTAL;
            case RIGHT:
                return Gravity.END;
            case LEFT:
            default:
                return Gravity.START;
        }
    }
}
