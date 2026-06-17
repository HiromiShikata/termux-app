package com.termux.shared.net.socket.local;

import org.junit.Assert;
import org.junit.Test;

public class PeerCredTest {

    private static PeerCred newPeerCred() {
        return new PeerCred();
    }

    @Test
    public void defaultIdentifiersAreInitializedToMinusOne() {
        PeerCred peerCred = newPeerCred();
        Assert.assertEquals(-1, peerCred.pid);
        Assert.assertEquals(-1, peerCred.uid);
        Assert.assertEquals(-1, peerCred.gid);
    }

    @Test
    public void processStringUsesPidOnlyWhenNameMissing() {
        PeerCred peerCred = newPeerCred();
        peerCred.pid = 100;
        Assert.assertEquals("100", peerCred.getProcessString());
    }

    @Test
    public void processStringUsesPidOnlyWhenNameEmpty() {
        PeerCred peerCred = newPeerCred();
        peerCred.pid = 200;
        peerCred.pname = "";
        Assert.assertEquals("200", peerCred.getProcessString());
    }

    @Test
    public void processStringIncludesNameWhenPresent() {
        PeerCred peerCred = newPeerCred();
        peerCred.pid = 300;
        peerCred.pname = "com.termux";
        Assert.assertEquals("300 (com.termux)", peerCred.getProcessString());
    }

    @Test
    public void userStringUsesUidOnlyWhenNameMissing() {
        PeerCred peerCred = newPeerCred();
        peerCred.uid = 1000;
        Assert.assertEquals("1000", peerCred.getUserString());
    }

    @Test
    public void userStringIncludesNameWhenPresent() {
        PeerCred peerCred = newPeerCred();
        peerCred.uid = 1000;
        peerCred.uname = "u0_a100";
        Assert.assertEquals("1000 (u0_a100)", peerCred.getUserString());
    }

    @Test
    public void groupStringUsesGidOnlyWhenNameMissing() {
        PeerCred peerCred = newPeerCred();
        peerCred.gid = 9997;
        Assert.assertEquals("9997", peerCred.getGroupString());
    }

    @Test
    public void groupStringIncludesNameWhenPresent() {
        PeerCred peerCred = newPeerCred();
        peerCred.gid = 9997;
        peerCred.gname = "everybody";
        Assert.assertEquals("9997 (everybody)", peerCred.getGroupString());
    }

    @Test
    public void minimalStringJoinsProcessUserAndGroup() {
        PeerCred peerCred = newPeerCred();
        peerCred.pid = 5;
        peerCred.pname = "app";
        peerCred.uid = 10;
        peerCred.uname = "user";
        peerCred.gid = 20;
        peerCred.gname = "group";
        Assert.assertEquals("process=5 (app), user=10 (user), group=20 (group)",
            peerCred.getMinimalString());
    }

    @Test
    public void logStringContainsAllSectionsWithoutCmdlineWhenNull() {
        PeerCred peerCred = newPeerCred();
        peerCred.pid = 1;
        peerCred.uid = 2;
        peerCred.gid = 3;
        String logString = peerCred.getLogString();
        Assert.assertTrue(logString.contains("Peer Cred:"));
        Assert.assertTrue(logString.contains("Process"));
        Assert.assertTrue(logString.contains("User"));
        Assert.assertTrue(logString.contains("Group"));
        Assert.assertFalse(logString.contains("Cmdline"));
    }

    @Test
    public void logStringIncludesCmdlineWhenPresent() {
        PeerCred peerCred = newPeerCred();
        peerCred.cmdline = "/system/bin/app --flag";
        Assert.assertTrue(peerCred.getLogString().contains("Cmdline"));
    }

    @Test
    public void markdownStringContainsAllSectionsWithoutCmdlineWhenNull() {
        PeerCred peerCred = newPeerCred();
        String markdown = peerCred.getMarkdownString();
        Assert.assertTrue(markdown.contains("## Peer Cred"));
        Assert.assertTrue(markdown.contains("Process"));
        Assert.assertFalse(markdown.contains("Cmdline"));
    }

    @Test
    public void markdownStringIncludesCmdlineWhenPresent() {
        PeerCred peerCred = newPeerCred();
        peerCred.cmdline = "/system/bin/app";
        Assert.assertTrue(peerCred.getMarkdownString().contains("Cmdline"));
    }

    @Test
    public void staticPeerCredLogStringReturnsNullLiteralForNull() {
        Assert.assertEquals("null", PeerCred.getPeerCredLogString(null));
    }

    @Test
    public void staticPeerCredLogStringDelegatesToInstance() {
        PeerCred peerCred = newPeerCred();
        Assert.assertEquals(peerCred.getLogString(), PeerCred.getPeerCredLogString(peerCred));
    }

    @Test
    public void staticPeerCredMarkdownStringReturnsNullLiteralForNull() {
        Assert.assertEquals("null", PeerCred.getPeerCredMarkdownString(null));
    }

    @Test
    public void staticPeerCredMarkdownStringDelegatesToInstance() {
        PeerCred peerCred = newPeerCred();
        Assert.assertEquals(peerCred.getMarkdownString(), PeerCred.getPeerCredMarkdownString(peerCred));
    }
}
