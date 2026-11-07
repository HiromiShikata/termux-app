package com.termux.app.handedness;

import android.view.View;
import android.widget.LinearLayout;

import com.termux.R;

import java.util.ArrayList;
import java.util.List;

public final class TextInputHistoryRowHandednessLayout {

    private final ViewGroupChildOrderMirror childOrderMirror = new ViewGroupChildOrderMirror();

    public void apply(View historyRowView, Handedness handedness) {
        childOrderMirror.apply((LinearLayout) historyRowView, handedness, originalOrder -> {
            List<View> target = new ArrayList<>();
            target.add(historyRowView.findViewById(R.id.toolbar_text_input_history_restore_button));
            target.add(historyRowView.findViewById(R.id.toolbar_text_input_history_edit_button));
            target.add(historyRowView.findViewById(R.id.toolbar_text_input_history_entry_scroll));
            target.add(historyRowView.findViewById(R.id.toolbar_text_input_history_pin_button));
            return target;
        });
    }

}
