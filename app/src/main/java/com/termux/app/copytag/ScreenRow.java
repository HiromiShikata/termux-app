package com.termux.app.copytag;

public final class ScreenRow {

    public final String text;

    public final boolean continuesOnTheNextRow;

    public ScreenRow(String text, boolean continuesOnTheNextRow) {
        this.text = text == null ? "" : text;
        this.continuesOnTheNextRow = continuesOnTheNextRow;
    }

}
