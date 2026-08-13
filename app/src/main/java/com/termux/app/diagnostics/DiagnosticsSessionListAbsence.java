package com.termux.app.diagnostics;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collection;

public final class DiagnosticsSessionListAbsence {

    public enum Reason {

        PROJECT_GROUP_COLLAPSED("the project group holding it is collapsed"),
        SESSION_NAME_HIDDEN("its name is hidden and hidden sessions are being hidden"),
        NO_ROW_BUILT("the list built no row for it");

        @NonNull
        private final String mReportLabel;

        Reason(@NonNull String reportLabel) {
            mReportLabel = reportLabel;
        }

        @NonNull
        public String getReportLabel() {
            return mReportLabel;
        }
    }

    @Nullable
    private final Reason mReason;

    private DiagnosticsSessionListAbsence(@Nullable Reason reason) {
        mReason = reason;
    }

    @NonNull
    public static DiagnosticsSessionListAbsence presentInTheList() {
        return new DiagnosticsSessionListAbsence(null);
    }

    @NonNull
    public static DiagnosticsSessionListAbsence ofListState(@NonNull DiagnosticsSessionListDisplay listDisplay,
                                                            @NonNull String sessionName,
                                                            @NonNull Collection<String> collapsedProjectSessionNames,
                                                            @NonNull Collection<String> hiddenSessionNames) {
        if (listDisplay != DiagnosticsSessionListDisplay.NOT_DISPLAYED) {
            return presentInTheList();
        }
        if (hiddenSessionNames.contains(sessionName)) {
            return new DiagnosticsSessionListAbsence(Reason.SESSION_NAME_HIDDEN);
        }
        if (collapsedProjectSessionNames.contains(sessionName)) {
            return new DiagnosticsSessionListAbsence(Reason.PROJECT_GROUP_COLLAPSED);
        }
        return new DiagnosticsSessionListAbsence(Reason.NO_ROW_BUILT);
    }

    public boolean hasReason() {
        return mReason != null;
    }

    @NonNull
    public String getReportLabel() {
        if (mReason == null) {
            throw new IllegalStateException("a session the list shows has no absence to report");
        }
        return "not displayed because " + mReason.getReportLabel();
    }
}
