package com.termux.app.diagnostics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.termux.terminal.ScrollAnswerRecord;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

public class ScrollAnswerTotalsTest {

    @Test
    public void sessionsWhoseProgramAnsweredEveryScrollLeaveNothingOutstanding() {
        ScrollAnswerRecord answered = new ScrollAnswerRecord();
        answered.recordScrollSentToTheProgram(1000L);
        answered.recordOutputFromTheProgram(1200L);

        ScrollAnswerTotals totals = ScrollAnswerTotals.ofRecords(Collections.singletonList(answered));

        assertEquals("an episode was opened by the scroll", 1L, totals.getEpisodesSentToTheProgram());
        assertEquals("the program's output closed it", 1L, totals.getEpisodesAnsweredByTheProgram());
        assertFalse("nothing is outstanding once the episode closed", totals.hasUnansweredEpisode());
    }

    @Test
    public void anEpisodeTheProgramNeverAnsweredIsReportedWithTheInstantItWasSent() {
        ScrollAnswerRecord unanswered = new ScrollAnswerRecord();
        unanswered.recordScrollSentToTheProgram(5000L);

        ScrollAnswerTotals totals = ScrollAnswerTotals.ofRecords(Collections.singletonList(unanswered));

        assertEquals("the episode was opened", 1L, totals.getEpisodesSentToTheProgram());
        assertEquals("no output closed it", 0L, totals.getEpisodesAnsweredByTheProgram());
        assertTrue("the episode is still outstanding", totals.hasUnansweredEpisode());
        assertEquals("the instant the scroll was sent is kept", 5000L,
            totals.getEarliestUnansweredEpisodeSentAtMillis());
    }

    @Test
    public void theEarliestOutstandingEpisodeAcrossSessionsIsTheOneReported() {
        ScrollAnswerRecord later = new ScrollAnswerRecord();
        later.recordScrollSentToTheProgram(9000L);
        ScrollAnswerRecord earlier = new ScrollAnswerRecord();
        earlier.recordScrollSentToTheProgram(3000L);

        ScrollAnswerTotals totals = ScrollAnswerTotals.ofRecords(Arrays.asList(later, earlier));

        assertEquals("both sessions opened an episode", 2L, totals.getEpisodesSentToTheProgram());
        assertEquals("the earliest outstanding episode is the one reported", 3000L,
            totals.getEarliestUnansweredEpisodeSentAtMillis());
    }

    @Test
    public void countsAreSummedAcrossEverySessionGiven() {
        ScrollAnswerRecord busy = new ScrollAnswerRecord();
        busy.recordScrollSentToTheProgram(1000L);
        busy.recordOutputFromTheProgram(1100L);
        busy.recordScrollSentToTheProgram(2000L);
        busy.recordOutputFromTheProgram(2100L);
        ScrollAnswerRecord quiet = new ScrollAnswerRecord();
        quiet.recordScrollSentToTheProgram(3000L);
        quiet.recordOutputFromTheProgram(3100L);

        ScrollAnswerTotals totals = ScrollAnswerTotals.ofRecords(Arrays.asList(busy, quiet));

        assertEquals("three episodes were opened in total", 3L, totals.getEpisodesSentToTheProgram());
        assertEquals("all three were answered", 3L, totals.getEpisodesAnsweredByTheProgram());
        assertFalse("nothing is outstanding", totals.hasUnansweredEpisode());
    }

    @Test
    public void aProgramAnsweringBetweenTheTwoReadsIsNotReportedAsAnEpisodeSentAtTheEpoch() {
        ScrollAnswerTotals totals = ScrollAnswerTotals.of(319L, 318L, 0L);

        assertFalse("each session's counts are read one call at a time while the terminal keeps"
            + " running, so they can skew by one against an episode that closed between the two"
            + " reads. Reading that skew as an outstanding episode would put 1970-01-01T00:00:00Z"
            + " into the field this whole investigation turns on", totals.hasUnansweredEpisode());
    }

    @Test
    public void aProcessWhereNothingWasScrolledReportsNoEpisodes() {
        ScrollAnswerTotals totals = ScrollAnswerTotals.ofRecords(Collections.<ScrollAnswerRecord>emptyList());

        assertEquals("no episode was opened", 0L, totals.getEpisodesSentToTheProgram());
        assertEquals("no episode was answered", 0L, totals.getEpisodesAnsweredByTheProgram());
        assertFalse("nothing is outstanding", totals.hasUnansweredEpisode());
    }
}
