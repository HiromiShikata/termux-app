package com.termux.app.handedness;

import android.view.View;
import android.widget.BaseAdapter;
import android.widget.ListView;

import com.termux.R;

public final class BrowserTabsListHandednessPart implements HandednessLayoutPart {

    private final View activityRoot;

    public BrowserTabsListHandednessPart(View activityRoot) {
        this.activityRoot = activityRoot;
    }

    @Override
    public void applyHandedness(Handedness handedness) {
        ListView listView = activityRoot.findViewById(R.id.browser_tabs_list);
        if (listView != null && listView.getAdapter() instanceof BaseAdapter) {
            ((BaseAdapter) listView.getAdapter()).notifyDataSetChanged();
        }
    }

}
