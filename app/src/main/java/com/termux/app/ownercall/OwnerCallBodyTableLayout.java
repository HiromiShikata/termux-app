package com.termux.app.ownercall;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TableLayout;
import android.widget.TableRow;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

public final class OwnerCallBodyTableLayout extends TableLayout {

    @NonNull
    private int[] mColumnWeights = new int[0];

    public OwnerCallBodyTableLayout(@NonNull Context context) {
        super(context);
    }

    public void setColumnWeights(@NonNull int[] columnWeights) {
        mColumnWeights = columnWeights.clone();
        requestLayout();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int availableWidth = View.MeasureSpec.getSize(widthMeasureSpec)
            - getPaddingLeft() - getPaddingRight();
        if (availableWidth > 0 && mColumnWeights.length > 0) {
            assignColumnWidths(availableWidth);
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    private void assignColumnWidths(int availableWidth) {
        int weightSum = 0;
        for (int weight : mColumnWeights) {
            weightSum += weight;
        }
        int[] columnWidths = new int[mColumnWeights.length];
        int assigned = 0;
        for (int columnIndex = 0; columnIndex < mColumnWeights.length - 1; columnIndex++) {
            columnWidths[columnIndex] = availableWidth * mColumnWeights[columnIndex] / weightSum;
            assigned += columnWidths[columnIndex];
        }
        columnWidths[mColumnWeights.length - 1] = availableWidth - assigned;
        for (int rowIndex = 0; rowIndex < getChildCount(); rowIndex++) {
            TableRow row = (TableRow) getChildAt(rowIndex);
            for (int columnIndex = 0; columnIndex < row.getChildCount()
                && columnIndex < columnWidths.length; columnIndex++) {
                ViewGroup.LayoutParams layoutParams = row.getChildAt(columnIndex).getLayoutParams();
                layoutParams.width = columnWidths[columnIndex];
                layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT;
            }
        }
    }

    @NonNull
    static int[] columnWeightsOf(@NonNull OwnerCallBodyTable table, int minimumWeight,
                                 int maximumWeight) {
        List<List<OwnerCallBodyRange>> rows = new ArrayList<>();
        rows.add(table.getHeaderCells());
        rows.addAll(table.getRows());
        int[] weights = new int[table.getHeaderCells().size()];
        for (int columnIndex = 0; columnIndex < weights.length; columnIndex++) {
            int longest = 0;
            for (List<OwnerCallBodyRange> row : rows) {
                String cellText = row.get(columnIndex).getText();
                longest = Math.max(longest, cellText.codePointCount(0, cellText.length()));
            }
            weights[columnIndex] = Math.max(minimumWeight, Math.min(maximumWeight, longest));
        }
        return weights;
    }
}
