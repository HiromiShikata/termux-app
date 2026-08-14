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
import java.util.concurrent.atomic.AtomicInteger;

@RunWith(RobolectricTestRunner.class)
public class OwnerCallInboxTest {

    private static final String SESSION_URL =
        "https://github.com/HiromiShikata/termux-app/issues/1884";
    private static final String OTHER_SESSION_URL =
        "https://github.com/HiromiShikata/termux-app/issues/1885";
    private static final String SESSION_FILE_URL =
        "https://calls.example.test/call-to-user/umino/"
            + "https___github_com_HiromiShikata_termux-app_issues_1884.yaml?k=token";
    private static final String OTHER_SESSION_FILE_URL =
        "https://calls.example.test/call-to-user/umino/"
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

        void register(String url, String file) {
            filesByUrl.put(url, file);
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
        public void delete(String url) {
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

        inbox.refreshFor(SESSION_URL, true, SESSION_FILE_URL, changes::incrementAndGet);
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

        inbox.refreshFor(SESSION_URL, false, SESSION_FILE_URL, () -> {
        });
        flushMainLooper();

        Assert.assertTrue(transport.fetchedUrls.isEmpty());
        Assert.assertTrue(inbox.callsFor(SESSION_URL).isEmpty());
    }

    @Test
    public void fetchesTheFileOfOneSessionOnlyAndOnlyOncePerOpening() throws Exception {
        RecordingTransport transport = new RecordingTransport();
        transport.register(SESSION_FILE_URL, TWO_CALLS);
        transport.register(OTHER_SESSION_FILE_URL, FOREIGN_CALL);
        OwnerCallInbox inbox = new OwnerCallInbox(transport);

        inbox.refreshFor(SESSION_URL, true, SESSION_FILE_URL, () -> {
        });
        flushMainLooper();
        inbox.refreshFor(SESSION_URL, true, SESSION_FILE_URL, () -> {
        });
        flushMainLooper();

        Assert.assertEquals(Collections.singletonList(SESSION_FILE_URL), transport.fetchedUrls);
        Assert.assertTrue("only the opened session holds calls",
            inbox.callsFor(OTHER_SESSION_URL).isEmpty());
    }

    @Test
    public void showsNoCallOfAFileThatBelongsToAnotherSession() throws Exception {
        RecordingTransport transport = new RecordingTransport();
        transport.register(SESSION_FILE_URL, FOREIGN_CALL);
        OwnerCallInbox inbox = new OwnerCallInbox(transport);

        inbox.refreshFor(SESSION_URL, true, SESSION_FILE_URL, () -> {
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

        inbox.refreshFor(SESSION_URL, true, SESSION_FILE_URL, () -> {
        });
        flushMainLooper();
        inbox.refreshFor(OTHER_SESSION_URL, true, OTHER_SESSION_FILE_URL, () -> {
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
        inbox.refreshFor(SESSION_URL, true, SESSION_FILE_URL, () -> {
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

        inbox.refreshFor(SESSION_URL, true, SESSION_FILE_URL, () -> {
        });
        flushMainLooper();
        inbox.refreshFor(SESSION_URL, false, SESSION_FILE_URL, () -> {
        });
        flushMainLooper();
        inbox.refreshFor(SESSION_URL, true, SESSION_FILE_URL, () -> {
        });
        flushMainLooper();

        Assert.assertEquals(2, transport.fetchedUrls.size());
        Assert.assertEquals(2, inbox.callsFor(SESSION_URL).size());
    }

    @Test
    public void holdsNoCallWhenTheFileCannotBeRead() throws Exception {
        RecordingTransport transport = new RecordingTransport();
        OwnerCallInbox inbox = new OwnerCallInbox(transport);

        inbox.refreshFor(SESSION_URL, true, SESSION_FILE_URL, () -> {
        });
        flushMainLooper();

        Assert.assertTrue(inbox.callsFor(SESSION_URL).isEmpty());
    }
}
