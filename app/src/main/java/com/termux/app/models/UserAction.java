package com.termux.app.models;

public enum UserAction {

    ABOUT("about");

    private final String name;

    UserAction(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

}
