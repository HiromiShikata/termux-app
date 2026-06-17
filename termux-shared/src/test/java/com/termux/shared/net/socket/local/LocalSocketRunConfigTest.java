package com.termux.shared.net.socket.local;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.termux.shared.errors.Error;

import org.junit.Assert;
import org.junit.Test;

public class LocalSocketRunConfigTest {

    private static final class StubLocalSocketManagerClient implements ILocalSocketManager {
        @Nullable
        @Override
        public Thread.UncaughtExceptionHandler getLocalSocketManagerClientThreadUEH(
            @NonNull LocalSocketManager localSocketManager) {
            return null;
        }

        @Override
        public void onError(@NonNull LocalSocketManager localSocketManager,
                            @Nullable LocalClientSocket clientSocket, @NonNull Error error) {
        }

        @Override
        public void onDisallowedClientConnected(@NonNull LocalSocketManager localSocketManager,
                            @NonNull LocalClientSocket clientSocket, @NonNull Error error) {
        }

        @Override
        public void onClientAccepted(@NonNull LocalSocketManager localSocketManager,
                            @NonNull LocalClientSocket clientSocket) {
        }
    }

    private static LocalSocketRunConfig newFilesystemConfig() {
        return new LocalSocketRunConfig("server", "/data/local/tmp/socket", new StubLocalSocketManagerClient());
    }

    private static LocalSocketRunConfig newAbstractConfig() {
        return new LocalSocketRunConfig("abstract", "\0abstractsocket", new StubLocalSocketManagerClient());
    }

    @Test
    public void filesystemSocketIsNotAbstractAndKeepsAbsolutePath() {
        LocalSocketRunConfig config = newFilesystemConfig();
        Assert.assertEquals("server", config.getTitle());
        Assert.assertFalse(config.isAbstractNamespaceSocket());
        Assert.assertEquals("/data/local/tmp/socket", config.getPath());
    }

    @Test
    public void abstractSocketIsDetectedFromLeadingNullByte() {
        LocalSocketRunConfig config = newAbstractConfig();
        Assert.assertTrue(config.isAbstractNamespaceSocket());
        Assert.assertEquals("\0abstractsocket", config.getPath());
    }

    @Test
    public void logTitleIsPrefixedWithDefaultLogTag() {
        LocalSocketRunConfig config = newFilesystemConfig();
        Assert.assertTrue(config.getLogTitle().endsWith(".server"));
    }

    @Test
    public void localSocketManagerClientIsReturned() {
        ILocalSocketManager client = new StubLocalSocketManagerClient();
        LocalSocketRunConfig config = new LocalSocketRunConfig("t", "/tmp/s", client);
        Assert.assertSame(client, config.getLocalSocketManagerClient());
    }

    @Test
    public void fileDescriptorDefaultsToMinusOne() {
        Assert.assertEquals(Integer.valueOf(-1), newFilesystemConfig().getFD());
    }

    @Test
    public void setFileDescriptorAcceptsNonNegativeValue() {
        LocalSocketRunConfig config = newFilesystemConfig();
        config.setFD(7);
        Assert.assertEquals(Integer.valueOf(7), config.getFD());
    }

    @Test
    public void setFileDescriptorNormalizesNegativeValueToMinusOne() {
        LocalSocketRunConfig config = newFilesystemConfig();
        config.setFD(5);
        config.setFD(-9);
        Assert.assertEquals(Integer.valueOf(-1), config.getFD());
    }

    @Test
    public void receiveTimeoutDefaultsAndCanBeOverridden() {
        LocalSocketRunConfig config = newFilesystemConfig();
        Assert.assertEquals(Integer.valueOf(LocalSocketRunConfig.DEFAULT_RECEIVE_TIMEOUT),
            config.getReceiveTimeout());
        config.setReceiveTimeout(5000);
        Assert.assertEquals(Integer.valueOf(5000), config.getReceiveTimeout());
    }

    @Test
    public void sendTimeoutDefaultsAndCanBeOverridden() {
        LocalSocketRunConfig config = newFilesystemConfig();
        Assert.assertEquals(Integer.valueOf(LocalSocketRunConfig.DEFAULT_SEND_TIMEOUT),
            config.getSendTimeout());
        config.setSendTimeout(2500);
        Assert.assertEquals(Integer.valueOf(2500), config.getSendTimeout());
    }

    @Test
    public void deadlineDefaultsAndCanBeOverridden() {
        LocalSocketRunConfig config = newFilesystemConfig();
        Assert.assertEquals(Long.valueOf(LocalSocketRunConfig.DEFAULT_DEADLINE), config.getDeadline());
        config.setDeadline(12345L);
        Assert.assertEquals(Long.valueOf(12345L), config.getDeadline());
    }

    @Test
    public void backlogDefaultsAndAcceptsPositiveValue() {
        LocalSocketRunConfig config = newFilesystemConfig();
        Assert.assertEquals(Integer.valueOf(LocalSocketRunConfig.DEFAULT_BACKLOG), config.getBacklog());
        config.setBacklog(10);
        Assert.assertEquals(Integer.valueOf(10), config.getBacklog());
    }

    @Test
    public void backlogIgnoresNonPositiveValue() {
        LocalSocketRunConfig config = newFilesystemConfig();
        config.setBacklog(8);
        config.setBacklog(0);
        Assert.assertEquals(Integer.valueOf(8), config.getBacklog());
    }

    @Test
    public void logStringContainsConfiguredFields() {
        LocalSocketRunConfig config = newFilesystemConfig();
        config.setFD(3);
        String logString = config.getLogString();
        Assert.assertTrue(logString.contains("server Socket Server Run Config"));
        Assert.assertTrue(logString.contains("Path"));
        Assert.assertTrue(logString.contains("AbstractNamespaceSocket"));
        Assert.assertTrue(logString.contains("Backlog"));
    }

    @Test
    public void toStringMatchesLogString() {
        LocalSocketRunConfig config = newFilesystemConfig();
        Assert.assertEquals(config.getLogString(), config.toString());
    }

    @Test
    public void markdownStringContainsHeaderAndFields() {
        LocalSocketRunConfig config = newFilesystemConfig();
        String markdown = config.getMarkdownString();
        Assert.assertTrue(markdown.contains("## server Socket Server Run Config"));
        Assert.assertTrue(markdown.contains("ReceiveTimeout"));
        Assert.assertTrue(markdown.contains("SendTimeout"));
        Assert.assertTrue(markdown.contains("Deadline"));
    }

    @Test
    public void staticRunConfigLogStringReturnsNullLiteralForNull() {
        Assert.assertEquals("null", LocalSocketRunConfig.getRunConfigLogString(null));
    }

    @Test
    public void staticRunConfigLogStringDelegatesToInstance() {
        LocalSocketRunConfig config = newFilesystemConfig();
        Assert.assertEquals(config.getLogString(), LocalSocketRunConfig.getRunConfigLogString(config));
    }

    @Test
    public void staticRunConfigMarkdownStringReturnsNullLiteralForNull() {
        Assert.assertEquals("null", LocalSocketRunConfig.getRunConfigMarkdownString(null));
    }

    @Test
    public void staticRunConfigMarkdownStringDelegatesToInstance() {
        LocalSocketRunConfig config = newFilesystemConfig();
        Assert.assertEquals(config.getMarkdownString(),
            LocalSocketRunConfig.getRunConfigMarkdownString(config));
    }
}
