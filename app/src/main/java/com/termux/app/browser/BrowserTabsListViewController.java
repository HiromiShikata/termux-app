package com.termux.app.browser;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.termux.R;
import com.termux.app.TermuxActivity;

import java.util.List;

public class BrowserTabsListViewController extends ArrayAdapter<BrowserTab> implements AdapterView.OnItemClickListener {

    private static final int ACTIVE_TAB_TITLE_COLOR = 0xFF03A9F4;

    final TermuxActivity mActivity;

    final TermuxBrowserController mBrowserController;

    public BrowserTabsListViewController(@NonNull TermuxActivity activity,
                                         @NonNull TermuxBrowserController browserController,
                                         @NonNull List<BrowserTab> tabList) {
        super(activity.getApplicationContext(), R.layout.item_browser_tabs_list, tabList);
        this.mActivity = activity;
        this.mBrowserController = browserController;
    }

    @SuppressLint("SetTextI18n")
    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        View tabRowView = convertView;
        if (tabRowView == null) {
            LayoutInflater inflater = mActivity.getLayoutInflater();
            tabRowView = inflater.inflate(R.layout.item_browser_tabs_list, parent, false);
        }

        BrowserTab tab = getItem(position);
        if (tab == null) return tabRowView;

        TextView titleView = tabRowView.findViewById(R.id.browser_tab_title);
        TextView urlView = tabRowView.findViewById(R.id.browser_tab_url);
        ImageButton closeButton = tabRowView.findViewById(R.id.browser_tab_close_button);

        titleView.setText(tab.getTitle());
        urlView.setText(tab.getUrl());

        boolean isActive = tab == mBrowserController.getActiveTab();
        tabRowView.setActivated(isActive);
        titleView.setTextColor(isActive ? ACTIVE_TAB_TITLE_COLOR : titleView.getTextColors().getDefaultColor());

        closeButton.setOnClickListener(v -> mBrowserController.closeTab(tab));

        return tabRowView;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        BrowserTab clickedTab = getItem(position);
        if (clickedTab != null) mBrowserController.openTab(clickedTab);
    }
}
