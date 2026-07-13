package com.termux.app.apkupdate;

import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class ApkUpdateInstallResumeRequestTest {

    private static final class InMemoryStore implements ApkUpdatePendingState.Store {

        final Map<String, String> values = new HashMap<>();

        @Override
        public String getString(String key) {
            return values.get(key);
        }

        @Override
        public void putString(String key, String value) {
            values.put(key, value);
        }

        @Override
        public void remove(String key) {
            values.remove(key);
        }
    }

    @Test
    public void savesAndLoadsResumeRequest() {
        ApkUpdateInstallResumeRequest resumeRequest =
            new ApkUpdateInstallResumeRequest(new InMemoryStore());

        resumeRequest.saveResumeRequest(ApkUpdateAvailability.available(
            "0.121.0", "https://example.com/arm64", "termux-app_arm64-v8a.apk", 9876543L));

        Assert.assertTrue(resumeRequest.hasResumeRequest());
        ApkUpdateAvailability loaded = resumeRequest.loadResumeRequest();
        Assert.assertNotNull(loaded);
        Assert.assertEquals("0.121.0", loaded.getLatestVersionName());
        Assert.assertEquals("https://example.com/arm64", loaded.getDownloadUrl());
        Assert.assertEquals("termux-app_arm64-v8a.apk", loaded.getAssetName());
        Assert.assertEquals(9876543L, loaded.getExpectedSizeBytes());
    }

    @Test
    public void savesAndLoadsDownloadedFilePath() {
        ApkUpdateInstallResumeRequest resumeRequest =
            new ApkUpdateInstallResumeRequest(new InMemoryStore());

        resumeRequest.saveResumeRequest(ApkUpdateAvailability.available(
                "0.121.0", "https://example.com/arm64", "termux-app_arm64-v8a.apk")
            .withDownloadedFilePath("/data/cache/apkupdate/termux-app_arm64-v8a.apk"));

        ApkUpdateAvailability loaded = resumeRequest.loadResumeRequest();
        Assert.assertNotNull(loaded);
        Assert.assertTrue(loaded.hasDownloadedFilePath());
        Assert.assertEquals("/data/cache/apkupdate/termux-app_arm64-v8a.apk",
            loaded.getDownloadedFilePath());
    }

    @Test
    public void permissionDeniedPathPersistsResumeRequestSoReturnLeadsToInstall() {
        ApkUpdateInstallResumeRequest resumeRequest =
            new ApkUpdateInstallResumeRequest(new InMemoryStore());

        resumeRequest.saveResumeRequest(ApkUpdateAvailability.available(
            "0.121.0", "https://example.com/arm64", "termux-app_arm64-v8a.apk"));

        ApkUpdateAvailability resumed = resumeRequest.loadResumeRequest();
        Assert.assertNotNull(resumed);
        Assert.assertTrue(resumed.isUpdateAvailable());
        Assert.assertEquals("0.121.0", resumed.getLatestVersionName());
    }

    @Test
    public void clearResumeRequestRemovesPersistedRequest() {
        ApkUpdateInstallResumeRequest resumeRequest =
            new ApkUpdateInstallResumeRequest(new InMemoryStore());
        resumeRequest.saveResumeRequest(ApkUpdateAvailability.available(
            "0.121.0", "https://example.com/arm64", "termux-app_arm64-v8a.apk"));

        resumeRequest.clearResumeRequest();

        Assert.assertFalse(resumeRequest.hasResumeRequest());
        Assert.assertNull(resumeRequest.loadResumeRequest());
    }

    @Test
    public void loadReturnsNullWhenNothingSaved() {
        ApkUpdateInstallResumeRequest resumeRequest =
            new ApkUpdateInstallResumeRequest(new InMemoryStore());

        Assert.assertNull(resumeRequest.loadResumeRequest());
        Assert.assertFalse(resumeRequest.hasResumeRequest());
    }

    @Test
    public void savingUpToDateClearsAnyResumeRequest() {
        ApkUpdateInstallResumeRequest resumeRequest =
            new ApkUpdateInstallResumeRequest(new InMemoryStore());
        resumeRequest.saveResumeRequest(ApkUpdateAvailability.available(
            "0.121.0", "https://example.com/arm64", "termux-app_arm64-v8a.apk"));

        resumeRequest.saveResumeRequest(ApkUpdateAvailability.upToDate("0.121.0"));

        Assert.assertFalse(resumeRequest.hasResumeRequest());
    }

    @Test
    public void loadReturnsNullWhenPersistedDataIsPartial() {
        InMemoryStore store = new InMemoryStore();
        store.putString(ApkUpdateInstallResumeRequest.KEY_VERSION_NAME, "0.121.0");
        ApkUpdateInstallResumeRequest resumeRequest = new ApkUpdateInstallResumeRequest(store);

        Assert.assertNull(resumeRequest.loadResumeRequest());
    }
}
