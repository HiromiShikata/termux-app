package com.termux.app.terminal;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class ReversedListAdapterTest {

    private static class StringListAdapter extends BaseAdapter {

        private final List<String> items;

        StringListAdapter(List<String> items) {
            this.items = items;
        }

        @Override
        public int getCount() {
            return items.size();
        }

        @Override
        public Object getItem(int position) {
            return items.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            TextView textView = new TextView(parent.getContext());
            textView.setText(items.get(position));
            return textView;
        }
    }

    @Test
    public void presentsItemsInReversedOrder() {
        StringListAdapter delegate = new StringListAdapter(Arrays.asList("top", "middle", "bottom"));
        ReversedListAdapter reversed = new ReversedListAdapter(delegate);

        Assert.assertEquals(3, reversed.getCount());
        Assert.assertEquals("bottom", reversed.getItem(0));
        Assert.assertEquals("middle", reversed.getItem(1));
        Assert.assertEquals("top", reversed.getItem(2));
    }

    @Test
    public void mapsReversedPositionBackToDelegatePosition() {
        StringListAdapter delegate = new StringListAdapter(Arrays.asList("top", "middle", "bottom"));
        ReversedListAdapter reversed = new ReversedListAdapter(delegate);

        Assert.assertEquals(2, reversed.toDelegatePosition(0));
        Assert.assertEquals(1, reversed.toDelegatePosition(1));
        Assert.assertEquals(0, reversed.toDelegatePosition(2));
    }

    @Test
    public void getViewRendersTheReversedItem() {
        StringListAdapter delegate = new StringListAdapter(Arrays.asList("top", "middle", "bottom"));
        ReversedListAdapter reversed = new ReversedListAdapter(delegate);
        FrameLayout parent = new FrameLayout(RuntimeEnvironment.getApplication());

        TextView firstRow = (TextView) reversed.getView(0, null, parent);
        TextView lastRow = (TextView) reversed.getView(2, null, parent);

        Assert.assertEquals("bottom", firstRow.getText().toString());
        Assert.assertEquals("top", lastRow.getText().toString());
    }

    @Test
    public void propagatesDelegateDataSetChangeToObservers() {
        List<String> backingItems = new ArrayList<>(Arrays.asList("top", "bottom"));
        StringListAdapter delegate = new StringListAdapter(backingItems);
        ReversedListAdapter reversed = new ReversedListAdapter(delegate);
        boolean[] notified = {false};
        reversed.registerDataSetObserver(new android.database.DataSetObserver() {
            @Override
            public void onChanged() {
                notified[0] = true;
            }
        });

        backingItems.add("newest");
        delegate.notifyDataSetChanged();

        Assert.assertTrue(notified[0]);
        Assert.assertEquals("newest", reversed.getItem(0));
    }

    @Test
    public void detachStopsPropagatingDelegateChanges() {
        StringListAdapter delegate = new StringListAdapter(new ArrayList<>(Arrays.asList("top", "bottom")));
        ReversedListAdapter reversed = new ReversedListAdapter(delegate);
        boolean[] notified = {false};
        reversed.registerDataSetObserver(new android.database.DataSetObserver() {
            @Override
            public void onChanged() {
                notified[0] = true;
            }
        });

        reversed.detach();
        delegate.notifyDataSetChanged();

        Assert.assertFalse(notified[0]);
    }
}
