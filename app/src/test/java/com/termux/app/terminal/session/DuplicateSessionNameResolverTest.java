package com.termux.app.terminal.session;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DuplicateSessionNameResolverTest {

    @Test
    public void revealsExistingSessionWhenSameNamedSessionAlreadyExists() {
        List<String> liveSessionNames = Arrays.asList("deploy", "build");

        DuplicateSessionNameResolution resolution = DuplicateSessionNameResolver.resolve(
            "deploy", liveSessionNames, Collections.emptySet());

        Assert.assertTrue(resolution.shouldRevealExisting());
        Assert.assertEquals("deploy", resolution.getExistingSessionName());
        Assert.assertFalse(resolution.requiresUnhide());
    }

    @Test
    public void createsNewSessionWhenNoSessionSharesTheCandidateName() {
        List<String> liveSessionNames = Arrays.asList("deploy", "build");

        DuplicateSessionNameResolution resolution = DuplicateSessionNameResolver.resolve(
            "release", liveSessionNames, Collections.emptySet());

        Assert.assertFalse(resolution.shouldRevealExisting());
        Assert.assertNull(resolution.getExistingSessionName());
        Assert.assertFalse(resolution.requiresUnhide());
    }

    @Test
    public void revealsAndUnhidesExistingSessionWhenSameNamedSessionIsHidden() {
        List<String> liveSessionNames = Arrays.asList("build", "deploy");
        Set<String> hiddenSessionNames = new HashSet<>(Collections.singletonList("deploy"));

        DuplicateSessionNameResolution resolution = DuplicateSessionNameResolver.resolve(
            "deploy", liveSessionNames, hiddenSessionNames);

        Assert.assertTrue(resolution.shouldRevealExisting());
        Assert.assertEquals("deploy", resolution.getExistingSessionName());
        Assert.assertTrue(resolution.requiresUnhide());
    }

    @Test
    public void createsNewSessionForNullCandidateName() {
        List<String> liveSessionNames = Collections.singletonList("deploy");

        DuplicateSessionNameResolution resolution = DuplicateSessionNameResolver.resolve(
            null, liveSessionNames, Collections.emptySet());

        Assert.assertFalse(resolution.shouldRevealExisting());
    }

    @Test
    public void createsNewSessionForEmptyCandidateName() {
        List<String> liveSessionNames = Collections.singletonList("deploy");

        DuplicateSessionNameResolution resolution = DuplicateSessionNameResolver.resolve(
            "", liveSessionNames, Collections.emptySet());

        Assert.assertFalse(resolution.shouldRevealExisting());
    }

    @Test
    public void matchesExactNameAndCreatesNewSessionForSimilarButDifferentNames() {
        List<String> liveSessionNames = Arrays.asList("deploy-2", "Deploy");

        DuplicateSessionNameResolution resolution = DuplicateSessionNameResolver.resolve(
            "deploy", liveSessionNames, Collections.emptySet());

        Assert.assertFalse(resolution.shouldRevealExisting());
    }
}
