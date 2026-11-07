package com.termux.app.handedness;

import java.util.ArrayList;
import java.util.List;

public final class HandednessLayoutApplier {

    private final List<HandednessLayoutPart> parts = new ArrayList<>();

    public void registerPart(HandednessLayoutPart part) {
        parts.add(part);
    }

    public void applyHandedness(Handedness handedness) {
        for (HandednessLayoutPart part : parts) {
            part.applyHandedness(handedness);
        }
    }

}
