package com.termux.app.sessiondefinition;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class SessionDefinitionLoadResultTest {

    @Test
    public void reportsCountsAndFailureState() {
        SessionDefinitionLoadResult result = new SessionDefinitionLoadResult(
            Collections.emptyList(), 3, Arrays.asList("groupA", "groupB"));

        Assert.assertEquals(3, result.getTotalGroupCount());
        Assert.assertEquals(2, result.getFailedGroupCount());
        Assert.assertTrue(result.hasFailedGroups());
        Assert.assertEquals(Arrays.asList("groupA", "groupB"), result.getFailedGroupLabels());
    }

    @Test
    public void hasNoFailedGroupsWhenFailedListIsEmpty() {
        SessionDefinitionLoadResult result = new SessionDefinitionLoadResult(
            Collections.emptyList(), 2, Collections.emptyList());

        Assert.assertFalse(result.hasFailedGroups());
        Assert.assertEquals(0, result.getFailedGroupCount());
    }

    @Test
    public void copiesFailedGroupLabelsDefensively() {
        List<String> failedGroupLabels = new ArrayList<>();
        failedGroupLabels.add("groupA");
        SessionDefinitionLoadResult result =
            new SessionDefinitionLoadResult(Collections.emptyList(), 1, failedGroupLabels);

        failedGroupLabels.add("groupB");

        Assert.assertEquals(1, result.getFailedGroupCount());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void failedGroupLabelsAreUnmodifiable() {
        SessionDefinitionLoadResult result = new SessionDefinitionLoadResult(
            Collections.emptyList(), 1, Collections.singletonList("groupA"));

        result.getFailedGroupLabels().add("groupB");
    }
}
