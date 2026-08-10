package com.termux.app.diagnostics;

import androidx.annotation.NonNull;

import java.util.concurrent.atomic.AtomicReference;

public final class AppProcessPopulationHolder {

    private static final AppProcessPopulationHolder INSTANCE = new AppProcessPopulationHolder();

    @NonNull
    private final AtomicReference<DiagnosticsAppProcessPopulation> mPopulation =
        new AtomicReference<>(DiagnosticsAppProcessPopulation.UNMEASURED);

    @NonNull
    public static AppProcessPopulationHolder getInstance() {
        return INSTANCE;
    }

    public void record(@NonNull DiagnosticsAppProcessPopulation population) {
        mPopulation.set(population);
    }

    @NonNull
    public DiagnosticsAppProcessPopulation snapshot() {
        return mPopulation.get();
    }
}
