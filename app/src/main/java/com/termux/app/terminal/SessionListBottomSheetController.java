package com.termux.app.terminal;

import android.graphics.Color;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.termux.R;
import com.termux.app.TermuxActivity;
import com.termux.shared.theme.NightMode;
import com.termux.shared.theme.ThemeUtils;

public class SessionListBottomSheetController {

    private final TermuxActivity mActivity;

    public SessionListBottomSheetController(@NonNull TermuxActivity activity) {
        this.mActivity = activity;
    }

    public void show() {
        TermuxSessionsListViewController listController = mActivity.getTermuxSessionListViewController();
        if (listController == null) {
            return;
        }

        boolean darkTheme = ThemeUtils.shouldEnableDarkTheme(mActivity, NightMode.getAppNightMode().getName());

        BottomSheetDialog dialog = new BottomSheetDialog(mActivity);
        dialog.getDelegate().setLocalNightMode(
            darkTheme ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);

        View content = dialog.getLayoutInflater().inflate(R.layout.bottom_sheet_session_list, null);
        content.setBackgroundColor(darkTheme ? Color.BLACK : Color.WHITE);
        dialog.setContentView(content);

        TextView titleView = content.findViewById(R.id.bottom_sheet_session_list_title);
        titleView.setTextColor(darkTheme ? Color.WHITE : Color.BLACK);

        ListView sessionListView = content.findViewById(R.id.bottom_sheet_session_list);
        ReversedListAdapter reversedAdapter = new ReversedListAdapter(listController);
        sessionListView.setAdapter(reversedAdapter);

        sessionListView.setOnItemClickListener((parent, view, position, id) -> {
            int delegatePosition = reversedAdapter.toDelegatePosition(position);
            boolean isSessionRow = !((SessionHierarchyRow) listController.getItem(delegatePosition)).isHeader();
            listController.onItemClick(parent, view, delegatePosition, id);
            if (isSessionRow) {
                dialog.dismiss();
            }
        });

        sessionListView.setOnItemLongClickListener((parent, view, position, id) ->
            listController.onItemLongClick(parent, view, reversedAdapter.toDelegatePosition(position), id));

        dialog.setOnDismissListener(d -> reversedAdapter.detach());

        dialog.show();
    }
}
