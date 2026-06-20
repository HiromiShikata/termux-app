package com.termux.app;

public interface ActivityComponent {

    default void onActivityStop() {
    }

    default void onActivityDestroy() {
    }

    default boolean onBackPressed() {
        return false;
    }
}
