package com.termux.app.browser;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.termux.R;

import java.util.List;

public class BrowserTabsListViewController extends ArrayAdapter<BrowserTab> implements AdapterView.OnItemClickListener {

    private static final int ACTIVE_TAB_TITLE_COLOR = 0xFF03A9F4;

    final BrowserTabSelectionListener mSelectionListener;

    public BrowserTabsListViewController(@NonNull Context context,
                                         @NonNull BrowserTabSelectionListener selectionListener,
                                         @NonNull List<BrowserTab> tabList) {
        super(context.getApplicationContext(), R.layout.item_browser_tabs_list, tabList);
        this.mSelectionListener = selectionListener;
    }

    @SuppressLint("SetTextI18n")
    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        View tabRowView = convertView;
        if (tabRowView == null) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            tabRowView = inflater.inflate(R.layout.item_browser_tabs_list, parent, false);
        }

        BrowserTab tab = getItem(position);
        if (tab == null) return tabRowView;

        TextView titleView = tabRowView.findViewById(R.id.browser_tab_title);
        TextView urlView = tabRowView.findViewById(R.id.browser_tab_url);
        ImageButton closeButton = tabRowView.findViewById(R.id.browser_tab_close_button);

        titleView.setText(tab.getTitle());
        urlView.setText(tab.getUrl());

        boolean isActive = tab == mSelectionListener.getActiveTab();
        tabRowView.setActivated(isActive);
        titleView.setTextColor(isActive ? ACTIVE_TAB_TITLE_COLOR : titleView.getTextColors().getDefaultColor());

        tabRowView.setOnClickListener(v -> mSelectionListener.openTab(tab));
        closeButton.setOnClickListener(v -> mSelectionListener.closeTab(tab));

        return tabRowView;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        BrowserTab clickedTab = getItem(position);
        if (clickedTab != null) mSelectionListener.openTab(clickedTab);
    }
}
