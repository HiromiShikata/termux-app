package com.termux.app.browser;

import org.junit.Assert;
import org.junit.Test;

public class BrowserTabPersistenceBatchTest {

    @Test
    public void aWriteRequestedOutsideABatchHappensImmediately() {
        BrowserTabPersistenceBatch batch = new BrowserTabPersistenceBatch();
        Assert.assertTrue(batch.requestWrite());
        Assert.assertTrue(batch.requestWrite());
    }

    @Test
    public void everyWriteRequestedInsideABatchIsDeferred() {
        BrowserTabPersistenceBatch batch = new BrowserTabPersistenceBatch();
        batch.begin();
        Assert.assertFalse(batch.requestWrite());
        Assert.assertFalse(batch.requestWrite());
        Assert.assertFalse(batch.requestWrite());
    }

    @Test
    public void closingABatchThatDeferredWritesMakesExactlyOneWriteDue() {
        BrowserTabPersistenceBatch batch = new BrowserTabPersistenceBatch();
        batch.begin();
        batch.requestWrite();
        batch.requestWrite();
        Assert.assertTrue(batch.end());
    }

    @Test
    public void closingABatchThatDeferredNothingLeavesNoWriteDue() {
        BrowserTabPersistenceBatch batch = new BrowserTabPersistenceBatch();
        batch.begin();
        Assert.assertFalse(batch.end());
    }

    @Test
    public void aBatchDoesNotKeepTheDeferredWriteDueForTheNextBatch() {
        BrowserTabPersistenceBatch batch = new BrowserTabPersistenceBatch();
        batch.begin();
        batch.requestWrite();
        Assert.assertTrue(batch.end());
        batch.begin();
        Assert.assertFalse(batch.end());
    }

    @Test
    public void nestedBatchesWriteOnlyWhenTheOutermostOneCloses() {
        BrowserTabPersistenceBatch batch = new BrowserTabPersistenceBatch();
        batch.begin();
        batch.begin();
        batch.requestWrite();
        Assert.assertFalse(batch.end());
        Assert.assertTrue(batch.end());
    }

    @Test
    public void closingABatchThatWasNeverOpenedLeavesWritesImmediate() {
        BrowserTabPersistenceBatch batch = new BrowserTabPersistenceBatch();
        Assert.assertFalse(batch.end());
        Assert.assertTrue(batch.requestWrite());
    }
}
