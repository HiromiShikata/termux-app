package com.termux.shared.shell.am;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.termux.shared.errors.Error;
import com.termux.shared.net.socket.local.ILocalSocketManager;
import com.termux.shared.net.socket.local.LocalClientSocket;
import com.termux.shared.net.socket.local.LocalSocketManager;

import org.junit.Assert;
import org.junit.Test;

public class AmSocketServerRunConfigTest {

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

    private static AmSocketServerRunConfig newConfig() {
        return new AmSocketServerRunConfig("am", "/data/local/tmp/am-socket", new StubLocalSocketManagerClient());
    }

    @Test
    public void inheritsTitleAndPathFromLocalSocketRunConfig() {
        AmSocketServerRunConfig config = newConfig();
        Assert.assertEquals("am", config.getTitle());
        Assert.assertEquals("/data/local/tmp/am-socket", config.getPath());
    }

    @Test
    public void shouldCheckDisplayOverAppsPermissionDefaultsToTrueWhenUnset() {
        Assert.assertTrue(AmSocketServerRunConfig.DEFAULT_CHECK_DISPLAY_OVER_APPS_PERMISSION);
        Assert.assertTrue(newConfig().shouldCheckDisplayOverAppsPermission());
    }

    @Test
    public void setCheckDisplayOverAppsPermissionFalseOverridesDefault() {
        AmSocketServerRunConfig config = newConfig();
        config.setCheckDisplayOverAppsPermission(false);
        Assert.assertFalse(config.shouldCheckDisplayOverAppsPermission());
    }

    @Test
    public void setCheckDisplayOverAppsPermissionTrueIsHonoured() {
        AmSocketServerRunConfig config = newConfig();
        config.setCheckDisplayOverAppsPermission(false);
        config.setCheckDisplayOverAppsPermission(true);
        Assert.assertTrue(config.shouldCheckDisplayOverAppsPermission());
    }

    @Test
    public void setCheckDisplayOverAppsPermissionNullFallsBackToDefault() {
        AmSocketServerRunConfig config = newConfig();
        config.setCheckDisplayOverAppsPermission(false);
        config.setCheckDisplayOverAppsPermission(null);
        Assert.assertTrue(config.shouldCheckDisplayOverAppsPermission());
    }

    @Test
    public void logStringAppendsAmCommandSectionToParentLog() {
        AmSocketServerRunConfig config = newConfig();
        String logString = config.getLogString();
        Assert.assertTrue(logString.contains("am Socket Server Run Config"));
        Assert.assertTrue(logString.contains("Am Command:"));
        Assert.assertTrue(logString.contains("CheckDisplayOverAppsPermission"));
    }

    @Test
    public void logStringReflectsOverriddenPermissionFlag() {
        AmSocketServerRunConfig config = newConfig();
        config.setCheckDisplayOverAppsPermission(false);
        Assert.assertTrue(config.getLogString().contains("false"));
    }

    @Test
    public void markdownStringAppendsAmCommandHeaderToParentMarkdown() {
        AmSocketServerRunConfig config = newConfig();
        String markdown = config.getMarkdownString();
        Assert.assertTrue(markdown.contains("## am Socket Server Run Config"));
        Assert.assertTrue(markdown.contains("## Am Command"));
        Assert.assertTrue(markdown.contains("CheckDisplayOverAppsPermission"));
    }

    @Test
    public void toStringEqualsLogString() {
        AmSocketServerRunConfig config = newConfig();
        Assert.assertEquals(config.getLogString(), config.toString());
    }

    @Test
    public void staticRunConfigLogStringReturnsNullLiteralForNull() {
        Assert.assertEquals("null", AmSocketServerRunConfig.getRunConfigLogString(null));
    }

    @Test
    public void staticRunConfigLogStringDelegatesToInstance() {
        AmSocketServerRunConfig config = newConfig();
        Assert.assertEquals(config.getLogString(), AmSocketServerRunConfig.getRunConfigLogString(config));
    }

    @Test
    public void staticRunConfigMarkdownStringReturnsNullLiteralForNull() {
        Assert.assertEquals("null", AmSocketServerRunConfig.getRunConfigMarkdownString(null));
    }

    @Test
    public void staticRunConfigMarkdownStringDelegatesToInstance() {
        AmSocketServerRunConfig config = newConfig();
        Assert.assertEquals(config.getMarkdownString(), AmSocketServerRunConfig.getRunConfigMarkdownString(config));
    }
}
