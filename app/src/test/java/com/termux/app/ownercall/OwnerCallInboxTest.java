package com.termux.app.ownercall;

import android.os.Looper;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@RunWith(RobolectricTestRunner.class)
public class OwnerCallInboxTest {

    private static final long NOW = 1786681348000L;
    private static final long MINIMUM_READ_INTERVAL_MILLIS =
        OwnerCallInbox.MINIMUM_READ_INTERVAL_MILLIS;

    private static final String SESSION_URL =
        "https://github.com/HiromiShikata/termux-app/issues/1884";
    private static final String OTHER_SESSION_URL =
        "https://github.com/HiromiShikata/termux-app/issues/1885";
    private static final String SESSION_FILE_URL =
        "https://calls.example.test/in-tmux-by-human/call-to-user/umino/"
            + "https___github_com_HiromiShikata_termux-app_issues_1884.yaml?k=token";
    private static final String OTHER_SESSION_FILE_URL =
        "https://calls.example.test/in-tmux-by-human/call-to-user/umino/"
            + "https___github_com_HiromiShikata_termux-app_issues_1885.yaml?k=token";

    private static final String TWO_CALLS = document(
        "https_//github_com/HiromiShikata/termux-app/issues/1884", "2026-08-14T04:22:28Z",
        "Decide whether the previous addresses may be deleted in bulk.")
        + document("https_//github_com/HiromiShikata/termux-app/issues/1884",
        "2026-08-14T04:40:00Z", "Decide whether the invoice recipient may be changed.");

    private static final String FOREIGN_CALL = document(
        "https_//github_com/HiromiShikata/termux-app/issues/9999", "2026-08-14T04:22:28Z",
        "This document belongs to another session.");

    private static String document(String sessionName, String calledAt, String body) {
        return "---\nsessionName: \"" + sessionName + "\"\ncalledAt: \"" + calledAt
            + "\"\nbody: |2\n  " + body + "\n";
    }

    private static final class RecordingTransport implements OwnerCallFileTransport {

        private final Map<String, String> filesByUrl = new HashMap<>();
        private final List<String> fetchedUrls = Collections.synchronizedList(new ArrayList<>());
        private final List<String> deletedUrls = Collections.synchronizedList(new ArrayList<>());
        private boolean deletionFails;

        void register(String url, String file) {
            filesByUrl.put(url, file);
        }

        void refuseDeletion() {
            deletionFails = true;
        }

        @Override
        public String fetch(String url) throws IOException {
            fetchedUrls.add(url);
            String file = filesByUrl.get(url);
            if (file == null) {
                throw new IOException("no owner call file registered for " + url);
            }
            return file;
        }

        @Override
        public void delete(String url) throws IOException {
            if (deletionFails) {
                throw new IOException("Unexpected HTTP response code 500 for " + url);
            }
            deletedUrls.add(url);
            filesByUrl.remove(url);
        }
    }

    private static void flushMainLooper() throws InterruptedException {
        for (int attempt = 0; attempt < 200; attempt++) {
            Shadows.shadowOf(Looper.getMainLooper()).idle();
            Thread.sleep(5);
        }
    }

    @Test
    public void readsTheCallsOfTheOpenedSessionFromItsOwnFile() throws Exception {
        RecordingTransport transport = new RecordingTransport();
        transport.register(SESSION_FILE_URL, TWO_CALLS);
        OwnerCallInbox inbox = new OwnerCallInbox(transport);
        AtomicInteger changes = new AtomicInteger();

        inbox.refreshFor(SESSION_URL, true, SESSION_FILE_URL, NOW, changes::incrementAndGet);
        flushMainLooper();

        Assert.assertEquals(Collections.singletonList(SESSION_FILE_URL), transport.fetchedUrls);
        Assert.assertEquals(2, inbox.callsFor(SESSION_URL).size());
        Assert.assertEquals("Decide whether the previous addresses may be deleted in bulk.",
            inbox.callsFor(SESSION_URL).get(0).getBody());
        Assert.assertEquals(1, changes.get());
    }

    @Test
    public void fetchesNothingForASessionThatIsNotCalling() throws Exception {
        RecordingTransport transport = new RecordingTransport();
        transport.register(SESSION_FILE_URL, TWO_CALLS);
        OwnerCallInbox inbox = new OwnerCallInbox(transport);

        inbox.refreshFor(SESSION_URL, false, SESSION_FILE_URL, NOW, () -> {
        });
        flushMainLooper();

        Assert.assertTrue(transport.fetchedUrls.isEmpty());
        Assert.assertTrue(inbox.callsFor(SESSION_URL).isEmpty());
    }

