package com.termux.app.terminal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class CallToUserTagScannerTest {

    /**
     * The exact line the statusline emitter renders into the pane for an approved call: the opening
     * marker, the trigger value, and the closing marker, with nothing between them. This constant is
     * the single expression of that cross-component data contract on this side; the emitter side
     * asserts against the same format.
     */
    private static final String EMITTED_TRIGGER_TAG_FORMAT = "<call-to-user>%s</call-to-user>";

    private static final String EMITTED_CANDIDATE_TAG_FORMAT =
        "<call-to-user-pending>%s</call-to-user-pending>";

    private static String emittedTriggerTag(String triggerValue) {
        return String.format(EMITTED_TRIGGER_TAG_FORMAT, triggerValue);
    }

    private static String emittedCandidateTag(String reason) {
        return String.format(EMITTED_CANDIDATE_TAG_FORMAT, reason);
    }

    private static List<String> newTriggerValues(CallToUserTagScanner scanner, String output) {
        List<String> triggerValues = new ArrayList<>();
        for (ApprovedCallToUser call : scanner.newCalls(output)) {
            triggerValues.add(call.getTriggerValue());
        }
        return triggerValues;
    }

    @Test
    public void matchesTheExactLineTheEmitterRendersForAnApprovalTimestampTriggerValue() {
        String approvalTimestamp = "2026-07-28T09:00:01.234Z";

        assertEquals(List.of(approvalTimestamp),
            CallToUserTagScanner.extractTriggerValues(emittedTriggerTag(approvalTimestamp)));
    }

    @Test
    public void matchesTheExactLineTheEmitterRendersForALegacyMessageTextTriggerValue() {
        String messageText = "🔴deploy review: please approve the rollout";

        assertEquals(List.of(messageText),
            CallToUserTagScanner.extractTriggerValues(emittedTriggerTag(messageText)));
    }

    @Test
    public void firesAndDeduplicatesIdenticallyForBothTriggerValueForms() {
        CallToUserTagScanner timestampScanner = new CallToUserTagScanner();
        String approvalTimestamp = "2026-07-28T09:00:01.234Z";
        String laterApprovalTimestamp = "2026-07-28T09:30:00.000Z";

        assertEquals(List.of(approvalTimestamp),
            newTriggerValues(timestampScanner, emittedTriggerTag(approvalTimestamp)));
        assertTrue(newTriggerValues(timestampScanner, emittedTriggerTag(approvalTimestamp)).isEmpty());
        assertEquals(List.of(laterApprovalTimestamp), newTriggerValues(timestampScanner,
            emittedTriggerTag(approvalTimestamp) + "\n" + emittedTriggerTag(laterApprovalTimestamp)));

        CallToUserTagScanner messageTextScanner = new CallToUserTagScanner();
        String messageText = "🔴deploy review: please approve the rollout";
        String laterMessageText = "🔴release cut: confirm the tag";

        assertEquals(List.of(messageText),
            newTriggerValues(messageTextScanner, emittedTriggerTag(messageText)));
        assertTrue(newTriggerValues(messageTextScanner, emittedTriggerTag(messageText)).isEmpty());
        assertEquals(List.of(laterMessageText), newTriggerValues(messageTextScanner,
            emittedTriggerTag(messageText) + "\n" + emittedTriggerTag(laterMessageText)));
    }

    @Test
    public void neitherFamilyMatchesWhenTheEmittedLineWasCutBeforeItsClosingMarker() {
        String longNonAsciiReason =
            "🔴本番デプロイの承認: 対象のプルリクエストをレビューのうえ、"
                + "マージの可否をご判断いただけますでしょうか";
        String cutTriggerTag = emittedTriggerTag(longNonAsciiReason).substring(0, 60);
        String cutCandidateTag = emittedCandidateTag(longNonAsciiReason).substring(0, 60);

        assertTrue(CallToUserTagScanner.extractTriggerValues(cutTriggerTag).isEmpty());
        assertTrue(CallToUserTagScanner.extractCandidateReasons(cutCandidateTag).isEmpty());
        assertTrue(new CallToUserTagScanner().newCalls(cutTriggerTag).isEmpty());
    }

    @Test
    public void aCutCandidateTagLeavesNoRememberedReasonForALaterCompleteTrigger() {
        CallToUserTagScanner scanner = new CallToUserTagScanner();
        String longNonAsciiReason =
            "🔴本番デプロイの承認: 対象のプルリクエストをレビューのうえ、"
                + "マージの可否をご判断いただけますでしょうか";
        String approvalTimestamp = "2026-07-28T09:00:01.234Z";

        assertTrue(scanner.newCalls(emittedCandidateTag(longNonAsciiReason).substring(0, 60))
            .isEmpty());

        List<ApprovedCallToUser> calls = scanner.newCalls(
            emittedCandidateTag(longNonAsciiReason).substring(0, 60)
                + "\n" + emittedTriggerTag(approvalTimestamp));

        assertEquals(1, calls.size());
        assertEquals(approvalTimestamp, calls.get(0).getTriggerValue());
        assertEquals(approvalTimestamp, calls.get(0).getDisplayReason());
    }

    @Test
    public void extractsTheCandidateReasonOfACompleteEmittedCandidateBlock() {
        String reason = "🔴build failure: decide whether to roll back";

        assertEquals(List.of(reason),
            CallToUserTagScanner.extractCandidateReasons(emittedCandidateTag(reason)));
        assertTrue(CallToUserTagScanner.extractTriggerValues(emittedCandidateTag(reason)).isEmpty());
    }

    @Test
    public void extractsReasonOfCompleteBlock() {
        List<String> reasons = CallToUserTagScanner.extractTriggerValues(
            "before <call-to-user>needs approval</call-to-user> after");
        assertEquals(1, reasons.size());
        assertEquals("needs approval", reasons.get(0));
    }

    @Test
    public void trimsSurroundingWhitespaceAndNewlinesInsideBlock() {
        List<String> reasons = CallToUserTagScanner.extractTriggerValues(
            "<call-to-user>\n  please review the diff  \n</call-to-user>");
        assertEquals(1, reasons.size());
        assertEquals("please review the diff", reasons.get(0));
    }

    @Test
    public void preservesJapaneseReason() {
        List<String> reasons = CallToUserTagScanner.extractTriggerValues(
            "<call-to-user>承認をお願いします</call-to-user>");
        assertEquals(1, reasons.size());
        assertEquals("承認をお願いします", reasons.get(0));
    }

    @Test
    public void ignoresBlockWithoutClosingTag() {
        List<String> reasons = CallToUserTagScanner.extractTriggerValues("<call-to-user>incomplete");
        assertTrue(reasons.isEmpty());
    }

    @Test
    public void extractsMultipleBlocksNonGreedily() {
        List<String> reasons = CallToUserTagScanner.extractTriggerValues(
            "<call-to-user>first</call-to-user> mid <call-to-user>second</call-to-user>");
        assertEquals(2, reasons.size());
        assertEquals("first", reasons.get(0));
        assertEquals("second", reasons.get(1));
    }

    @Test
    public void ignoresEmptyBlock() {
        List<String> reasons = CallToUserTagScanner.extractTriggerValues("<call-to-user>   </call-to-user>");
        assertTrue(reasons.isEmpty());
    }

    @Test
    public void normalizeReturnsNullForBlankAndNull() {
        assertNull(CallToUserTagScanner.normalizeReason(null));
        assertNull(CallToUserTagScanner.normalizeReason("   "));
        assertEquals("done", CallToUserTagScanner.normalizeReason("  done  "));
    }

    @Test
    public void preservesRunsOfHorizontalWhitespaceWhenTrimmingOnly() {
        assertEquals("a    b\nc  d", CallToUserTagScanner.normalizeReason("a    b\nc  d"));
    }

    @Test
    public void preservesTabsAndMixedHorizontalWhitespace() {
        assertEquals("a \t \t b\nc\t\td",
            CallToUserTagScanner.normalizeReason("a \t \t b\nc\t\td"));
    }

    @Test
    public void extractedReasonPreservesInternalWhitespaceRaw() {
        List<String> reasons = CallToUserTagScanner.extractTriggerValues(
            "<call-to-user>please    review\nthe    diff</call-to-user>");
        assertEquals(1, reasons.size());
        assertEquals("please    review\nthe    diff", reasons.get(0));
    }

    @Test
    public void newReasonsReturnsEachReasonInOrderOnFirstScan() {
        CallToUserTagScanner scanner = new CallToUserTagScanner();
        List<String> reasons = newTriggerValues(scanner,
            "<call-to-user>first</call-to-user><call-to-user>second</call-to-user>");
        assertEquals(2, reasons.size());
        assertEquals("first", reasons.get(0));
        assertEquals("second", reasons.get(1));
    }

    @Test
    public void deduplicatesAlreadyFiredReasonOnRedraw() {
        CallToUserTagScanner scanner = new CallToUserTagScanner();
        String output = "prompt <call-to-user>needs approval</call-to-user> prompt";

        assertEquals(1, newTriggerValues(scanner, output).size());
        assertTrue(newTriggerValues(scanner, output).isEmpty());
    }

    @Test
    public void firesNextNewReasonAfterPreviousFired() {
        CallToUserTagScanner scanner = new CallToUserTagScanner();

        assertEquals(List.of("first"),
            newTriggerValues(scanner, "<call-to-user>first</call-to-user>"));
        assertEquals(List.of("second"),
            newTriggerValues(scanner, "<call-to-user>first</call-to-user><call-to-user>second</call-to-user>"));
        assertTrue(newTriggerValues(scanner,
            "<call-to-user>first</call-to-user><call-to-user>second</call-to-user>").isEmpty());
    }

    @Test
    public void returnsEmptyWhenNoBlockPresent() {
        CallToUserTagScanner scanner = new CallToUserTagScanner();
        assertTrue(newTriggerValues(scanner, "plain terminal output").isEmpty());
        assertTrue(newTriggerValues(scanner, null).isEmpty());
    }

    @Test
    public void doesNotMatchPartialTagName() {
        List<String> reasons = CallToUserTagScanner.extractTriggerValues(
            "<call-to>x</call-to><call-to-user-extra>y</call-to-user-extra>");
        assertTrue(reasons.isEmpty());
    }

    @Test
    public void doesNotReFireWhenAnAlreadyFiredTagRemainsInScrollbackAndMoreOutputArrives() {
        CallToUserTagScanner scanner = new CallToUserTagScanner();

        assertEquals(List.of("approval"),
            newTriggerValues(scanner, "<call-to-user>approval</call-to-user>"));

        assertTrue(newTriggerValues(scanner,
            "<call-to-user>approval</call-to-user>\nmore output line 1").isEmpty());
        assertTrue(newTriggerValues(scanner,
            "<call-to-user>approval</call-to-user>\nmore output line 1\nmore output line 2").isEmpty());
    }

    @Test
    public void firesTwoNewTagsAppearingInASingleUpdateEachExactlyOnce() {
        CallToUserTagScanner scanner = new CallToUserTagScanner();

        assertEquals(List.of("alpha"),
            newTriggerValues(scanner, "<call-to-user>alpha</call-to-user>"));

        List<String> burst = newTriggerValues(scanner,
            "<call-to-user>alpha</call-to-user><call-to-user>beta</call-to-user><call-to-user>gamma</call-to-user>");
        assertEquals(2, burst.size());
        assertEquals("beta", burst.get(0));
        assertEquals("gamma", burst.get(1));
    }

    @Test
    public void firesAGenuinelyNewTagAfterEarlierTagsHaveBeenTrimmedOutOfTheTranscript() {
        CallToUserTagScanner scanner = new CallToUserTagScanner();

        StringBuilder longTranscript = new StringBuilder("<call-to-user>approval</call-to-user>\n");
        for (int line = 0; line < 5000; line++) {
            longTranscript.append("output line ").append(line).append('\n');
        }
        assertEquals(List.of("approval"), newTriggerValues(scanner, longTranscript.toString()));

        String trimmedWithNewTag =
            "output line 4998\noutput line 4999\n<call-to-user>review now</call-to-user>\n";
        assertEquals(List.of("review now"), newTriggerValues(scanner, trimmedWithNewTag));
    }

    @Test
    public void doesNotReFireAnAlreadyFiredTagWithInternalWhitespaceOnReScan() {
        CallToUserTagScanner scanner = new CallToUserTagScanner();
        String output = "prompt <call-to-user>needs    review of   diff</call-to-user> tail";

        assertEquals(List.of("needs    review of   diff"), newTriggerValues(scanner, output));
        assertTrue(newTriggerValues(scanner, output).isEmpty());
        assertTrue(newTriggerValues(scanner,
            output + "\nmore output appended after the answer").isEmpty());
    }

    @Test
    public void doesNotReFireAnAlreadyFiredTagWhenLaterTranscriptHasTrimmedTheTagAway() {
        CallToUserTagScanner scanner = new CallToUserTagScanner();

        assertEquals(List.of("approval"),
            newTriggerValues(scanner, "<call-to-user>approval</call-to-user>\nline a\nline b\n"));
        assertTrue(newTriggerValues(scanner,
            "line a\nline b\nstill the same already fired output\n").isEmpty());
    }

    @Test
    public void doesNotReFireAnEarlierReasonWhenAReFedWindowShowsItWhileTheNewerReasonScrolledOffTheTail() {
        CallToUserTagScanner scanner = new CallToUserTagScanner();

        assertEquals(List.of("first"),
            newTriggerValues(scanner, "<call-to-user>first</call-to-user>\n"));
        assertEquals(List.of("second"),
            newTriggerValues(scanner, "<call-to-user>first</call-to-user>\n<call-to-user>second</call-to-user>\n"));

        assertTrue(newTriggerValues(scanner,
            "<call-to-user>first</call-to-user>\nlater plain output\n").isEmpty());

        assertEquals(List.of("third"),
            newTriggerValues(scanner, "<call-to-user>first</call-to-user>\nlater plain output\n<call-to-user>third</call-to-user>\n"));
    }
}
