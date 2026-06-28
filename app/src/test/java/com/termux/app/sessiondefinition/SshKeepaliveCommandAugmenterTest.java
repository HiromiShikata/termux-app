package com.termux.app.sessiondefinition;

import org.junit.Assert;
import org.junit.Test;

public class SshKeepaliveCommandAugmenterTest {

    private final SshKeepaliveCommandAugmenter augmenter = new SshKeepaliveCommandAugmenter();

    @Test
    public void addsKeepaliveOptionsToPlainSshCommand() {
        Assert.assertEquals(
            "ssh -o ServerAliveInterval=60 -o ServerAliveCountMax=3 'host'",
            augmenter.augment("ssh 'host'"));
    }

    @Test
    public void addsKeepaliveOptionsToSshInsideAutosshCommand() {
        Assert.assertEquals(
            "autossh -M 0 ssh -o ServerAliveInterval=60 -o ServerAliveCountMax=3 'host'",
            augmenter.augment("autossh -M 0 ssh 'host'"));
    }

    @Test
    public void doesNotDuplicateKeepaliveWhenTemplateAlreadySpecifiesServerAliveInterval() {
        String command = "ssh -o ServerAliveInterval=30 'host'";
        Assert.assertEquals(command, augmenter.augment(command));
    }

    @Test
    public void leavesCommandUnchangedWhenItDoesNotInvokeSsh() {
        Assert.assertEquals("connect 'host'", augmenter.augment("connect 'host'"));
    }

    @Test
    public void doesNotMatchSshSubstringInsideAnotherToken() {
        Assert.assertEquals("myssh 'host'", augmenter.augment("myssh 'host'"));
        Assert.assertEquals("sshpass 'host'", augmenter.augment("sshpass 'host'"));
    }

    @Test
    public void preservesTokensFollowingTheSshInvocation() {
        Assert.assertEquals(
            "ssh -o ServerAliveInterval=60 -o ServerAliveCountMax=3 -p 2222 'host'",
            augmenter.augment("ssh -p 2222 'host'"));
    }

    @Test
    public void returnsNullForNullCommand() {
        Assert.assertNull(augmenter.augment(null));
    }

    @Test
    public void augmentsSshAppearingAtStartWithLeadingWhitespacePreservedAfterAutossh() {
        Assert.assertEquals(
            "autossh -f -M 0 ssh -o ServerAliveInterval=60 -o ServerAliveCountMax=3 -N 'host'",
            augmenter.augment("autossh -f -M 0 ssh -N 'host'"));
    }
}
