package com.termux.app.process;

import androidx.annotation.NonNull;

import java.util.List;

public interface ThreadTable {

    @NonNull
    List<ProcessThread> threads();
}
