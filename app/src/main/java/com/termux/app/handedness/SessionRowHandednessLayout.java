package com.termux.app.handedness;

import android.view.View;
import android.view.ViewGroup;

import com.termux.R;

import java.util.ArrayList;
import java.util.List;

public final class SessionRowHandednessLayout {

    private final ViewGroupChildOrderMirror childOrderMirror = new ViewGroupChildOrderMirror();
    private final MarginEdgeMirror marginEdgeMirror = new MarginEdgeMirror();

    public void apply(View sessionRowView, Handedness handedness) {
        ViewGroup content = sessionRowView.findViewById(R.id.session_row_content);
        childOrderMirror.apply(content, handedness, originalOrder -> {
            List<View> target = new ArrayList<>();
            for (View child : originalOrder) {
                if (child.getId() == R.id.session_reconnect_control
                    || child.getId() == R.id.session_disable_toggle) {
                    target.add(child);
                }
            }
            for (View child : originalOrder) {
                if (child.getId() != R.id.session_reconnect_control
                    && child.getId() != R.id.session_disable_toggle
                    && child.getId() != R.id.session_active_indicator_bar) {
                    target.add(child);
                }
            }
            if (!target.isEmpty() && target.get(0).getId() != R.id.session_active_indicator_bar) {
                target.add(0, originalOrder.get(0));
            }
            return target;
        });
        marginEdgeMirror.apply(sessionRowView.findViewById(R.id.session_reconnect_control), handedness);
        marginEdgeMirror.apply(sessionRowView.findViewById(R.id.session_disable_toggle), handedness);
    }

}
