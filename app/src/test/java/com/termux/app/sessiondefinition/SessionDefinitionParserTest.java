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
