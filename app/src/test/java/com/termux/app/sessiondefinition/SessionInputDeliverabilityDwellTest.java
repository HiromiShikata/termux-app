package com.termux.app.sessiondefinition;

import org.junit.Assert;
import org.junit.Test;

public class SessionInputDeliverabilityDwellTest {

    private final SessionInputDeliverabilityDwell dwell = new SessionInputDeliverabilityDwell();

    @Test
    public void aSessionThatCanReceiveInputIsNeverReported() {
        Assert.assertFalse(dwell.hasBeenUnableToReceiveInputLongEnough("a", true, 1_000L));
        Assert.assertFalse(dwell.hasBeenUnableToReceiveInputLongEnough(
            "a", true, 1_000L + SessionInputDeliverabilityDwell.DWELL_MILLIS * 10));
    }

    @Test
    public void aSessionThatHasJustStartedIsNotReconnectedOnTheFirstObservation() {
        Assert.assertFalse(dwell.hasBeenUnableToReceiveInputLongEnough("a", false, 1_000L));
        Assert.assertFalse(dwell.hasBeenUnableToReceiveInputLongEnough(
            "a", false, 1_000L + SessionInputDeliverabilityDwell.DWELL_MILLIS - 1));
    }

    @Test
    public void aSessionThatStillCannotReceiveInputAfterTheDwellIsReported() {
        dwell.hasBeenUnableToReceiveInputLongEnough("a", false, 1_000L);

        Assert.assertTrue(dwell.hasBeenUnableToReceiveInputLongEnough(
            "a", false, 1_000L + SessionInputDeliverabilityDwell.DWELL_MILLIS));
    }

    @Test
    public void aSessionThatRecoveredInBetweenStartsTheDwellAgain() {
        dwell.hasBeenUnableToReceiveInputLongEnough("a", false, 1_000L);
        dwell.hasBeenUnableToReceiveInputLongEnough("a", true, 2_000L);

        Assert.assertFalse(dwell.hasBeenUnableToReceiveInputLongEnough(
            "a", false, 1_000L + SessionInputDeliverabilityDwell.DWELL_MILLIS));
    }

    @Test
    public void eachSessionKeepsItsOwnDwell() {
        dwell.hasBeenUnableToReceiveInputLongEnough("a", false, 1_000L);

        Assert.assertFalse(dwell.hasBeenUnableToReceiveInputLongEnough(
            "b", false, 1_000L + SessionInputDeliverabilityDwell.DWELL_MILLIS));
    }
}
