package com.termux.app;

import org.junit.Assert;
import org.junit.Test;

public class TermuxSessionCreationBatchTest {

    private final TermuxSessionCreationBatch batch = new TermuxSessionCreationBatch();

    private int runCount;

    private void countOneRun() {
        runCount++;
    }

    private void createThreeSessions() {
        batch.runOrDefer(this::countOneRun);
        batch.runOrDefer(this::countOneRun);
        batch.runOrDefer(this::countOneRun);
    }

    @Test
    public void workSharedBySessionsDoesNotRunWhileTheRestoreIsStillCreatingThem() {
        batch.begin();

        createThreeSessions();

        Assert.assertEquals("every session restored at startup ran this work again, and each run"
                + " is a synchronous transaction that holds the main thread while the user is"
                + " trying to scroll and type",
            0, runCount);
    }

    @Test
    public void workSharedBySessionsRunsExactlyOnceWhenTheRestoreFinishes() {
        batch.begin();
        createThreeSessions();

        batch.end(this::countOneRun);

        Assert.assertEquals("the state the work publishes is the same for the whole restore, so"
                + " publishing it once at the end says everything the repeated runs said",
            1, runCount);
    }

    @Test
    public void aSessionCreatedOutsideARestoreStillRunsItsWorkImmediately() {
        batch.runOrDefer(this::countOneRun);
        batch.runOrDefer(this::countOneRun);

        Assert.assertEquals("a session the user creates one at a time must publish its state at once,"
                + " because nothing else is going to publish it",
            2, runCount);
    }

    @Test
    public void aRestoreThatCreatedNoSessionPublishesNothing() {
        batch.begin();

        batch.end(this::countOneRun);

        Assert.assertEquals("a restore that created nothing changed nothing", 0, runCount);
    }

    @Test
    public void workIsPublishedOnceWhenTheOutermostRestoreFinishes() {
        batch.begin();
        batch.begin();
        createThreeSessions();
        batch.end(this::countOneRun);

        Assert.assertEquals("an inner batch finishing does not mean the restore finished",
            0, runCount);

        batch.end(this::countOneRun);

        Assert.assertEquals("the restore that finished last is the one that publishes",
            1, runCount);
    }
}
