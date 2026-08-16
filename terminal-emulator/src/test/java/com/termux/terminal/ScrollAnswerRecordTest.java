package com.termux.terminal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class ScrollAnswerRecordTest {

    @Test
    public void aScrollThatTheProgramAnsweredWithOutputCountsAsAnswered() {
        ScrollAnswerRecord record = new ScrollAnswerRecord();

        record.recordScrollSentToTheProgram(10_000L);
        record.recordOutputFromTheProgram(10_050L);

        assertEquals("a person who scrolls a program that redraws sees the screen move, and that is"
                + " the only sense in which scrolling works while the alternate screen is active,"
                + " so output arriving after a scroll is what answers it",
            1L, record.getEpisodesAnsweredByTheProgram());
        assertEquals(1L, record.getEpisodesSentToTheProgram());
        assertNull(record.getUnansweredEpisodeSentAtMillis());
    }

    @Test
    public void aScrollTheProgramNeverAnsweredKeepsTheTimeItWasSent() {
        ScrollAnswerRecord record = new ScrollAnswerRecord();

        record.recordScrollSentToTheProgram(10_000L);

        assertEquals("the moment a session stopped answering is the only thing that dates the"
                + " failure, so the time of the outstanding scroll has to survive until it is"
                + " answered rather than being replaced by the time of the reading",
            Long.valueOf(10_000L), record.getUnansweredEpisodeSentAtMillis());
        assertEquals(0L, record.getEpisodesAnsweredByTheProgram());
    }

    @Test
    public void aBurstOfStepsWhileNothingHasAnsweredIsOneEpisodeWaitingOnOneAnswer() {
        ScrollAnswerRecord record = new ScrollAnswerRecord();

        record.recordScrollSentToTheProgram(10_000L);
        record.recordScrollSentToTheProgram(10_016L);
        record.recordScrollSentToTheProgram(10_032L);

        assertEquals("one swipe of a finger produces a run of steps a few milliseconds apart and the"
                + " program redraws once for the run, so counting each step as something owed an"
                + " answer would report a working session as answering one scroll in three",
            1L, record.getEpisodesSentToTheProgram());
        assertEquals(Long.valueOf(10_000L), record.getUnansweredEpisodeSentAtMillis());
    }

    @Test
    public void aScrollAfterAnAnsweredOneIsANewEpisodeOwedItsOwnAnswer() {
        ScrollAnswerRecord record = new ScrollAnswerRecord();

        record.recordScrollSentToTheProgram(10_000L);
        record.recordOutputFromTheProgram(10_050L);
        record.recordScrollSentToTheProgram(20_000L);

        assertEquals(2L, record.getEpisodesSentToTheProgram());
        assertEquals("a session that answered every earlier scroll and stops answering now is the"
                + " failure this measures, so the count of what was sent has to keep rising while"
                + " the count of what was answered stands still",
            1L, record.getEpisodesAnsweredByTheProgram());
        assertEquals(Long.valueOf(20_000L), record.getUnansweredEpisodeSentAtMillis());
    }

    @Test
    public void outputThatNoScrollWasWaitingOnAnswersNothing() {
        ScrollAnswerRecord record = new ScrollAnswerRecord();

        record.recordOutputFromTheProgram(10_000L);

        assertEquals("a program on the alternate screen writes continuously on its own, and counting"
                + " that as an answer would report every session as answering scrolls it was never"
                + " sent",
            0L, record.getEpisodesAnsweredByTheProgram());
        assertEquals(0L, record.getEpisodesSentToTheProgram());
        assertNull(record.getLastEpisodeAnsweredAtMillis());
    }

    @Test
    public void theTimeOfTheLastAnswerIsTheMomentTheOutputArrived() {
        ScrollAnswerRecord record = new ScrollAnswerRecord();

        record.recordScrollSentToTheProgram(10_000L);
        record.recordOutputFromTheProgram(10_050L);
        record.recordOutputFromTheProgram(11_000L);

        assertEquals("how long a session has been answering nothing is read from when it last"
                + " answered, so that time is the moment output closed a scroll rather than the"
                + " moment of any later output",
            Long.valueOf(10_050L), record.getLastEpisodeAnsweredAtMillis());
    }

    @Test
    public void aRecordThatHasSeenNothingReportsNoScrollWaitingAndNoAnswer() {
        ScrollAnswerRecord record = new ScrollAnswerRecord();

        assertEquals(0L, record.getEpisodesSentToTheProgram());
        assertEquals(0L, record.getEpisodesAnsweredByTheProgram());
        assertNull(record.getUnansweredEpisodeSentAtMillis());
        assertNull("a session nobody has scrolled has to be distinguishable from one that answered a"
                + " scroll, otherwise every quiet session reads as having answered",
            record.getLastEpisodeAnsweredAtMillis());
    }
}
