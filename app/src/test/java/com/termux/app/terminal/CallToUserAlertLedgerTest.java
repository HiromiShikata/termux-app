package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

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

    @Test
    public void pruningDropsOnlyTheIdentitiesOfSessionsThatNoLongerExist() {
        InMemoryStorage storage = new InMemoryStorage();
        CallToUserAlertLedger ledger = new CallToUserAlertLedger(storage);
        ledger.markAlertedIfNew("ended", "key-1");
        ledger.markAlertedIfNew("live", "key-2");

        ledger.pruneToSessionNames(new HashSet<>(Collections.singletonList("live")));

        CallToUserAlertLedger restartedLedger = new CallToUserAlertLedger(storage);
        Assert.assertEquals(1, restartedLedger.size());
        Assert.assertTrue(restartedLedger.markAlertedIfNew("ended", "key-1"));
        Assert.assertFalse(restartedLedger.markAlertedIfNew("live", "key-2"));
    }

    @Test
    public void pruningWithAllSessionsLiveKeepsEveryIdentity() {
        InMemoryStorage storage = new InMemoryStorage();
        CallToUserAlertLedger ledger = new CallToUserAlertLedger(storage);
        ledger.markAlertedIfNew("a", "key-1");
        ledger.markAlertedIfNew("b", "key-2");

        ledger.pruneToSessionNames(new HashSet<>(Arrays.asList("a", "b")));

        Assert.assertEquals(2, new CallToUserAlertLedger(storage).size());
    }

    @Test
    public void pruningToAnEmptyLiveSetDropsEverythingAndPersistsIt() {
        InMemoryStorage storage = new InMemoryStorage();
        CallToUserAlertLedger ledger = new CallToUserAlertLedger(storage);
        ledger.pruneToSessionNames(Collections.<String>emptySet());
        Assert.assertEquals(0, ledger.size());

        ledger.markAlertedIfNew("gone", "key-1");
        ledger.pruneToSessionNames(Collections.<String>emptySet());

        Assert.assertEquals(0, new CallToUserAlertLedger(storage).size());
    }

    private static final class AlreadyAlertedCase {
        final String alertedSession;
        final String alertedKey;
        final String probeSession;
        final String probeKey;
        final String probeValue;
        final boolean expectAlreadyAlerted;

        AlreadyAlertedCase(String alertedSession, String alertedKey, String probeSession, String probeKey,
                           String probeValue, boolean expectAlreadyAlerted) {
            this.alertedSession = alertedSession;
            this.alertedKey = alertedKey;
            this.probeSession = probeSession;
            this.probeKey = probeKey;
            this.probeValue = probeValue;
            this.expectAlreadyAlerted = expectAlreadyAlerted;
        }
    }

    @Test
    public void aBareTagValueIsAlreadyAlertedOnlyWhenTheSessionAlertedThatValue() {
        String tokenedKey = "call:10:00:01 out:10:00:02 reply:10:00:00 first";
        AlreadyAlertedCase[] cases = {
            new AlreadyAlertedCase("worker", tokenedKey, "worker", "first", "first", true),
            new AlreadyAlertedCase("worker", tokenedKey, "worker", "second", "second", false),
            new AlreadyAlertedCase("worker", tokenedKey, "other", "first", "first", false),
            new AlreadyAlertedCase("worker", tokenedKey, "worker", tokenedKey, "first", true),
            new AlreadyAlertedCase("worker", "first", "worker", "first", "first", true),
            new AlreadyAlertedCase("worker", "first", "worker", "second", "second", false),
        };
        for (AlreadyAlertedCase testCase : cases) {
            CallToUserAlertLedger ledger = new CallToUserAlertLedger(new InMemoryStorage());
            Assert.assertTrue(ledger.markAlertedIfNew(testCase.alertedSession, testCase.alertedKey,
                valueOf(testCase.alertedKey)));
            Assert.assertEquals(testCase.probeSession + " " + testCase.probeKey, testCase.expectAlreadyAlerted,
                ledger.isAlreadyAlerted(testCase.probeSession, testCase.probeKey, testCase.probeValue));
        }
    }

    @Test
    public void aBareTagValueWithTheSameTextIsANewIdentityForMarking() {
        CallToUserAlertLedger ledger = new CallToUserAlertLedger(new InMemoryStorage());
        Assert.assertTrue(ledger.markAlertedIfNew("worker", "call:1 out:2 reply:0 first", "first"));

        Assert.assertTrue(ledger.markAlertedIfNew("worker", "first", "first"));
        Assert.assertFalse(ledger.markAlertedIfNew("worker", "first", "first"));
    }

    @Test
    public void anAlreadyAlertedBareTagValueSurvivesARestart() {
        InMemoryStorage storage = new InMemoryStorage();
        new CallToUserAlertLedger(storage).recordAlerted("worker", "first", "first");

        CallToUserAlertLedger restartedLedger = new CallToUserAlertLedger(storage);

        Assert.assertTrue(restartedLedger.isAlreadyAlerted("worker", "first", "first"));
        Assert.assertFalse(restartedLedger.isAlreadyAlerted("worker", "second", "second"));
    }

    private static String valueOf(String key) {
        int tokenEnd = key.lastIndexOf(' ');
        return tokenEnd < 0 ? key : key.substring(tokenEnd + 1);
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
