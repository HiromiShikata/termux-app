package com.termux.app.browser;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class SessionNameBrowserTabUrlResolverTest {

    private final SessionNameBrowserTabUrlResolver resolver = new SessionNameBrowserTabUrlResolver();

    @Test
    public void restoredUrlSessionNameYieldsBrowserTabUrl() {
        String sessionName = "https://github.com/HiromiShikata/termux-app/issues/224";
        Assert.assertEquals(sessionName, resolver.resolve(sessionName));
    }

    @Test
    public void restoredIpHostUrlSessionNameWithPortYieldsBrowserTabUrl() {
        String sessionName = "http://50.30.32.53:9980/in-tmux-by-human/index.v3.json";
        Assert.assertEquals(sessionName, resolver.resolve(sessionName));
    }

    @Test
    public void restoredNonUrlSessionNameYieldsNoBrowserTab() {
        Assert.assertNull(resolver.resolve("bash"));
        Assert.assertNull(resolver.resolve("my project session"));
    }

    @Test
    public void surroundingWhitespaceIsTrimmedFromTheBrowserTabUrl() {
        Assert.assertEquals("https://example.com/", resolver.resolve("  https://example.com/  "));
    }

    @Test
    public void nullOrEmptySessionNameYieldsNoBrowserTab() {
        Assert.assertNull(resolver.resolve(null));
        Assert.assertNull(resolver.resolve(""));
        Assert.assertNull(resolver.resolve("   "));
    }

    @Test
    public void restoringMixedSessionNamesAttachesTabsOnlyForUrlNames() {
        List<String> persistedSessionNames = Arrays.asList(
            "https://github.com/HiromiShikata/termux-app/issues/110",
            "bash",
            "https://example.com/dashboard",
            "login");

        List<String> attachedBrowserTabUrls = new ArrayList<>();
        for (String name : persistedSessionNames) {
            String browserTabUrl = resolver.resolve(name);
            if (browserTabUrl != null) attachedBrowserTabUrls.add(browserTabUrl);
        }

        Assert.assertEquals(
            Arrays.asList(
                "https://github.com/HiromiShikata/termux-app/issues/110",
                "https://example.com/dashboard"),
            attachedBrowserTabUrls);
    }
}
