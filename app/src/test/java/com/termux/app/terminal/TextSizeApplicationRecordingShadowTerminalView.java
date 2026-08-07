package com.termux.app.terminal;

import com.termux.view.TerminalView;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.shadows.ShadowView;

import java.util.ArrayList;
import java.util.List;

@Implements(TerminalView.class)
public class TextSizeApplicationRecordingShadowTerminalView extends ShadowView {

    private static final List<Integer> APPLIED_TEXT_SIZES = new ArrayList<>();

    public static List<Integer> appliedTextSizes() {
        return APPLIED_TEXT_SIZES;
    }

    public static void forgetAppliedTextSizes() {
        APPLIED_TEXT_SIZES.clear();
    }

    @Implementation
    protected void setTextSize(int textSize) {
        APPLIED_TEXT_SIZES.add(textSize);
    }
}
