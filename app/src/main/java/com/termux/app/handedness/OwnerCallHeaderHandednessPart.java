package com.termux.app.handedness;

import android.view.View;
import android.widget.LinearLayout;

import com.termux.R;

import java.util.ArrayList;
import java.util.List;

public final class OwnerCallHeaderHandednessPart implements HandednessLayoutPart {

    private final ViewGroupChildOrderMirror childOrderMirror = new ViewGroupChildOrderMirror();
    private final MarginEdgeMirror marginEdgeMirror = new MarginEdgeMirror();
    private final LinearLayout header;
    private final View position;

    public OwnerCallHeaderHandednessPart(View activityRoot) {
        header = activityRoot.findViewById(R.id.owner_call_dialog_header);
        position = activityRoot.findViewById(R.id.owner_call_dialog_position);
    }

    @Override
    public void applyHandedness(Handedness handedness) {
        childOrderMirror.apply(header, handedness, originalOrder -> {
            List<View> target = new ArrayList<>();
            target.add(header.findViewById(R.id.owner_call_dialog_close_button));
            target.add(header.findViewById(R.id.owner_call_dialog_previous_button));
            target.add(header.findViewById(R.id.owner_call_dialog_next_button));
            target.add(header.findViewById(R.id.owner_call_dialog_position));
            target.add(header.findViewById(R.id.owner_call_dialog_relative_time));
            return target;
        });
        marginEdgeMirror.apply(position, handedness);
    }

}
