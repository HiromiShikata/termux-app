package com.termux.app.terminal;

import android.database.DataSetObserver;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import androidx.annotation.NonNull;

public class ReversedListAdapter extends BaseAdapter {

    private final BaseAdapter mDelegate;

    private final DataSetObserver mDelegateObserver = new DataSetObserver() {
        @Override
        public void onChanged() {
            notifyDataSetChanged();
        }

        @Override
        public void onInvalidated() {
            notifyDataSetInvalidated();
        }
    };

    public ReversedListAdapter(@NonNull BaseAdapter delegate) {
        this.mDelegate = delegate;
        mDelegate.registerDataSetObserver(mDelegateObserver);
    }

    public void detach() {
        mDelegate.unregisterDataSetObserver(mDelegateObserver);
    }

    public int toDelegatePosition(int position) {
        return mDelegate.getCount() - 1 - position;
    }

    @Override
    public int getCount() {
        return mDelegate.getCount();
    }

    @Override
    public Object getItem(int position) {
        return mDelegate.getItem(toDelegatePosition(position));
    }

    @Override
    public long getItemId(int position) {
        return mDelegate.getItemId(toDelegatePosition(position));
    }

    @Override
    public boolean hasStableIds() {
        return mDelegate.hasStableIds();
    }

    @Override
    public int getViewTypeCount() {
        return mDelegate.getViewTypeCount();
    }

    @Override
    public int getItemViewType(int position) {
        return mDelegate.getItemViewType(toDelegatePosition(position));
    }

    @Override
    public boolean areAllItemsEnabled() {
        return mDelegate.areAllItemsEnabled();
    }

    @Override
    public boolean isEnabled(int position) {
        return mDelegate.isEnabled(toDelegatePosition(position));
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        return mDelegate.getView(toDelegatePosition(position), convertView, parent);
    }
}
