package com.termux.app.apkupdate;

import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class ApkUpdatePendingStateTest {

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
    public void savesAndLoadsAvailableUpdate() {
        ApkUpdatePendingState pendingState = new ApkUpdatePendingState(new InMemoryStore());

        pendingState.save(ApkUpdateAvailability.available(
            "0.121.0", "https://example.com/arm64", "termux-app_arm64-v8a.apk"));

        Assert.assertTrue(pendingState.hasPending());
        ApkUpdateAvailability loaded = pendingState.load();
        Assert.assertNotNull(loaded);
        Assert.assertTrue(loaded.isUpdateAvailable());
        Assert.assertEquals("0.121.0", loaded.getLatestVersionName());
        Assert.assertEquals("https://example.com/arm64", loaded.getDownloadUrl());
        Assert.assertEquals("termux-app_arm64-v8a.apk", loaded.getAssetName());
    }

    @Test
    public void loadReturnsNullWhenNothingSaved() {
        ApkUpdatePendingState pendingState = new ApkUpdatePendingState(new InMemoryStore());

        Assert.assertNull(pendingState.load());
        Assert.assertFalse(pendingState.hasPending());
    }

    @Test
    public void savingUpToDateClearsAnyPendingUpdate() {
        ApkUpdatePendingState pendingState = new ApkUpdatePendingState(new InMemoryStore());
        pendingState.save(ApkUpdateAvailability.available(
            "0.121.0", "https://example.com/arm64", "termux-app_arm64-v8a.apk"));

        pendingState.save(ApkUpdateAvailability.upToDate("0.121.0"));

        Assert.assertNull(pendingState.load());
        Assert.assertFalse(pendingState.hasPending());
    }

    @Test
    public void clearRemovesPendingUpdate() {
        ApkUpdatePendingState pendingState = new ApkUpdatePendingState(new InMemoryStore());
        pendingState.save(ApkUpdateAvailability.available(
            "0.121.0", "https://example.com/arm64", "termux-app_arm64-v8a.apk"));

        pendingState.clear();

        Assert.assertNull(pendingState.load());
        Assert.assertFalse(pendingState.hasPending());
    }

    @Test
    public void loadReturnsNullWhenPersistedDataIsPartial() {
        InMemoryStore store = new InMemoryStore();
        store.putString(ApkUpdatePendingState.KEY_LATEST_VERSION_NAME, "0.121.0");
        ApkUpdatePendingState pendingState = new ApkUpdatePendingState(store);

        Assert.assertNull(pendingState.load());
        Assert.assertFalse(pendingState.hasPending());
    }

    @Test
    public void loadIfNewerThanInstalledReturnsAvailableWhenInstalledIsOlder() {
        ApkUpdatePendingState pendingState = new ApkUpdatePendingState(new InMemoryStore());
        pendingState.save(ApkUpdateAvailability.available(
            "0.118.2536", "https://example.com/arm64", "termux-app_arm64-v8a.apk"));

        ApkUpdateAvailability loaded = pendingState.loadIfNewerThanInstalled("0.118.2535");

        Assert.assertNotNull(loaded);
        Assert.assertTrue(loaded.isUpdateAvailable());
        Assert.assertEquals("0.118.2536", loaded.getLatestVersionName());
    }

    @Test
    public void loadIfNewerThanInstalledReturnsNullAndClearsWhenInstalledEqualsLatest() {
        ApkUpdatePendingState pendingState = new ApkUpdatePendingState(new InMemoryStore());
        pendingState.save(ApkUpdateAvailability.available(
            "0.118.2535", "https://example.com/arm64", "termux-app_arm64-v8a.apk"));

        Assert.assertNull(pendingState.loadIfNewerThanInstalled("0.118.2535"));
        Assert.assertFalse(pendingState.hasPending());
    }

    @Test
    public void loadIfNewerThanInstalledReturnsNullAndClearsWhenInstalledIsNewer() {
        ApkUpdatePendingState pendingState = new ApkUpdatePendingState(new InMemoryStore());
        pendingState.save(ApkUpdateAvailability.available(
            "0.118.2534", "https://example.com/arm64", "termux-app_arm64-v8a.apk"));

        Assert.assertNull(pendingState.loadIfNewerThanInstalled("0.118.2535"));
        Assert.assertFalse(pendingState.hasPending());
    }

    @Test
    public void loadIfNewerThanInstalledTreatsLeadingVTagAsEqualToInstalledVersionName() {
        InMemoryStore store = new InMemoryStore();
        store.putString(ApkUpdatePendingState.KEY_LATEST_VERSION_NAME, "v0.118.2535");
        store.putString(ApkUpdatePendingState.KEY_DOWNLOAD_URL, "https://example.com/arm64");
        store.putString(ApkUpdatePendingState.KEY_ASSET_NAME, "termux-app_arm64-v8a.apk");
        ApkUpdatePendingState pendingState = new ApkUpdatePendingState(store);

        Assert.assertNull(pendingState.loadIfNewerThanInstalled("0.118.2535"));
        Assert.assertFalse(pendingState.hasPending());
    }

    @Test
    public void loadIfNewerThanInstalledReturnsNullWhenNothingSaved() {
        ApkUpdatePendingState pendingState = new ApkUpdatePendingState(new InMemoryStore());

        Assert.assertNull(pendingState.loadIfNewerThanInstalled("0.118.2535"));
    }
}