    @Test
    public void fetchesTheFileOfTheOpenedSessionOnly() throws Exception {
        RecordingTransport transport = new RecordingTransport();
        transport.register(SESSION_FILE_URL, TWO_CALLS);
        transport.register(OTHER_SESSION_FILE_URL, FOREIGN_CALL);
        OwnerCallInbox inbox = new OwnerCallInbox(transport);

        inbox.refreshFor(SESSION_URL, true, SESSION_FILE_URL, NOW, () -> {
        });
        flushMainLooper();
        inbox.refreshFor(SESSION_URL, true, SESSION_FILE_URL,
            NOW + MINIMUM_READ_INTERVAL_MILLIS * 3, () -> {
            });
        flushMainLooper();

        Assert.assertFalse(transport.fetchedUrls.contains(OTHER_SESSION_FILE_URL));
        Assert.assertTrue("only the opened session holds calls",
            inbox.callsFor(OTHER_SESSION_URL).isEmpty());
    }

    @Test
    public void stopsShowingTheCallsOnceTheFileNoLongerHoldsThem() throws Exception {
        RecordingTransport transport = new RecordingTransport();
        transport.register(SESSION_FILE_URL, TWO_CALLS);
        OwnerCallInbox inbox = new OwnerCallInbox(transport);
        inbox.refreshFor(SESSION_URL, true, SESSION_FILE_URL, NOW, () -> {
        });
        flushMainLooper();
        Assert.assertEquals(2, inbox.callsFor(SESSION_URL).size());
        AtomicInteger changes = new AtomicInteger();

        transport.register(SESSION_FILE_URL, "");
        inbox.refreshFor(SESSION_URL, true, SESSION_FILE_URL, NOW + MINIMUM_READ_INTERVAL_MILLIS,
            changes::incrementAndGet);
        flushMainLooper();

        Assert.assertTrue("the answered calls stop being shown once the file is emptied",
            inbox.callsFor(SESSION_URL).isEmpty());
        Assert.assertEquals(1, changes.get());
    }

    @Test
    public void showsACallAppendedAfterTheFileWasAlreadyRead() throws Exception {
        RecordingTransport transport = new RecordingTransport();
        transport.register(SESSION_FILE_URL, TWO_CALLS);
        OwnerCallInbox inbox = new OwnerCallInbox(transport);
        inbox.refreshFor(SESSION_URL, true, SESSION_FILE_URL, NOW, () -> {
        });
        flushMainLooper();
        Assert.assertEquals(2, inbox.callsFor(SESSION_URL).size());

        transport.register(SESSION_FILE_URL, TWO_CALLS
            + document("https_//github_com/HiromiShikata/termux-app/issues/1884",
            "2026-08-14T05:10:00Z", "Decide whether the release may go out today."));
        inbox.refreshFor(SESSION_URL, true, SESSION_FILE_URL, NOW + MINIMUM_READ_INTERVAL_MILLIS,
            () -> {
            });
        flushMainLooper();

        Assert.assertEquals(3, inbox.callsFor(SESSION_URL).size());
        Assert.assertEquals("Decide whether the release may go out today.",
            inbox.callsFor(SESSION_URL).get(2).getBody());
    }

    @Test
    public void showsNoCallOfAFileThatBelongsToAnotherSession() throws Exception {
        RecordingTransport transport = new RecordingTransport();
        transport.register(SESSION_FILE_URL, FOREIGN_CALL);
        OwnerCallInbox inbox = new OwnerCallInbox(transport);

        inbox.refreshFor(SESSION_URL, true, SESSION_FILE_URL, NOW, () -> {
        });
        flushMainLooper();

        Assert.assertTrue(inbox.callsFor(SESSION_URL).isEmpty());
    }

    @Test
    public void replacesTheHeldCallsWhenAnotherCallingSessionIsOpened() throws Exception {
        RecordingTransport transport = new RecordingTransport();
        transport.register(SESSION_FILE_URL, TWO_CALLS);
        transport.register(OTHER_SESSION_FILE_URL, document(
            "https_//github_com/HiromiShikata/termux-app/issues/1885", "2026-08-14T07:00:00Z",
            "Decide whether the branch may be deleted."));
        OwnerCallInbox inbox = new OwnerCallInbox(transport);

        inbox.refreshFor(SESSION_URL, true, SESSION_FILE_URL, NOW, () -> {
        });
        flushMainLooper();
        inbox.refreshFor(OTHER_SESSION_URL, true, OTHER_SESSION_FILE_URL, NOW, () -> {
        });
        flushMainLooper();

        Assert.assertEquals(1, inbox.callsFor(OTHER_SESSION_URL).size());
        Assert.assertTrue(inbox.callsFor(SESSION_URL).isEmpty());
    }

