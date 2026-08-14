package com.termux.app.ownercall;

import android.graphics.Rect;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

public final class OwnerCallDialogViewport {

    private OwnerCallDialogViewport() {
    }

    @NonNull
    public static OwnerCallDialogGeometry resolve(@NonNull ViewGroup dialogParent,
                                                  @NonNull View terminalArea,
                                                  int screenHeightPixels,
                                                  int terminalRowHeightPixels) {
        Rect terminalBounds = new Rect(0, 0, terminalArea.getWidth(), terminalArea.getHeight());
        dialogParent.offsetDescendantRectToMyCoords(terminalArea, terminalBounds);
        return OwnerCallDialogGeometry.resolve(screenHeightPixels, terminalBounds.left,
            terminalBounds.width(), dialogParent.getHeight() - terminalBounds.bottom,
            terminalRowHeightPixels);
    }
}
