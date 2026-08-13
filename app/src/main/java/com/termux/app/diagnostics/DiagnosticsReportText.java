package com.termux.app.diagnostics;

import androidx.annotation.NonNull;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class DiagnosticsReportText {

    @NonNull
    private final StringBuilder mBuilder = new StringBuilder();

    @NonNull
    private final DiagnosticsReportCharacterAllowances mAllowances;

    @NonNull
    private final Map<String, Integer> mMeasuredOffsetByName = new LinkedHashMap<>();

    @NonNull
    private final Map<String, Integer> mMeasuredCharactersByName = new LinkedHashMap<>();

    public DiagnosticsReportText(@NonNull DiagnosticsReportCharacterAllowances allowances) {
        mAllowances = allowances;
    }

    @NonNull
    public DiagnosticsReportText append(@NonNull String text) {
        mBuilder.append(text);
        return this;
    }

    @NonNull
    public DiagnosticsReportText append(char character) {
        mBuilder.append(character);
        return this;
    }

    @NonNull
    public DiagnosticsReportText append(int number) {
        mBuilder.append(number);
        return this;
    }

    @NonNull
    public DiagnosticsReportText append(long number) {
        mBuilder.append(number);
        return this;
    }

    public int length() {
        return mBuilder.length();
    }

    public void recordSubsection(@NonNull String subsectionName, int measuredCharacters) {
        mMeasuredOffsetByName.put(subsectionName, mBuilder.length());
        mMeasuredCharactersByName.put(subsectionName, measuredCharacters);
    }

    public int getAllowedCharactersOf(@NonNull String subsectionName) {
        return mAllowances.getAllowedCharactersOf(subsectionName);
    }

    @NonNull
    public Map<String, Integer> getMeasuredOffsetByName() {
        return Collections.unmodifiableMap(mMeasuredOffsetByName);
    }

    @NonNull
    public Map<String, Integer> getMeasuredCharactersByName() {
        return Collections.unmodifiableMap(mMeasuredCharactersByName);
    }

    @Override
    @NonNull
    public String toString() {
        return mBuilder.toString();
    }
}
