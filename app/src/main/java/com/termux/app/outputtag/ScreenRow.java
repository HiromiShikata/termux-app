package com.termux.app.outputtag;

public final class ScreenRow {

    public final String text;

    public final boolean continuesOnTheNextRow;

    public ScreenRow(String text, boolean continuesOnTheNextRow) {
        this.text = text == null ? "" : text;
        this.continuesOnTheNextRow = continuesOnTheNextRow;
    }

}
