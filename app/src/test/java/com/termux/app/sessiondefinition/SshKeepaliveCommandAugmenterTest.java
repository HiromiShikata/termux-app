package com.termux.app.sessiondefinition;

import org.junit.Assert;
import org.junit.Test;

public class SshKeepaliveCommandAugmenterTest {

    private final SshKeepaliveCommandAugmenter augmenter = new SshKeepaliveCommandAugmenter();

    @Test
    public void addsKeepaliveOptionsToPlainSshCommand() {
        Assert.assertEquals(
            "ssh -o ServerAliveInterval=30 -o ServerAliveCountMax=3 -o TCPKeepAlive=yes 'host'",
            augmenter.augment("ssh 'host'"));
    }

    @Test
    public void addsKeepaliveOptionsAndGatetimeToSshInsideAutosshCommand() {
        Assert.assertEquals(
            "AUTOSSH_GATETIME=0 autossh -M 0 ssh -o ServerAliveInterval=30 -o ServerAliveCountMax=3 -o TCPKeepAlive=yes 'host'",
            augmenter.augment("autossh -M 0 ssh 'host'"));
    }

    @Test
    public void doesNotDuplicateKeepaliveWhenTemplateAlreadySpecifiesServerAliveInterval() {
        String command = "ssh -o ServerAliveInterval=30 'host'";
        Assert.assertEquals(command, augmenter.augment(command));
    }

    @Test
    public void doesNotDuplicateGatetimeWhenTemplateAlreadySpecifiesAutosshGatetime() {
        String command = "AUTOSSH_GATETIME=5 autossh -M 0 ssh -o ServerAliveInterval=30 'host'";
        Assert.assertEquals(command, augmenter.augment(command));
    }

    @Test
    public void isIdempotentWhenAppliedTwice() {
        String once = augmenter.augment("autossh -M 0 ssh 'host'");
        Assert.assertEquals(once, augmenter.augment(once));
    }

    @Test
    public void isIdempotentForPlainSshWhenAppliedTwice() {
        String once = augmenter.augment("ssh 'host'");
        Assert.assertEquals(once, augmenter.augment(once));
    }

    @Test
    public void leavesCommandUnchangedWhenItDoesNotInvokeSsh() {
        Assert.assertEquals("connect 'host'", augmenter.augment("connect 'host'"));
    }

    @Test
    public void doesNotAddGatetimeToPlainSshCommand() {
        Assert.assertEquals(
            "ssh -o ServerAliveInterval=30 -o ServerAliveCountMax=3 -o TCPKeepAlive=yes 'host'",
            augmenter.augment("ssh 'host'"));
    }

    @Test
    public void doesNotMatchSshSubstringInsideAnotherToken() {
        Assert.assertEquals("myssh 'host'", augmenter.augment("myssh 'host'"));
        Assert.assertEquals("sshpass 'host'", augmenter.augment("sshpass 'host'"));
    }

    @Test
    public void doesNotMatchAutosshSubstringInsideAnotherToken() {
        Assert.assertEquals("myautossh 'host'", augmenter.augment("myautossh 'host'"));
    }

    @Test
    public void preservesTokensFollowingTheSshInvocation() {
        Assert.assertEquals(
            "ssh -o ServerAliveInterval=30 -o ServerAliveCountMax=3 -o TCPKeepAlive=yes -p 2222 'host'",
            augmenter.augment("ssh -p 2222 'host'"));
    }

    @Test
    public void returnsNullForNullCommand() {
        Assert.assertNull(augmenter.augment(null));
    }

    @Test
    public void augmentsSshAndGatetimeForAutosshWithLeadingFlags() {
        Assert.assertEquals(
            "AUTOSSH_GATETIME=0 autossh -f -M 0 ssh -o ServerAliveInterval=30 -o ServerAliveCountMax=3 -o TCPKeepAlive=yes -N 'host'",
            augmenter.augment("autossh -f -M 0 ssh -N 'host'"));
    }
}
