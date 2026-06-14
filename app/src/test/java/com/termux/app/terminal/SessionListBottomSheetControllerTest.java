package com.termux.app.terminal;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.Arrays;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class SessionListBottomSheetControllerTest {

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
    public void bindsSessionListInSameOrderAsLeftDrawerWithoutReversing() {
        ListView listView = new ListView(RuntimeEnvironment.getApplication());
        StringListAdapter delegate =
            new StringListAdapter(Arrays.asList("project header", "session a", "session b"));

        SessionListBottomSheetController.bindSessionListAdapter(listView, delegate);

        Assert.assertSame(delegate, listView.getAdapter());
        Assert.assertEquals("project header", listView.getAdapter().getItem(0));
        Assert.assertEquals("session a", listView.getAdapter().getItem(1));
        Assert.assertEquals("session b", listView.getAdapter().getItem(2));
    }

    @Test
    public void togglingFromHiddenOpensSheet() {
        Assert.assertEquals(View.VISIBLE, SessionListBottomSheetController.nextSheetVisibility(View.GONE));
    }

    @Test
    public void togglingFromVisibleClosesSheet() {
        Assert.assertEquals(View.GONE, SessionListBottomSheetController.nextSheetVisibility(View.VISIBLE));
    }

    @Test
    public void scrimIsVisibleWhileSheetIsOpenSoOutsideTapsCanDismissIt() {
        Assert.assertEquals(View.VISIBLE, SessionListBottomSheetController.scrimVisibilityForSheet(View.VISIBLE));
    }

    @Test
    public void scrimIsGoneWhileSheetIsClosedSoItDoesNotBlockInteraction() {
        Assert.assertEquals(View.GONE, SessionListBottomSheetController.scrimVisibilityForSheet(View.GONE));
    }

    @Test
    public void capsSheetHeightToOneThirdOfScreen() {
        Assert.assertEquals(640, SessionListBottomSheetController.computeSheetMaxHeight(1920));
    }

    @Test
    public void capsSheetHeightWithFlooredDivisionForNonDivisibleScreen() {
        Assert.assertEquals(800, SessionListBottomSheetController.computeSheetMaxHeight(2400));
    }

    @Test
    public void hideIfPresentDoesNothingWhenControllerIsNull() {
        SessionListBottomSheetController.hideIfPresent(null);
    }
}
