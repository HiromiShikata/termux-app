package com.termux.app.sessiondefinition;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.json.JSONException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class SessionDefinitionUnansweredCallParseTest {

    private static final String SESSION_NAME = "https://github.com/demo/repo/issues/1";

    private final SessionDefinitionParser parser = new SessionDefinitionParser();

    private static String versionFiveDocument(String unansweredCallsJson) {
        return "{\"version\":5,"
            + "\"overviewUrl\":\"https://github.com/orgs/demo/projects/1\","
            + "\"tdpmConsoleUrl\":\"https://console.example.test/projects/demo?k=t\","
            + "\"newIssueUrl\":\"https://github.com/demo/repo/issues/new\","
            + "\"groups\":[{\"story\":\"Story Alpha\",\"sessions\":[{"
            + "\"name\":\"" + SESSION_NAME + "\",\"description\":\"Issue 1\","
            + "\"unansweredCalls\":" + unansweredCallsJson + "}]}]}";
    }

    @Test
    public void readsTheTimeAndBodyOfAnUnansweredCall() throws JSONException {
        List<SessionDefinitionEntry> entries = parser.parseGroup("demo", versionFiveDocument(
            "[{\"calledAt\":\"2026-08-13T10:14:00.000Z\",\"body\":\"Please decide\"}]"));

        List<UnansweredOwnerCall> calls = entries.get(0).getUnansweredCallsForUrl(SESSION_NAME);
        assertEquals(1, calls.size());
        assertEquals("2026-08-13T10:14:00.000Z", calls.get(0).getCalledAt());
        assertEquals("Please decide", calls.get(0).getBody());
    }

    @Test
    public void keepsEveryUnansweredCallOfOneSessionInDocumentOrder() throws JSONException {
        List<SessionDefinitionEntry> entries = parser.parseGroup("demo", versionFiveDocument(
            "[{\"calledAt\":\"2026-08-13T10:14:00.000Z\",\"body\":\"first\"},"
                + "{\"calledAt\":\"2026-08-13T10:41:30.000Z\",\"body\":\"second\"}]"));

        List<UnansweredOwnerCall> calls = entries.get(0).getUnansweredCallsForUrl(SESSION_NAME);
        assertEquals(2, calls.size());
        assertEquals("first", calls.get(0).getBody());
        assertEquals("second", calls.get(1).getBody());
    }

    @Test
    public void keepsTwoCallsCarryingTheSameBodySeparableByTheirTime() throws JSONException {
        List<SessionDefinitionEntry> entries = parser.parseGroup("demo", versionFiveDocument(
            "[{\"calledAt\":\"2026-08-13T10:14:00.000Z\",\"body\":\"same\"},"
                + "{\"calledAt\":\"2026-08-13T10:41:30.000Z\",\"body\":\"same\"}]"));

        List<UnansweredOwnerCall> calls = entries.get(0).getUnansweredCallsForUrl(SESSION_NAME);
        assertEquals(2, calls.size());
        assertEquals(calls.get(0).getBody(), calls.get(1).getBody());
        assertTrue(!calls.get(0).getCalledAt().equals(calls.get(1).getCalledAt()));
    }

    @Test
    public void readsNoCallForASessionWhoseArrayIsEmpty() throws JSONException {
        List<SessionDefinitionEntry> entries = parser.parseGroup("demo", versionFiveDocument("[]"));

        assertTrue(entries.get(0).getUnansweredCallsForUrl(SESSION_NAME).isEmpty());
    }

    @Test
    public void readsNoCallFromAVersionFourDocumentThatCarriesNoCallArray() throws JSONException {
        String versionFour = "{\"version\":4,\"groups\":[{\"story\":\"Story Alpha\",\"sessions\":[{"
            + "\"name\":\"" + SESSION_NAME + "\",\"description\":\"Issue 1\"}]}]}";

        List<SessionDefinitionEntry> entries = parser.parseGroup("demo", versionFour);

        assertTrue(entries.get(0).getUnansweredCallsForUrl(SESSION_NAME).isEmpty());
    }

    @Test
    public void skipsACallEntryThatCarriesNoTime() throws JSONException {
        List<SessionDefinitionEntry> entries = parser.parseGroup("demo", versionFiveDocument(
            "[{\"body\":\"no time\"},{\"calledAt\":\"2026-08-13T10:14:00.000Z\",\"body\":\"kept\"}]"));

        List<UnansweredOwnerCall> calls = entries.get(0).getUnansweredCallsForUrl(SESSION_NAME);
        assertEquals(1, calls.size());
        assertEquals("kept", calls.get(0).getBody());
    }

    @Test
    public void readsTheVersionFiveIndexAsNameAndPathEntries() throws JSONException {
        List<SessionDefinitionGroupReference> references = parser.parseIndex(
            "{\"version\":5,\"projects\":[{\"name\":\"demo\",\"path\":\"/jsonpub/demo.v5.json?k=t\"}]}",
            "https://console.example.test/jsonpub/index.v5.json?k=t");

        assertEquals(1, references.size());
        assertEquals("demo", references.get(0).getLabel());
        assertEquals("/jsonpub/demo.v5.json?k=t", references.get(0).getUrl());
    }
}
