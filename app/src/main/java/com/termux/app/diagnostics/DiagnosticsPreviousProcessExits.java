package com.termux.app.diagnostics;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class DiagnosticsPreviousProcessExits {

    public enum Reading {
        NOT_TAKEN,
        NOT_KEPT_BY_THIS_ANDROID,
        READ
    }

    public static final DiagnosticsPreviousProcessExits NOT_TAKEN =
        new DiagnosticsPreviousProcessExits(Reading.NOT_TAKEN, Collections.<DiagnosticsPreviousProcessExit>emptyList());

    @NonNull
    private final Reading mReading;

    @NonNull
    private final List<DiagnosticsPreviousProcessExit> mExits;

    private DiagnosticsPreviousProcessExits(@NonNull Reading reading,
                                            @NonNull List<DiagnosticsPreviousProcessExit> exits) {
        mReading = reading;
        mExits = Collections.unmodifiableList(new ArrayList<>(exits));
    }

    @NonNull
    public static DiagnosticsPreviousProcessExits notRecordedByThisAndroid() {
        return new DiagnosticsPreviousProcessExits(Reading.NOT_KEPT_BY_THIS_ANDROID,
            Collections.<DiagnosticsPreviousProcessExit>emptyList());
    }

    @NonNull
    public static DiagnosticsPreviousProcessExits recorded(
        @NonNull List<DiagnosticsPreviousProcessExit> exits) {
        return new DiagnosticsPreviousProcessExits(Reading.READ, exits);
    }

    @NonNull
    public Reading getReading() {
        return mReading;
    }

    @NonNull
    public List<DiagnosticsPreviousProcessExit> getExits() {
        return mExits;
    }
}
