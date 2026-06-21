package com.termux.app.browser;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BrowserPerTabHistoryIsolationScenarioTest {

    private static final String TAB_SESSION_A = "tab-session-a";
    private static final String TAB_SESSION_B = "tab-session-b";

    private static final class SavedHistory {
        final List<String> entries;
        final int currentIndex;

        SavedHistory(List<String> entries, int currentIndex) {
            this.entries = new ArrayList<>(entries);
            this.currentIndex = currentIndex;
        }
    }

    private static final class SharedWebViewModel {
        final List<String> backForwardEntries = new ArrayList<>();
        int currentIndex = -1;

        void loadUrl(String url) {
            while (backForwardEntries.size() > currentIndex + 1) {
                backForwardEntries.remove(backForwardEntries.size() - 1);
            }
            backForwardEntries.add(url);
            currentIndex = backForwardEntries.size() - 1;
        }

        SavedHistory saveState() {
            return new SavedHistory(backForwardEntries, currentIndex);
        }

        void restoreState(SavedHistory savedHistory) {
            backForwardEntries.clear();
            backForwardEntries.addAll(savedHistory.entries);
            currentIndex = savedHistory.currentIndex;
        }

        boolean canGoBack() {
            return currentIndex > 0;
        }

        void goBack() {
            if (canGoBack()) currentIndex--;
        }

        String currentUrl() {
            return currentIndex < 0 ? null : backForwardEntries.get(currentIndex);
        }
    }

    private static final class BrowserModel {
        final SharedWebViewModel webView = new SharedWebViewModel();
        final Map<String, SavedHistory> savedStateByTab = new HashMap<>();
        String displayedTabId;

        void displayTab(String tabId) {
            if (tabId.equals(displayedTabId)) return;
            if (displayedTabId != null) {
                savedStateByTab.put(displayedTabId, webView.saveState());
            }
            displayedTabId = tabId;
            SavedHistory savedHistory = savedStateByTab.get(tabId);
            BrowserTabStateRestoration restoration =
                BrowserTabStateRestoration.resolve(savedHistory != null, false);
            if (restoration.shouldRestoreState()) {
                webView.restoreState(savedHistory);
            }
        }

        void navigateDisplayedTabTo(String url) {
            webView.loadUrl(url);
        }

        boolean pressBackWithinTab() {
            if (webView.canGoBack()) {
                webView.goBack();
                return true;
            }
            return false;
        }
    }

    @Test
    public void switchingAwayCapturesTheSourceTabsHistoryIntoTheSourceTab() {
        BrowserModel browser = new BrowserModel();
        String tabA = TAB_SESSION_A + "-1";
        String tabB = TAB_SESSION_B + "-1";

        browser.displayTab(tabA);
        browser.navigateDisplayedTabTo("https://a.example/home");
        browser.navigateDisplayedTabTo("https://a.example/page");

        browser.displayTab(tabB);

        SavedHistory capturedA = browser.savedStateByTab.get(tabA);
        Assert.assertNotNull(capturedA);
        Assert.assertEquals(2, capturedA.entries.size());
        Assert.assertEquals("https://a.example/page", capturedA.entries.get(capturedA.currentIndex));
    }

    @Test
    public void switchingToATabRestoresThatTabsOwnHistory() {
        BrowserModel browser = new BrowserModel();
        String tabA = TAB_SESSION_A + "-1";
        String tabB = TAB_SESSION_B + "-1";

        browser.displayTab(tabA);
        browser.navigateDisplayedTabTo("https://a.example/home");
        browser.navigateDisplayedTabTo("https://a.example/page");

        browser.displayTab(tabB);
        browser.navigateDisplayedTabTo("https://b.example/home");
        browser.navigateDisplayedTabTo("https://b.example/page");

        browser.displayTab(tabA);

        Assert.assertEquals("https://a.example/page", browser.webView.currentUrl());
    }

    @Test
    public void pressingBackWalksOnlyTheCurrentTabsHistoryAndNeverAnotherSessionsPages() {
        BrowserModel browser = new BrowserModel();
        String tabA = TAB_SESSION_A + "-1";
        String tabB = TAB_SESSION_B + "-1";

        browser.displayTab(tabA);
        browser.navigateDisplayedTabTo("https://a.example/home");
        browser.navigateDisplayedTabTo("https://a.example/page");

        browser.displayTab(tabB);
        browser.navigateDisplayedTabTo("https://b.example/home");
        browser.navigateDisplayedTabTo("https://b.example/page");

        browser.displayTab(tabA);

        Assert.assertTrue(browser.pressBackWithinTab());
        Assert.assertEquals("https://a.example/home", browser.webView.currentUrl());

        Assert.assertFalse(browser.pressBackWithinTab());
        Assert.assertEquals("https://a.example/home", browser.webView.currentUrl());
    }

    @Test
    public void aFreshTabWithoutSavedStateLoadsItsUrlInsteadOfRestoring() {
        BrowserModel browser = new BrowserModel();
        String tabA = TAB_SESSION_A + "-1";

        browser.displayTab(tabA);
        Assert.assertNull(browser.savedStateByTab.get(tabA));

        browser.navigateDisplayedTabTo("https://a.example/home");
        Assert.assertEquals("https://a.example/home", browser.webView.currentUrl());
        Assert.assertFalse(browser.webView.canGoBack());
    }
}