    @Test
    public void deletesTheFileOfTheSessionTheOwnerRepliedTo() throws Exception {
        RecordingTransport transport = new RecordingTransport();
        transport.register(SESSION_FILE_URL, TWO_CALLS);
        OwnerCallInbox inbox = new OwnerCallInbox(transport);
        inbox.refreshFor(SESSION_URL, true, SESSION_FILE_URL, NOW, () -> {
        });
        flushMainLooper();
        AtomicInteger changes = new AtomicInteger();

        inbox.deleteAnsweredCalls(SESSION_URL, SESSION_FILE_URL, changes::incrementAndGet);
        flushMainLooper();

        Assert.assertEquals(Collections.singletonList(SESSION_FILE_URL), transport.deletedUrls);
        Assert.assertTrue(inbox.callsFor(SESSION_URL).isEmpty());
        Assert.assertEquals(1, changes.get());
    }

    @Test
    public void readsTheFileAgainWhenTheSessionCallsAfterTheOwnerAnsweredTheEarlierCalls()
        throws Exception {
        RecordingTransport transport = new RecordingTransport();
        transport.register(SESSION_FILE_URL, TWO_CALLS);
        OwnerCallInbox inbox = new OwnerCallInbox(transport);

        inbox.refreshFor(SESSION_URL, true, SESSION_FILE_URL, NOW, () -> {
        });
        flushMainLooper();
        inbox.refreshFor(SESSION_URL, false, SESSION_FILE_URL, NOW, () -> {
        });
        flushMainLooper();
        inbox.refreshFor(SESSION_URL, true, SESSION_FILE_URL, NOW, () -> {
        });
        flushMainLooper();

        Assert.assertEquals(2, transport.fetchedUrls.size());
        Assert.assertEquals(2, inbox.callsFor(SESSION_URL).size());
    }

    @Test
    public void showsNothingWhenTheOwnerCallFileCannotBeReached() throws Exception {
        AtomicInteger changes = new AtomicInteger();
        OwnerCallInbox inbox = new OwnerCallInbox(
            failingTransport(new IOException("failed to connect to calls.example.test")));

        inbox.refreshFor(SESSION_URL, true, SESSION_FILE_URL, NOW, changes::incrementAndGet);
        flushMainLooper();

        Assert.assertTrue(inbox.callsFor(SESSION_URL).isEmpty());
        Assert.assertEquals(0, changes.get());
    }

    @Test
    public void showsNothingWhenTheServerRejectsTheAccessToken() throws Exception {
        AtomicInteger changes = new AtomicInteger();
        OwnerCallInbox inbox = new OwnerCallInbox(
            failingTransport(new IOException("Unexpected HTTP response code 401")));

        inbox.refreshFor(SESSION_URL, true, SESSION_FILE_URL, NOW, changes::incrementAndGet);
        flushMainLooper();

        Assert.assertTrue(inbox.callsFor(SESSION_URL).isEmpty());
        Assert.assertEquals(0, changes.get());
    }

    @Test
    public void showsNothingWhenTheServerAnswersWithSomethingOtherThanOwnerCallDocuments()
        throws Exception {
        RecordingTransport transport = new RecordingTransport();
        transport.register(SESSION_FILE_URL, "<html><body>Not Found</body></html>\n");
        OwnerCallInbox inbox = new OwnerCallInbox(transport);
        AtomicInteger changes = new AtomicInteger();

        inbox.refreshFor(SESSION_URL, true, SESSION_FILE_URL, NOW, changes::incrementAndGet);
        flushMainLooper();

        Assert.assertTrue(inbox.callsFor(SESSION_URL).isEmpty());
        Assert.assertEquals(0, changes.get());
    }

    @Test
    public void readsTheFileAgainWhenTheServerDidNotServeItYet() throws Exception {
        RecordingTransport transport = new RecordingTransport();
        transport.register(SESSION_FILE_URL, "");
        OwnerCallInbox inbox = new OwnerCallInbox(transport);
        AtomicInteger changes = new AtomicInteger();

        inbox.refreshFor(SESSION_URL, true, SESSION_FILE_URL, NOW, changes::incrementAndGet);
        flushMainLooper();
        Assert.assertTrue(inbox.callsFor(SESSION_URL).isEmpty());
        Assert.assertEquals(0, changes.get());

        transport.register(SESSION_FILE_URL, TWO_CALLS);
        inbox.refreshFor(SESSION_URL, true, SESSION_FILE_URL, NOW + MINIMUM_READ_INTERVAL_MILLIS,
            changes::incrementAndGet);
        flushMainLooper();

        Assert.assertEquals(2, inbox.callsFor(SESSION_URL).size());
        Assert.assertEquals(1, changes.get());
    }

