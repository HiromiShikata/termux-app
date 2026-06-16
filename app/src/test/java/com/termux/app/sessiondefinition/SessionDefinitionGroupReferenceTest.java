package com.termux.app.sessiondefinition;

import org.junit.Assert;
import org.junit.Test;

public class SessionDefinitionGroupReferenceTest {

    @Test
    public void exposesLabelAndUrl() {
        SessionDefinitionGroupReference reference =
            new SessionDefinitionGroupReference("Project A", "https://example.com/a.json");

        Assert.assertEquals("Project A", reference.getLabel());
        Assert.assertEquals("https://example.com/a.json", reference.getUrl());
    }

    @Test
    public void retainsNullValues() {
        SessionDefinitionGroupReference reference = new SessionDefinitionGroupReference(null, null);

        Assert.assertNull(reference.getLabel());
        Assert.assertNull(reference.getUrl());
    }

    @Test
    public void retainsEmptyValues() {
        SessionDefinitionGroupReference reference = new SessionDefinitionGroupReference("", "");

        Assert.assertEquals("", reference.getLabel());
        Assert.assertEquals("", reference.getUrl());
    }
}
