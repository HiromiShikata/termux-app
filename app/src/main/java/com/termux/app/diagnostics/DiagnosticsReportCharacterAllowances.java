package com.termux.app.diagnostics;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class DiagnosticsReportCharacterAllowances {

    private final boolean mUnlimited;

    @NonNull
    private final Map<String, Integer> mAllowedCharactersByName;

    private final boolean mGrantsEveryWantedCharacter;

    private DiagnosticsReportCharacterAllowances(boolean unlimited,
                                                 @NonNull Map<String, Integer> allowedCharactersByName,
                                                 boolean grantsEveryWantedCharacter) {
        mUnlimited = unlimited;
        mAllowedCharactersByName = Collections.unmodifiableMap(
            new LinkedHashMap<>(allowedCharactersByName));
        mGrantsEveryWantedCharacter = grantsEveryWantedCharacter;
    }

    @NonNull
    public static DiagnosticsReportCharacterAllowances unlimited() {
        return new DiagnosticsReportCharacterAllowances(true,
            new LinkedHashMap<String, Integer>(), true);
    }

    @NonNull
    public static DiagnosticsReportCharacterAllowances allocatedByPriority(
            int ceilingCharacters,
            int omissionNoteCharacters,
            @NonNull List<DiagnosticsReportSubsection> subsectionsInPriorityOrder,
            @NonNull List<String> subsectionNamesInPrintOrder,
            @NonNull Map<String, Integer> measuredOffsetByName,
            @NonNull Map<String, Integer> measuredCharactersByName) {
        Map<String, Integer> largestUsefulCharactersByName = new LinkedHashMap<>();
        for (DiagnosticsReportSubsection subsection : subsectionsInPriorityOrder) {
            largestUsefulCharactersByName.put(subsection.getName(),
                subsection.getLargestUsefulCharacters());
        }

        for (String measuredName : measuredOffsetByName.keySet()) {
            if (!subsectionNamesInPrintOrder.contains(measuredName)) {
                throw new IllegalArgumentException("The report subsection " + measuredName
                    + " was rendered without being declared in the print order it is allocated from");
            }
        }

        List<String> renderedNamesInPrintOrder = new ArrayList<>();
        for (String subsectionName : subsectionNamesInPrintOrder) {
            if (measuredOffsetByName.containsKey(subsectionName)) {
                renderedNamesInPrintOrder.add(subsectionName);
            }
        }

        Map<String, Integer> wantedByName = new LinkedHashMap<>();
        Map<String, Integer> fixedCharactersBeforeByName = new LinkedHashMap<>();
        int measuredCharactersBefore = 0;
        for (String subsectionName : renderedNamesInPrintOrder) {
            int measuredCharacters = valueOf(measuredCharactersByName, subsectionName);
            wantedByName.put(subsectionName, Math.min(measuredCharacters,
                valueOf(largestUsefulCharactersByName, subsectionName)));
            fixedCharactersBeforeByName.put(subsectionName,
                valueOf(measuredOffsetByName, subsectionName) - measuredCharactersBefore);
            measuredCharactersBefore += measuredCharacters;
        }

        Map<String, Integer> allowedByName = new LinkedHashMap<>();
        for (String subsectionName : subsectionNamesInPrintOrder) {
            allowedByName.put(subsectionName, 0);
        }

        boolean grantsEveryWantedCharacter = true;
        for (DiagnosticsReportSubsection subsection : subsectionsInPriorityOrder) {
            String subsectionName = subsection.getName();
            if (!renderedNamesInPrintOrder.contains(subsectionName)) continue;
            int wantedCharacters = valueOf(wantedByName, subsectionName);
            if (wantedCharacters == 0) continue;
            if (wantedCharacters < valueOf(measuredCharactersByName, subsectionName)) {
                grantsEveryWantedCharacter = false;
            }

            int slackCharacters = ceilingCharacters - offsetOf(subsectionName,
                renderedNamesInPrintOrder, fixedCharactersBeforeByName, allowedByName,
                measuredCharactersByName, omissionNoteCharacters);
            boolean printedAfterThisSubsection = false;
            for (String laterName : renderedNamesInPrintOrder) {
                if (laterName.equals(subsectionName)) {
                    printedAfterThisSubsection = true;
                    continue;
                }
                if (!printedAfterThisSubsection) continue;
                int laterAllowedCharacters = valueOf(allowedByName, laterName);
                if (laterAllowedCharacters == 0) continue;
                int laterSlackCharacters = ceilingCharacters - offsetOf(laterName,
                    renderedNamesInPrintOrder, fixedCharactersBeforeByName, allowedByName,
                    measuredCharactersByName, omissionNoteCharacters) - laterAllowedCharacters;
                if (laterSlackCharacters < slackCharacters) slackCharacters = laterSlackCharacters;
            }

            int grantedCharacters = wantedCharacters;
            if (grantedCharacters > slackCharacters) {
                grantedCharacters = Math.max(0, slackCharacters - omissionNoteCharacters);
                grantsEveryWantedCharacter = false;
            }
            allowedByName.put(subsectionName, grantedCharacters);
        }

        return new DiagnosticsReportCharacterAllowances(false, allowedByName,
            grantsEveryWantedCharacter);
    }

    public boolean grantsEveryWantedCharacter() {
        return mGrantsEveryWantedCharacter;
    }

    public int getAllowedCharactersOf(@NonNull String subsectionName) {
        if (mUnlimited) return Integer.MAX_VALUE;
        return valueOf(mAllowedCharactersByName, subsectionName);
    }

    private static int offsetOf(@NonNull String subsectionName,
                                @NonNull List<String> renderedNamesInPrintOrder,
                                @NonNull Map<String, Integer> fixedCharactersBeforeByName,
                                @NonNull Map<String, Integer> allowedByName,
                                @NonNull Map<String, Integer> measuredCharactersByName,
                                int omissionNoteCharacters) {
        int spentCharactersBefore = 0;
        for (String earlierName : renderedNamesInPrintOrder) {
            if (earlierName.equals(subsectionName)) break;
            int allowedCharacters = valueOf(allowedByName, earlierName);
            spentCharactersBefore += allowedCharacters;
            if (allowedCharacters < valueOf(measuredCharactersByName, earlierName)) {
                spentCharactersBefore += omissionNoteCharacters;
            }
        }
        return valueOf(fixedCharactersBeforeByName, subsectionName) + spentCharactersBefore;
    }

    private static int valueOf(@NonNull Map<String, Integer> charactersByName,
                               @NonNull String subsectionName) {
        Integer characters = charactersByName.get(subsectionName);
        if (characters == null) {
            throw new IllegalArgumentException(
                "No character allowance is declared for the report subsection " + subsectionName);
        }
        return characters;
    }
}
