package com.termux.app.diagnostics;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DiagnosticsReportCharacterAllowancesTest {

    private static final String PRINTED_FIRST = "printed first";

    private static final String PRINTED_SECOND = "printed second";

    private static final int OMISSION_NOTE_CHARACTERS = 10;

    private static final List<String> PRINT_ORDER =
        Arrays.asList(PRINTED_FIRST, PRINTED_SECOND);

    private static List<DiagnosticsReportSubsection> secondPrintedIsTheHigherPriority(
            int largestUsefulOfPrintedFirst) {
        return Arrays.asList(
            new DiagnosticsReportSubsection(PRINTED_SECOND, 50),
            new DiagnosticsReportSubsection(PRINTED_FIRST, largestUsefulOfPrintedFirst));
    }

    private static Map<String, Integer> charactersOf(int printedFirst, int printedSecond) {
        Map<String, Integer> charactersByName = new LinkedHashMap<>();
        charactersByName.put(PRINTED_FIRST, printedFirst);
        charactersByName.put(PRINTED_SECOND, printedSecond);
        return charactersByName;
    }

    private static Map<String, Integer> offsetsWithProseBetweenThem(int measuredOfPrintedFirst) {
        Map<String, Integer> offsetByName = new LinkedHashMap<>();
        offsetByName.put(PRINTED_FIRST, 0);
        offsetByName.put(PRINTED_SECOND, measuredOfPrintedFirst + 10);
        return offsetByName;
    }

    @Test
    public void theHigherPrioritySubsectionKeepsItsCharactersEvenWhenItIsPrintedLast() {
        DiagnosticsReportCharacterAllowances allowances =
            DiagnosticsReportCharacterAllowances.allocatedByPriority(100, OMISSION_NOTE_CHARACTERS,
                secondPrintedIsTheHigherPriority(50), PRINT_ORDER,
                offsetsWithProseBetweenThem(80), charactersOf(80, 40));

        Assert.assertEquals("the subsection declared the higher priority is printed last, so allocating"
                + " by print order is what starves it, and it has to come away with everything it wants",
            40, allowances.getAllowedCharactersOf(PRINTED_SECOND));
        Assert.assertEquals("the subsection printed first is the lower priority, so it takes what the"
                + " ceiling has left once the higher priority subsection and its omission note are paid for",
            30, allowances.getAllowedCharactersOf(PRINTED_FIRST));
    }

    @Test
    public void aSubsectionIsHeldToTheLargestSizeItDeclaresUseful() {
        DiagnosticsReportCharacterAllowances allowances =
            DiagnosticsReportCharacterAllowances.allocatedByPriority(1000, OMISSION_NOTE_CHARACTERS,
                secondPrintedIsTheHigherPriority(50), PRINT_ORDER,
                offsetsWithProseBetweenThem(80), charactersOf(80, 40));

        Assert.assertEquals("without a declared largest useful size the first subsection served would"
                + " take every character it rendered and leave nothing for the rest",
            50, allowances.getAllowedCharactersOf(PRINTED_FIRST));
        Assert.assertFalse("the subsection rendered more than it may keep, so the report has to be"
                + " rendered again against these allowances rather than returned as measured",
            allowances.grantsEveryWantedCharacter());
    }

    @Test
    public void aReportThatFitsKeepsEveryCharacterEverySubsectionRendered() {
        DiagnosticsReportCharacterAllowances allowances =
            DiagnosticsReportCharacterAllowances.allocatedByPriority(1000, OMISSION_NOTE_CHARACTERS,
                secondPrintedIsTheHigherPriority(50), PRINT_ORDER,
                offsetsWithProseBetweenThem(30), charactersOf(30, 40));

        Assert.assertEquals(30, allowances.getAllowedCharactersOf(PRINTED_FIRST));
        Assert.assertEquals(40, allowances.getAllowedCharactersOf(PRINTED_SECOND));
        Assert.assertTrue("nothing was cut, so the measured report is already the report to deliver",
            allowances.grantsEveryWantedCharacter());
    }

    @Test
    public void aSubsectionRenderedWithoutADeclaredPlaceInTheOrderIsRejected() {
        Map<String, Integer> offsetByName = offsetsWithProseBetweenThem(30);
        offsetByName.put("rendered without being declared", 200);
        Map<String, Integer> measuredCharactersByName = charactersOf(30, 40);
        measuredCharactersByName.put("rendered without being declared", 20);

        try {
            DiagnosticsReportCharacterAllowances.allocatedByPriority(1000, OMISSION_NOTE_CHARACTERS,
                secondPrintedIsTheHigherPriority(50), PRINT_ORDER, offsetByName,
                measuredCharactersByName);
            Assert.fail("a subsection that takes report characters without a declared priority is"
                + " starved silently, which is the defect this allocation exists to remove, so it has"
                + " to be rejected rather than allocated zero");
        } catch (IllegalArgumentException rejected) {
            Assert.assertTrue("the rejection has to name the subsection that was left undeclared."
                    + " Actual message: " + rejected.getMessage(),
                rejected.getMessage().contains("rendered without being declared"));
        }
    }

    @Test
    public void theMeasuringPassIsGrantedEveryCharacterItAsksFor() {
        DiagnosticsReportCharacterAllowances allowances =
            DiagnosticsReportCharacterAllowances.unlimited();

        Assert.assertEquals("the first render exists to measure what each subsection wants, so it may"
                + " not be cut short by an allowance",
            Integer.MAX_VALUE, allowances.getAllowedCharactersOf(PRINTED_FIRST));
        Assert.assertTrue(allowances.grantsEveryWantedCharacter());
    }
}
