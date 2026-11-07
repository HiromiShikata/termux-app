package com.termux.app.handedness;

import android.view.View;

import androidx.drawerlayout.widget.DrawerLayout;

import com.termux.R;

public final class TabDrawerEdge {

    private TabDrawerEdge() {
    }

    public static int currentGravity(DrawerLayout drawerLayout) {
        View drawer = drawerLayout.findViewById(R.id.right_drawer);
        return ((DrawerLayout.LayoutParams) drawer.getLayoutParams()).gravity;
    }

    public static void apply(DrawerLayout drawerLayout, Handedness handedness) {
        View drawer = drawerLayout.findViewById(R.id.right_drawer);
        DrawerLayout.LayoutParams params = (DrawerLayout.LayoutParams) drawer.getLayoutParams();
        Integer original = (Integer) drawer.getTag(R.id.handedness_original_drawer_gravity_tag);
        if (original == null) {
            original = params.gravity;
            drawer.setTag(R.id.handedness_original_drawer_gravity_tag, original);
        }
        params.gravity = HandednessGravity.mirrored(original, handedness);
        drawer.setLayoutParams(params);
    }

}
