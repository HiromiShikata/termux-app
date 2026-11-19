package com.termux.app.apkupdate;

import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class DownloadFlagReleaseTest {

    private static final String FAILURE_MESSAGE = "persist failed";

    @Test
    public void clearsFlagAfterPersistCompletes() {
        AtomicBoolean downloadInProgress = new AtomicBoolean(true);
        AtomicInteger persisted = new AtomicInteger();

        DownloadFlagRelease.releaseAfter(downloadInProgress, persisted::incrementAndGet);

        Assert.assertEquals(1, persisted.get());
        Assert.assertFalse(downloadInProgress.get());
    }

    @Test
    public void clearsFlagAndPropagatesWhenPersistThrows() {
        AtomicBoolean downloadInProgress = new AtomicBoolean(true);
        AtomicReference<IllegalStateException> propagated = new AtomicReference<>();

        try {
            DownloadFlagRelease.releaseAfter(downloadInProgress, () -> {
                throw new IllegalStateException(FAILURE_MESSAGE);
            });
        } catch (IllegalStateException exception) {
            propagated.set(exception);
        }

        Assert.assertNotNull(propagated.get());
        Assert.assertEquals(FAILURE_MESSAGE, propagated.get().getMessage());
        Assert.assertFalse(downloadInProgress.get());
    }

    @Test
    public void keepsFlagHeldWhilePersistRuns() {
        AtomicBoolean downloadInProgress = new AtomicBoolean(true);
        AtomicReference<Boolean> observedInsidePersist = new AtomicReference<>();

        DownloadFlagRelease.releaseAfter(downloadInProgress, () -> observedInsidePersist.set(downloadInProgress.get()));

        Assert.assertEquals(Boolean.TRUE, observedInsidePersist.get());
        Assert.assertFalse(downloadInProgress.get());
    }
}