    @Test
    public void readsTheFileAtMostOnceInTheMinimumIntervalWhileItHoldsNoCall() throws Exception {
        RecordingTransport transport = new RecordingTransport();
        transport.register(SESSION_FILE_URL, "");
        OwnerCallInbox inbox = new OwnerCallInbox(transport);

        inbox.refreshFor(SESSION_URL, true, SESSION_FILE_URL, NOW, () -> {
        });
        flushMainLooper();
        for (long screenUpdate = 1; screenUpdate < MINIMUM_READ_INTERVAL_MILLIS;
             screenUpdate += MINIMUM_READ_INTERVAL_MILLIS / 10) {
            inbox.refreshFor(SESSION_URL, true, SESSION_FILE_URL, NOW + screenUpdate, () -> {
            });
        }
        flushMainLooper();

        Assert.assertEquals(Collections.singletonList(SESSION_FILE_URL), transport.fetchedUrls);

        inbox.refreshFor(SESSION_URL, true, SESSION_FILE_URL, NOW + MINIMUM_READ_INTERVAL_MILLIS,
            () -> {
            });
        flushMainLooper();

        Assert.assertEquals(2, transport.fetchedUrls.size());
    }

    @Test
    public void readsTheFileAtMostOnceInTheMinimumIntervalWhileItHoldsCalls() throws Exception {
        RecordingTransport transport = new RecordingTransport();
        transport.register(SESSION_FILE_URL, TWO_CALLS);
        OwnerCallInbox inbox = new OwnerCallInbox(transport);

        inbox.refreshFor(SESSION_URL, true, SESSION_FILE_URL, NOW, () -> {
        });
        flushMainLooper();
        inbox.refreshFor(SESSION_URL, true, SESSION_FILE_URL, NOW + 1, () -> {
        });
        flushMainLooper();

        Assert.assertEquals(Collections.singletonList(SESSION_FILE_URL), transport.fetchedUrls);

        inbox.refreshFor(SESSION_URL, true, SESSION_FILE_URL,
            NOW + MINIMUM_READ_INTERVAL_MILLIS * 5, () -> {
            });
        flushMainLooper();

        Assert.assertEquals(2, transport.fetchedUrls.size());
        Assert.assertEquals(2, inbox.callsFor(SESSION_URL).size());
    }

    @Test
    public void opensTheSessionWithoutWaitingForTheOwnerCallFile() throws Exception {
        CountDownLatch fetchStarted = new CountDownLatch(1);
        CountDownLatch fetchMayFinish = new CountDownLatch(1);
        OwnerCallInbox inbox = new OwnerCallInbox(new OwnerCallFileTransport() {
            @Override
            public String fetch(String url) throws IOException {
                fetchStarted.countDown();
                try {
                    fetchMayFinish.await(5, TimeUnit.SECONDS);
                } catch (InterruptedException interruptedFetch) {
                    throw new IOException(interruptedFetch);
                }
                return TWO_CALLS;
            }

            @Override
            public void delete(String url) {
            }
        });

        inbox.refreshFor(SESSION_URL, true, SESSION_FILE_URL, NOW, () -> {
        });

        Assert.assertTrue("the file is read off the thread that opens the session",
            fetchStarted.await(5, TimeUnit.SECONDS));
        Assert.assertTrue(inbox.callsFor(SESSION_URL).isEmpty());

        fetchMayFinish.countDown();
        flushMainLooper();

        Assert.assertEquals(2, inbox.callsFor(SESSION_URL).size());
    }

    @Test
    public void closesTheAnsweredCallsEvenWhenTheServerCannotDeleteTheFile() throws Exception {
        RecordingTransport transport = new RecordingTransport();
        transport.register(SESSION_FILE_URL, TWO_CALLS);
        transport.refuseDeletion();
        OwnerCallInbox inbox = new OwnerCallInbox(transport);
        inbox.refreshFor(SESSION_URL, true, SESSION_FILE_URL, NOW, () -> {
        });
        flushMainLooper();
        AtomicInteger changes = new AtomicInteger();

        inbox.deleteAnsweredCalls(SESSION_URL, SESSION_FILE_URL, changes::incrementAndGet);
        flushMainLooper();

        Assert.assertTrue(inbox.callsFor(SESSION_URL).isEmpty());
        Assert.assertEquals(1, changes.get());
    }

    private static OwnerCallFileTransport failingTransport(IOException failure) {
        return new OwnerCallFileTransport() {
            @Override
            public String fetch(String url) throws IOException {
                throw failure;
            }

            @Override
            public void delete(String url) throws IOException {
                throw failure;
            }
        };
    }
}
