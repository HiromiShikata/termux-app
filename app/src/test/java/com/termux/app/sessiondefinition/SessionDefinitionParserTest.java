package com.termux.app.sessiondefinition;

import org.json.JSONException;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class SessionDefinitionParserTest {

    private final SessionDefinitionParser parser = new SessionDefinitionParser();

    @Test
    public void parseIndexResolvesProjectNamesToSiblingJsonReferences() throws JSONException {
        String json = "{\"projects\":[\"alpha\",\"beta\",\"gamma\"]}";

        List<SessionDefinitionGroupReference> references = parser.parseIndex(json);

        Assert.assertEquals(3, references.size());
        Assert.assertEquals("alpha", references.get(0).getLabel());
        Assert.assertEquals("alpha.json", references.get(0).getUrl());
        Assert.assertEquals("beta", references.get(1).getLabel());
        Assert.assertEquals("beta.json", references.get(1).getUrl());
        Assert.assertEquals("gamma", references.get(2).getLabel());
        Assert.assertEquals("gamma.json", references.get(2).getUrl());
    }

    @Test
    public void parseGroupPreservesStoryAndUrlOrder() throws JSONException {
        String json = "["
            + "{\"story\":\"story-one\",\"urls\":[\"https://example.test/a1\",\"https://example.test/a2\"]},"
            + "{\"story\":\"story-two\",\"urls\":[\"https://example.test/b1\"]}"
            + "]";

        List<SessionDefinitionEntry> entries = parser.parseGroup("alpha", json);

        Assert.assertEquals(2, entries.size());
        Assert.assertEquals("alpha", entries.get(0).getGroupLabel());
        Assert.assertEquals("story-one", entries.get(0).getEntryLabel());
        Assert.assertEquals(2, entries.get(0).getUrls().size());
        Assert.assertEquals("https://example.test/a1", entries.get(0).getUrls().get(0));
        Assert.assertEquals("https://example.test/a2", entries.get(0).getUrls().get(1));
        Assert.assertEquals("story-two", entries.get(1).getEntryLabel());
        Assert.assertEquals(1, entries.get(1).getUrls().size());
    }

    @Test(expected = JSONException.class)
    public void parseIndexRejectsMissingProjects() throws JSONException {
        parser.parseIndex("{}");
    }

    @Test
    public void parseIndexToleratesVersionFieldOnIndex() throws JSONException {
        String json = "{\"version\":2,\"projects\":[\"alpha\",\"beta\"]}";

        List<SessionDefinitionGroupReference> references = parser.parseIndex(json);

        Assert.assertEquals(2, references.size());
        Assert.assertEquals("alpha", references.get(0).getLabel());
        Assert.assertEquals("alpha.json", references.get(0).getUrl());
        Assert.assertEquals("beta.json", references.get(1).getUrl());
    }

    @Test
    public void parseIndexResolvesProjectNamesToVersionTwoSiblingsWhenIndexUrlIsVersionTwo() throws JSONException {
        String json = "{\"version\":2,\"projects\":[\"alpha\",\"beta\"]}";

        List<SessionDefinitionGroupReference> references =
            parser.parseIndex(json, "https://example.test/base/index.v2.json");

        Assert.assertEquals(2, references.size());
        Assert.assertEquals("alpha", references.get(0).getLabel());
        Assert.assertEquals("alpha.v2.json", references.get(0).getUrl());
        Assert.assertEquals("beta.v2.json", references.get(1).getUrl());
    }

    @Test
    public void parseIndexKeepsPlainJsonSiblingsWhenIndexUrlIsNotVersionTwo() throws JSONException {
        String json = "{\"projects\":[\"alpha\"]}";

        List<SessionDefinitionGroupReference> references =
            parser.parseIndex(json, "https://example.test/base/index.json");

        Assert.assertEquals(1, references.size());
        Assert.assertEquals("alpha.json", references.get(0).getUrl());
    }

    @Test
    public void parseIndexResolvesProjectNamesToVersionThreeSiblingsWhenIndexUrlIsVersionThree() throws JSONException {
        String json = "{\"version\":3,\"projects\":[\"umino\",\"xmile\"]}";

        List<SessionDefinitionGroupReference> references =
            parser.parseIndex(json, "https://example.test/base/index.v3.json");

        Assert.assertEquals(2, references.size());
        Assert.assertEquals("umino", references.get(0).getLabel());
        Assert.assertEquals("umino.v3.json", references.get(0).getUrl());
        Assert.assertEquals("xmile.v3.json", references.get(1).getUrl());
    }

    @Test
    public void parseIndexResolvesVersionThreeSiblingsFromVersionFieldEvenWithoutVersionThreeUrl() throws JSONException {
        String json = "{\"version\":3,\"projects\":[\"umino\"]}";

        List<SessionDefinitionGroupReference> references = parser.parseIndex(json);

        Assert.assertEquals(1, references.size());
        Assert.assertEquals("umino.v3.json", references.get(0).getUrl());
    }

    @Test
    public void parseGroupReadsVersionThreeObjectWithOverviewUrlAndGroups() throws JSONException {
        String json = "{"
            + "\"version\":3,"
            + "\"overviewUrl\":\"https://github.com/HiromiShikata/projects/7\","
            + "\"groups\":["
            + "{\"story\":\"story-one\",\"urls\":["
            + "{\"url\":\"https://example.test/a1\",\"title\":\"Task A\"},"
            + "{\"url\":\"https://example.test/a2\"}"
            + "]},"
            + "{\"story\":\"story-two\",\"urls\":[\"https://example.test/b1\"]}"
            + "]}";

        List<SessionDefinitionEntry> entries = parser.parseGroup("umino", json);

        Assert.assertEquals(2, entries.size());
        SessionDefinitionEntry first = entries.get(0);
        Assert.assertEquals("umino", first.getGroupLabel());
        Assert.assertEquals("story-one", first.getEntryLabel());
        Assert.assertEquals("https://github.com/HiromiShikata/projects/7", first.getOverviewUrl());
        Assert.assertEquals(2, first.getUrls().size());
        Assert.assertEquals("Task A", first.getTitleForUrl("https://example.test/a1"));
        Assert.assertNull(first.getTitleForUrl("https://example.test/a2"));
        SessionDefinitionEntry second = entries.get(1);
        Assert.assertEquals("story-two", second.getEntryLabel());
        Assert.assertEquals("https://github.com/HiromiShikata/projects/7", second.getOverviewUrl());
    }

    @Test
    public void parseGroupTreatsMissingOrEmptyOverviewUrlAsNoOverviewUrl() throws JSONException {
        String missingJson = "{\"version\":3,\"groups\":["
            + "{\"story\":\"story-one\",\"urls\":[\"https://example.test/a1\"]}"
            + "]}";
        String emptyJson = "{\"version\":3,\"overviewUrl\":\"\",\"groups\":["
            + "{\"story\":\"story-one\",\"urls\":[\"https://example.test/a1\"]}"
            + "]}";

        Assert.assertNull(parser.parseGroup("umino", missingJson).get(0).getOverviewUrl());
        Assert.assertNull(parser.parseGroup("umino", emptyJson).get(0).getOverviewUrl());
    }

    @Test
    public void parseGroupLeavesOverviewUrlNullForVersionOneAndTwoArrays() throws JSONException {
        String json = "[{\"story\":\"story-one\",\"urls\":[\"https://example.test/a1\"]}]";

        List<SessionDefinitionEntry> entries = parser.parseGroup("alpha", json);

        Assert.assertNull(entries.get(0).getOverviewUrl());
    }

    @Test
    public void parseGroupReadsUrlItemsAsObjectsCarryingTitles() throws JSONException {
        String json = "["
            + "{\"story\":\"story-one\",\"urls\":["
            + "{\"url\":\"https://example.test/a1\",\"title\":\"Task A\"},"
            + "{\"url\":\"https://example.test/a2\",\"title\":\"Task B\"}"
            + "]}"
            + "]";

        List<SessionDefinitionEntry> entries = parser.parseGroup("alpha", json);

        Assert.assertEquals(1, entries.size());
        SessionDefinitionEntry entry = entries.get(0);
        Assert.assertEquals("story-one", entry.getEntryLabel());
        Assert.assertEquals(2, entry.getUrls().size());
        Assert.assertEquals("https://example.test/a1", entry.getUrls().get(0));
        Assert.assertEquals("https://example.test/a2", entry.getUrls().get(1));
        Assert.assertEquals("Task A", entry.getTitleForUrl("https://example.test/a1"));
        Assert.assertEquals("Task B", entry.getTitleForUrl("https://example.test/a2"));
    }

    @Test
    public void parseGroupAcceptsMixedStringAndObjectUrlItems() throws JSONException {
        String json = "["
            + "{\"story\":\"story-one\",\"urls\":["
            + "\"https://example.test/plain\","
            + "{\"url\":\"https://example.test/titled\",\"title\":\"Task A\"}"
            + "]}"
            + "]";

        List<SessionDefinitionEntry> entries = parser.parseGroup("alpha", json);

        SessionDefinitionEntry entry = entries.get(0);
        Assert.assertEquals(2, entry.getUrls().size());
        Assert.assertEquals("https://example.test/plain", entry.getUrls().get(0));
        Assert.assertEquals("https://example.test/titled", entry.getUrls().get(1));
        Assert.assertNull(entry.getTitleForUrl("https://example.test/plain"));
        Assert.assertEquals("Task A", entry.getTitleForUrl("https://example.test/titled"));
    }

    @Test
    public void parseGroupTreatsMissingOrEmptyTitleAsNoTitle() throws JSONException {
        String json = "["
            + "{\"story\":\"story-one\",\"urls\":["
            + "{\"url\":\"https://example.test/no-title-key\"},"
            + "{\"url\":\"https://example.test/empty-title\",\"title\":\"\"}"
            + "]}"
            + "]";

        List<SessionDefinitionEntry> entries = parser.parseGroup("alpha", json);

        SessionDefinitionEntry entry = entries.get(0);
        Assert.assertNull(entry.getTitleForUrl("https://example.test/no-title-key"));
        Assert.assertNull(entry.getTitleForUrl("https://example.test/empty-title"));
    }

    @Test
    public void parseIndexReadsVersionFourProjectsAsObjectsWithNameAndPathReference() throws JSONException {
        String json = "{\"version\":4,\"projects\":["
            + "{\"name\":\"umino\",\"path\":\"/in-tmux-by-human/umino.v4.json?k=TESTKEY\"},"
            + "{\"name\":\"xmile\",\"path\":\"/in-tmux-by-human/xmile.v4.json?k=TESTKEY\"}"
            + "]}";

        List<SessionDefinitionGroupReference> references = parser.parseIndex(json);

        Assert.assertEquals(2, references.size());
        Assert.assertEquals("umino", references.get(0).getLabel());
        Assert.assertEquals("/in-tmux-by-human/umino.v4.json?k=TESTKEY", references.get(0).getUrl());
        Assert.assertEquals("xmile", references.get(1).getLabel());
        Assert.assertEquals("/in-tmux-by-human/xmile.v4.json?k=TESTKEY", references.get(1).getUrl());
    }

    @Test
    public void parseIndexReadsVersionFourProjectsAsObjectsWhenIndexUrlIsVersionFourWithQueryKey() throws JSONException {
        String json = "{\"projects\":["
            + "{\"name\":\"umino\",\"path\":\"umino.v4.json?k=TESTKEY\"}"
            + "]}";

        List<SessionDefinitionGroupReference> references =
            parser.parseIndex(json, "https://example.test/base/index.v4.json?k=TESTKEY");

        Assert.assertEquals(1, references.size());
        Assert.assertEquals("umino", references.get(0).getLabel());
        Assert.assertEquals("umino.v4.json?k=TESTKEY", references.get(0).getUrl());
    }

    @Test
    public void parseGroupReadsVersionFourObjectWithOverviewUrlTdpmConsoleUrlAndGroups() throws JSONException {
        String json = "{"
            + "\"version\":4,"
            + "\"overviewUrl\":\"https://github.com/HiromiShikata/projects/7\","
            + "\"tdpmConsoleUrl\":\"https://example.test/tdpm-console?k=TESTKEY\","
            + "\"newIssueUrl\":\"https://example.test/new-issue?k=TESTKEY\","
            + "\"groups\":["
            + "{\"story\":\"story-one\",\"urls\":["
            + "{\"url\":\"https://example.test/a1\",\"title\":\"Task A\"}"
            + "]},"
            + "{\"story\":\"story-two\",\"urls\":[\"https://example.test/b1\"]}"
            + "]}";

        List<SessionDefinitionEntry> entries = parser.parseGroup("umino", json);

        Assert.assertEquals(2, entries.size());
        SessionDefinitionEntry first = entries.get(0);
        Assert.assertEquals("https://github.com/HiromiShikata/projects/7", first.getOverviewUrl());
        Assert.assertEquals("https://example.test/tdpm-console?k=TESTKEY", first.getTdpmConsoleUrl());
        Assert.assertEquals("https://example.test/new-issue?k=TESTKEY", first.getNewIssueUrl());
        Assert.assertEquals("Task A", first.getTitleForUrl("https://example.test/a1"));
        SessionDefinitionEntry second = entries.get(1);
        Assert.assertEquals("https://example.test/tdpm-console?k=TESTKEY", second.getTdpmConsoleUrl());
        Assert.assertEquals("https://example.test/new-issue?k=TESTKEY", second.getNewIssueUrl());
    }

    @Test
    public void parseGroupTreatsMissingOrEmptyNewIssueUrlAsNoNewIssueUrl() throws JSONException {
        String missingJson = "{\"version\":4,\"overviewUrl\":\"https://example.test/o\",\"groups\":["
            + "{\"story\":\"story-one\",\"urls\":[\"https://example.test/a1\"]}"
            + "]}";
        String emptyJson = "{\"version\":4,\"newIssueUrl\":\"\",\"groups\":["
            + "{\"story\":\"story-one\",\"urls\":[\"https://example.test/a1\"]}"
            + "]}";

        Assert.assertNull(parser.parseGroup("umino", missingJson).get(0).getNewIssueUrl());
        Assert.assertNull(parser.parseGroup("umino", emptyJson).get(0).getNewIssueUrl());
    }

    @Test
    public void parseGroupLeavesNewIssueUrlNullForVersionOneAndTwoArrays() throws JSONException {
        String json = "[{\"story\":\"story-one\",\"urls\":[\"https://example.test/a1\"]}]";

        List<SessionDefinitionEntry> entries = parser.parseGroup("alpha", json);

        Assert.assertNull(entries.get(0).getNewIssueUrl());
    }

    @Test
    public void parseGroupTreatsMissingOrEmptyTdpmConsoleUrlAsNoTdpmConsoleUrl() throws JSONException {
        String missingJson = "{\"version\":4,\"overviewUrl\":\"https://example.test/o\",\"groups\":["
            + "{\"story\":\"story-one\",\"urls\":[\"https://example.test/a1\"]}"
            + "]}";
        String emptyJson = "{\"version\":4,\"tdpmConsoleUrl\":\"\",\"groups\":["
            + "{\"story\":\"story-one\",\"urls\":[\"https://example.test/a1\"]}"
            + "]}";

        Assert.assertNull(parser.parseGroup("umino", missingJson).get(0).getTdpmConsoleUrl());
        Assert.assertNull(parser.parseGroup("umino", emptyJson).get(0).getTdpmConsoleUrl());
    }

    @Test
    public void parseGroupLeavesTdpmConsoleUrlNullForVersionOneAndTwoArrays() throws JSONException {
        String json = "[{\"story\":\"story-one\",\"urls\":[\"https://example.test/a1\"]}]";

        List<SessionDefinitionEntry> entries = parser.parseGroup("alpha", json);

        Assert.assertNull(entries.get(0).getTdpmConsoleUrl());
    }

    @Test
    public void parseGroupReadsVersionFourSessionsArrayWithNameAndDescription() throws JSONException {
        String json = "{"
            + "\"version\":4,"
            + "\"overviewUrl\":\"https://github.com/HiromiShikata/projects/7\","
            + "\"tdpmConsoleUrl\":\"https://example.test/tdpm-console?k=TESTKEY\","
            + "\"newIssueUrl\":\"https://example.test/new-issue?k=TESTKEY\","
            + "\"groups\":["
            + "{\"story\":\"story-one\",\"sessions\":["
            + "{\"name\":\"https://example.test/a1?k=TESTKEY\",\"description\":\"Session A\"},"
            + "{\"name\":\"https://example.test/a2?k=TESTKEY\",\"description\":\"Session B\"}"
            + "]},"
            + "{\"story\":\"story-two\",\"sessions\":["
            + "{\"name\":\"https://example.test/b1?k=TESTKEY\",\"description\":\"Session C\"}"
            + "]}"
            + "]}";

        List<SessionDefinitionEntry> entries = parser.parseGroup("umino", json);

        Assert.assertEquals(2, entries.size());
        SessionDefinitionEntry first = entries.get(0);
        Assert.assertEquals("umino", first.getGroupLabel());
        Assert.assertEquals("story-one", first.getEntryLabel());
        Assert.assertEquals("https://github.com/HiromiShikata/projects/7", first.getOverviewUrl());
        Assert.assertEquals("https://example.test/tdpm-console?k=TESTKEY", first.getTdpmConsoleUrl());
        Assert.assertEquals("https://example.test/new-issue?k=TESTKEY", first.getNewIssueUrl());
        Assert.assertEquals(2, first.getUrls().size());
        Assert.assertEquals("https://example.test/a1?k=TESTKEY", first.getUrls().get(0));
        Assert.assertEquals("https://example.test/a2?k=TESTKEY", first.getUrls().get(1));
        Assert.assertEquals("Session A", first.getTitleForUrl("https://example.test/a1?k=TESTKEY"));
        Assert.assertEquals("Session B", first.getTitleForUrl("https://example.test/a2?k=TESTKEY"));
        SessionDefinitionEntry second = entries.get(1);
        Assert.assertEquals("story-two", second.getEntryLabel());
        Assert.assertEquals(1, second.getUrls().size());
        Assert.assertEquals("https://example.test/b1?k=TESTKEY", second.getUrls().get(0));
        Assert.assertEquals("Session C", second.getTitleForUrl("https://example.test/b1?k=TESTKEY"));
    }

    @Test
    public void parseGroupPrefersDescriptionOverTitleWhenBothPresent() throws JSONException {
        String json = "["
            + "{\"story\":\"story-one\",\"sessions\":["
            + "{\"name\":\"https://example.test/a1?k=TESTKEY\",\"description\":\"From description\",\"title\":\"From title\"}"
            + "]}"
            + "]";

        List<SessionDefinitionEntry> entries = parser.parseGroup("alpha", json);

        SessionDefinitionEntry entry = entries.get(0);
        Assert.assertEquals(1, entry.getUrls().size());
        Assert.assertEquals("https://example.test/a1?k=TESTKEY", entry.getUrls().get(0));
        Assert.assertEquals("From description", entry.getTitleForUrl("https://example.test/a1?k=TESTKEY"));
    }

    @Test
    public void parseGroupFallsBackToTitleWhenDescriptionAbsentInSessionsArray() throws JSONException {
        String json = "["
            + "{\"story\":\"story-one\",\"sessions\":["
            + "{\"name\":\"https://example.test/a1?k=TESTKEY\",\"title\":\"From title\"}"
            + "]}"
            + "]";

        List<SessionDefinitionEntry> entries = parser.parseGroup("alpha", json);

        SessionDefinitionEntry entry = entries.get(0);
        Assert.assertEquals("From title", entry.getTitleForUrl("https://example.test/a1?k=TESTKEY"));
    }

    @Test
    public void parseGroupFallsBackToUrlWhenNameAbsentInSessionsArray() throws JSONException {
        String json = "["
            + "{\"story\":\"story-one\",\"sessions\":["
            + "{\"url\":\"https://example.test/a1?k=TESTKEY\",\"description\":\"Session A\"}"
            + "]}"
            + "]";

        List<SessionDefinitionEntry> entries = parser.parseGroup("alpha", json);

        SessionDefinitionEntry entry = entries.get(0);
        Assert.assertEquals(1, entry.getUrls().size());
        Assert.assertEquals("https://example.test/a1?k=TESTKEY", entry.getUrls().get(0));
        Assert.assertEquals("Session A", entry.getTitleForUrl("https://example.test/a1?k=TESTKEY"));
    }

    @Test
    public void parseGroupAcceptsPlainStringSessionItems() throws JSONException {
        String json = "["
            + "{\"story\":\"story-one\",\"sessions\":["
            + "\"https://example.test/plain?k=TESTKEY\","
            + "{\"name\":\"https://example.test/named?k=TESTKEY\",\"description\":\"Session A\"}"
            + "]}"
            + "]";

        List<SessionDefinitionEntry> entries = parser.parseGroup("alpha", json);

        SessionDefinitionEntry entry = entries.get(0);
        Assert.assertEquals(2, entry.getUrls().size());
        Assert.assertEquals("https://example.test/plain?k=TESTKEY", entry.getUrls().get(0));
        Assert.assertEquals("https://example.test/named?k=TESTKEY", entry.getUrls().get(1));
        Assert.assertNull(entry.getTitleForUrl("https://example.test/plain?k=TESTKEY"));
        Assert.assertEquals("Session A", entry.getTitleForUrl("https://example.test/named?k=TESTKEY"));
    }

    @Test
    public void parseGroupTreatsMissingSessionsAndUrlsAsEmptySessionList() throws JSONException {
        String json = "{\"version\":4,\"groups\":["
            + "{\"story\":\"story-one\"}"
            + "]}";

        List<SessionDefinitionEntry> entries = parser.parseGroup("umino", json);

        Assert.assertEquals(1, entries.size());
        Assert.assertEquals("story-one", entries.get(0).getEntryLabel());
        Assert.assertTrue(entries.get(0).getUrls().isEmpty());
    }

    @Test
    public void resolveUrlResolvesRelativeReferenceAgainstBase() throws Exception {
        String resolved = parser.resolveUrl("https://example.test/base/index.json", "groups/first.json");
        Assert.assertEquals("https://example.test/base/groups/first.json", resolved);
    }

    @Test
    public void resolveUrlKeepsAbsoluteReference() throws Exception {
        String resolved = parser.resolveUrl("https://example.test/base/index.json", "https://other.test/first.json");
        Assert.assertEquals("https://other.test/first.json", resolved);
    }

    @Test
    public void resolveUrlPreservesQueryKeyOnAbsolutePathReference() throws Exception {
        String resolved = parser.resolveUrl(
            "https://example.test/in-tmux-by-human/index.v4.json?k=TESTKEY",
            "/in-tmux-by-human/umino.v4.json?k=TESTKEY");
        Assert.assertEquals("https://example.test/in-tmux-by-human/umino.v4.json?k=TESTKEY", resolved);
    }

    @Test
    public void resolveUrlPreservesQueryKeyOnRelativeReference() throws Exception {
        String resolved = parser.resolveUrl(
            "https://example.test/in-tmux-by-human/index.v4.json?k=TESTKEY",
            "umino.v4.json?k=TESTKEY");
        Assert.assertEquals("https://example.test/in-tmux-by-human/umino.v4.json?k=TESTKEY", resolved);
    }
}
