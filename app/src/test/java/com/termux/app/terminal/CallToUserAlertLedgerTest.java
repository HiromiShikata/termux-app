package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

public class CallToUserAlertLedgerTest {

    private static final class InMemoryStorage implements CallToUserAlertLedger.Storage {
        private String mValue;

        @Override
        public String read() {
            return mValue;
        }

        @Override
        public void write(String value) {
            mValue = value;
        }
    }

    @Test
    public void newCallIdentityIsAcceptedOnceAndRejectedAfterwards() {
        CallToUserAlertLedger ledger = new CallToUserAlertLedger(new InMemoryStorage());

        Assert.assertTrue(ledger.markAlertedIfNew("worker", "call 1 out 1 reply 0 needs approval"));
        Assert.assertFalse(ledger.markAlertedIfNew("worker", "call 1 out 1 reply 0 needs approval"));
    }

    @Test
    public void sameIdentityAfterSimulatedRestartIsNotAlertedAgain() {
        InMemoryStorage storage = new InMemoryStorage();
        new CallToUserAlertLedger(storage).markAlertedIfNew("worker", "call 2 out 2 reply 1 review it");

        CallToUserAlertLedger restartedLedger = new CallToUserAlertLedger(storage);

        Assert.assertFalse(restartedLedger.markAlertedIfNew("worker", "call 2 out 2 reply 1 review it"));
    }

    @Test
    public void identityIsKeyedByBothSessionNameAndCallCycleKey() {
        IdentityCase[] cases = {
            new IdentityCase("worker", "key-a", "worker", "key-b", true),
            new IdentityCase("worker", "key-a", "other", "key-a", true),
            new IdentityCase("worker", "key-a", "worker", "key-a", false),
        };
        for (IdentityCase testCase : cases) {
            CallToUserAlertLedger ledger = new CallToUserAlertLedger(new InMemoryStorage());
            ledger.markAlertedIfNew(testCase.firstSession, testCase.firstKey);
            Assert.assertEquals(testCase.toString(), testCase.expectNew,
                ledger.markAlertedIfNew(testCase.secondSession, testCase.secondKey));
        }
    }

    @Test
    public void fieldsContainingSeparatorCharactersStayDistinct() {
        CallToUserAlertLedger ledger = new CallToUserAlertLedger(new InMemoryStorage());

        Assert.assertTrue(ledger.markAlertedIfNew("a\tb", "c"));
        Assert.assertTrue(ledger.markAlertedIfNew("a", "b\tc"));
        Assert.assertTrue(ledger.markAlertedIfNew("a\nb", "c"));
        Assert.assertFalse(ledger.markAlertedIfNew("a\tb", "c"));
    }

    @Test
    public void separatorCharactersSurviveAPersistedRoundTrip() {
        InMemoryStorage storage = new InMemoryStorage();
        new CallToUserAlertLedger(storage).markAlertedIfNew("a\nb\\c", "key\twith\nbreaks");

        Assert.assertFalse(new CallToUserAlertLedger(storage)
            .markAlertedIfNew("a\nb\\c", "key\twith\nbreaks"));
        Assert.assertTrue(new CallToUserAlertLedger(storage)
            .markAlertedIfNew("a\nb\\c", "key\twith\nbreaks-other"));
    }

    @Test
    public void oldestIdentitiesAreDroppedOnceTheCapIsExceeded() {
        InMemoryStorage storage = new InMemoryStorage();
        CallToUserAlertLedger ledger = new CallToUserAlertLedger(storage);
        for (int i = 0; i <= CallToUserAlertLedger.MAX_IDENTITIES; i++) {
            ledger.markAlertedIfNew("worker", "key-" + i);
        }

        CallToUserAlertLedger restartedLedger = new CallToUserAlertLedger(storage);

        Assert.assertTrue(restartedLedger.markAlertedIfNew("worker", "key-0"));
        Assert.assertFalse(restartedLedger.markAlertedIfNew("worker",
            "key-" + CallToUserAlertLedger.MAX_IDENTITIES));
    }

    private static final class IdentityCase {
        final String firstSession;
        final String firstKey;
        final String secondSession;
        final String secondKey;
        final boolean expectNew;

        IdentityCase(String firstSession, String firstKey, String secondSession, String secondKey,
               boolean expectNew) {
            this.firstSession = firstSession;
            this.firstKey = firstKey;
            this.secondSession = secondSession;
            this.secondKey = secondKey;
            this.expectNew = expectNew;
        }

        @Override
        public String toString() {
            return firstSession + "/" + firstKey + " then " + secondSession + "/" + secondKey;
        }
    }
}
