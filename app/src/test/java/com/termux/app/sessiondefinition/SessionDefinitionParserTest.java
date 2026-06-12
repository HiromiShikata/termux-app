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
    public void parseIndexPreservesGroupOrder() throws JSONException {
        String json = "{\"groups\":["
            + "{\"label\":\"first\",\"url\":\"first.json\"},"
            + "{\"label\":\"second\",\"url\":\"second.json\"},"
            + "{\"label\":\"third\",\"url\":\"third.json\"}"
            + "]}";

        List<SessionDefinitionGroupReference> references = parser.parseIndex(json);

        Assert.assertEquals(3, references.size());
        Assert.assertEquals("first", references.get(0).getLabel());
        Assert.assertEquals("first.json", references.get(0).getUrl());
        Assert.assertEquals("second", references.get(1).getLabel());
        Assert.assertEquals("third", references.get(2).getLabel());
    }

    @Test
    public void parseGroupPreservesEntryAndUrlOrder() throws JSONException {
        String json = "{\"entries\":["
            + "{\"label\":\"alpha\",\"urls\":[\"https://example.test/a1\",\"https://example.test/a2\"]},"
            + "{\"label\":\"beta\",\"urls\":[\"https://example.test/b1\"]}"
            + "]}";

        List<SessionDefinitionEntry> entries = parser.parseGroup("groupLabel", json);

        Assert.assertEquals(2, entries.size());
        Assert.assertEquals("groupLabel", entries.get(0).getGroupLabel());
        Assert.assertEquals("alpha", entries.get(0).getEntryLabel());
        Assert.assertEquals(2, entries.get(0).getUrls().size());
        Assert.assertEquals("https://example.test/a1", entries.get(0).getUrls().get(0));
        Assert.assertEquals("https://example.test/a2", entries.get(0).getUrls().get(1));
        Assert.assertEquals("beta", entries.get(1).getEntryLabel());
        Assert.assertEquals(1, entries.get(1).getUrls().size());
    }

    @Test(expected = JSONException.class)
    public void parseIndexRejectsMissingGroups() throws JSONException {
        parser.parseIndex("{}");
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
}
