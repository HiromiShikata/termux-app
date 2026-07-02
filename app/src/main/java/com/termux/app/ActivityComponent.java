package com.termux.app;

public interface ActivityComponent {

    default void onActivityResume() {
    }

    default void onActivityStop() {
    }

    default void onActivityDestroy() {
    }

    default boolean onBackPressed() {
        return false;
    }
}
