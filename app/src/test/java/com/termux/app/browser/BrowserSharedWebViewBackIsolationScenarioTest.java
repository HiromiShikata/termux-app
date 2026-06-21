package com.termux.app.browser;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class BrowserSharedWebViewBackIsolationScenarioTest {

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

        void clearHistory() {
            String current = currentUrl();
            backForwardEntries.clear();
            if (current != null) {
                backForwardEntries.add(current);
                currentIndex = 0;
            } else {
                currentIndex = -1;
            }
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
        String displayedTabId;

        void displayTab(String tabId, String tabUrl) {
            boolean displayedTabChanged = !tabId.equals(displayedTabId);
            if (!displayedTabChanged) return;
            displayedTabId = tabId;
            webView.loadUrl(tabUrl);
            BrowserHistoryIsolation isolation =
                BrowserHistoryIsolation.resolve(displayedTabChanged);
            if (isolation.shouldClearHistory()) {
                webView.clearHistory();
            }
        }

        void navigateDisplayedTabTo(String url) {
            webView.loadUrl(url);
        }

        boolean pressBack() {
            if (webView.canGoBack()) {
                webView.goBack();
                return true;
            }
            return false;
        }
    }

    @Test
    public void switchingToAnotherSessionsTabShowsThatTabsOwnUrlNotTheSourceTabsPage() {
        BrowserModel browser = new BrowserModel();

        browser.displayTab("tab-a", "https://a.example/home");
        browser.navigateDisplayedTabTo("https://a.example/page");

        browser.displayTab("tab-b", "https://b.example/home");

        Assert.assertEquals("https://b.example/home", browser.webView.currentUrl());
    }

    @Test
    public void pressingBackAfterSwitchingSessionsNeverReachesAnotherSessionsPage() {
        BrowserModel browser = new BrowserModel();

        browser.displayTab("tab-a", "https://a.example/home");
        browser.navigateDisplayedTabTo("https://a.example/page");

        browser.displayTab("tab-b", "https://b.example/home");

        Assert.assertFalse(browser.pressBack());
        Assert.assertEquals("https://b.example/home", browser.webView.currentUrl());
    }

    @Test
    public void withinSessionBackHistoryAccumulatedAfterTheSwitchSurvives() {
        BrowserModel browser = new BrowserModel();

        browser.displayTab("tab-a", "https://a.example/home");
        browser.displayTab("tab-b", "https://b.example/home");
        browser.navigateDisplayedTabTo("https://b.example/page");

        Assert.assertTrue(browser.pressBack());
        Assert.assertEquals("https://b.example/home", browser.webView.currentUrl());

        Assert.assertFalse(browser.pressBack());
        Assert.assertEquals("https://b.example/home", browser.webView.currentUrl());
    }

    @Test
    public void sameTabRedisplayPreservesTheAccumulatedHistory() {
        BrowserModel browser = new BrowserModel();

        browser.displayTab("tab-a", "https://a.example/home");
        browser.navigateDisplayedTabTo("https://a.example/page");

        browser.displayTab("tab-a", "https://a.example/page");

        Assert.assertTrue(browser.pressBack());
        Assert.assertEquals("https://a.example/home", browser.webView.currentUrl());
    }
}
