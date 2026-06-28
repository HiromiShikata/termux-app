package com.termux.app.browser;

import org.junit.Assert;
import org.junit.Test;

public class ProjectFooterActionTabSelectionTest {

    private static final String SESSION = "project-browser-session";
    private static final String OVERVIEW_URL = "https://overview.example/project";
    private static final String CONSOLE_URL = "https://console.example/project";
    private static final String NEW_TASK_URL = "https://tasks.example/project/new";

    @Test
    public void reusesTabWhenItsCurrentUrlEqualsTargetUrl() {
        BrowserTabManager manager = new BrowserTabManager();
        BrowserTab tab = manager.addTab(SESSION, OVERVIEW_URL);

        BrowserTab reusable =
            ProjectFooterActionTabSelection.resolveReusableTab(manager, SESSION, OVERVIEW_URL);

        Assert.assertSame(tab, reusable);
        Assert.assertEquals(1, manager.getTabs(SESSION).size());
    }

    @Test
    public void opensNewTabWhenOpenTabCurrentUrlDiffersFromTargetUrl() {
        BrowserTabManager manager = new BrowserTabManager();
        BrowserTab tab = manager.addTab(SESSION, OVERVIEW_URL);
        tab.setUrl("https://overview.example/project/other-page");

        BrowserTab reusable =
            ProjectFooterActionTabSelection.resolveReusableTab(manager, SESSION, OVERVIEW_URL);

        Assert.assertNull(reusable);
    }

    @Test
    public void reusesTabAfterItNavigatesBackToTargetUrl() {
        BrowserTabManager manager = new BrowserTabManager();
        BrowserTab tab = manager.addTab(SESSION, OVERVIEW_URL);
        tab.setUrl("https://overview.example/project/other-page");
        tab.setUrl(OVERVIEW_URL);

        BrowserTab reusable =
            ProjectFooterActionTabSelection.resolveReusableTab(manager, SESSION, OVERVIEW_URL);

        Assert.assertSame(tab, reusable);
    }

    @Test
    public void opensNewTabForConsoleWhenConsoleTabNavigatedAway() {
        BrowserTabManager manager = new BrowserTabManager();
        BrowserTab consoleTab = manager.addTab(SESSION, CONSOLE_URL);
        consoleTab.setUrl("https://console.example/project/logs");

        BrowserTab reusable =
            ProjectFooterActionTabSelection.resolveReusableTab(manager, SESSION, CONSOLE_URL);

        Assert.assertNull(reusable);
    }

    @Test
    public void opensNewTabForNewTaskWhenNewTaskTabNavigatedAway() {
        BrowserTabManager manager = new BrowserTabManager();
        BrowserTab newTaskTab = manager.addTab(SESSION, NEW_TASK_URL);
        newTaskTab.setUrl("https://tasks.example/project/123");

        BrowserTab reusable =
            ProjectFooterActionTabSelection.resolveReusableTab(manager, SESSION, NEW_TASK_URL);

        Assert.assertNull(reusable);
    }

    @Test
    public void reusesOnlyTheTabWhoseCurrentUrlMatchesAmongManyTabs() {
        BrowserTabManager manager = new BrowserTabManager();
        manager.addTab(SESSION, OVERVIEW_URL);
        BrowserTab consoleTab = manager.addTab(SESSION, CONSOLE_URL);
        manager.addTab(SESSION, NEW_TASK_URL);

        BrowserTab reusable =
            ProjectFooterActionTabSelection.resolveReusableTab(manager, SESSION, CONSOLE_URL);

        Assert.assertSame(consoleTab, reusable);
    }
}
