package com.termux.app.browser;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.termux.R;
import com.termux.shared.theme.NightMode;
import com.termux.shared.theme.ThemeUtils;

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
        ImageView faviconView = tabRowView.findViewById(R.id.browser_tab_favicon);

        titleView.setText(displayTitle(tab));
        urlView.setText(tab.getUrl());

        Bitmap favicon = tab.getFavicon();
        if (favicon != null) {
            faviconView.setImageBitmap(favicon);
        } else {
            faviconView.setImageBitmap(BrowserTabFaviconPlaceholder.letterBitmapForTab(tab));
        }

        boolean darkTheme = ThemeUtils.shouldEnableDarkTheme(getContext(), NightMode.getAppNightMode().getName());
        applyThemeAwareColors(tabRowView, titleView, urlView, tab, darkTheme);

        tabRowView.setOnClickListener(v -> mSelectionListener.openTab(tab));
        closeButton.setOnClickListener(v -> mSelectionListener.closeTab(tab));

        return tabRowView;
    }

    @NonNull
    private String displayTitle(@NonNull BrowserTab tab) {
        String title = tab.getTitle();
        return TextUtils.isEmpty(title) ? tab.getUrl() : title;
    }

    private void applyThemeAwareColors(@NonNull View tabRowView, @NonNull TextView titleView,
                                       @NonNull TextView urlView, @NonNull BrowserTab tab, boolean darkTheme) {
        boolean isActive = tab == mSelectionListener.getActiveTab();
        tabRowView.setActivated(isActive);
        tabRowView.setBackground(ContextCompat.getDrawable(getContext(), darkTheme
            ? R.drawable.session_background_black_selected
            : R.drawable.session_background_selected));

        int readableTextColor = darkTheme ? Color.WHITE : Color.BLACK;
        titleView.setTextColor(isActive ? ACTIVE_TAB_TITLE_COLOR : readableTextColor);
        urlView.setTextColor(readableTextColor);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        BrowserTab clickedTab = getItem(position);
        if (clickedTab != null) mSelectionListener.openTab(clickedTab);
    }
}
