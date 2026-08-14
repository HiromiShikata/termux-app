package com.termux.app.ownercall;

import com.termux.app.terminal.HostTmuxSessionName;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class OwnerCallFileUrlTest {

    private static final String ACCESS_TOKEN = "test-access-token";
    private static final String SESSION_DEFINITION_URL =
        "https://calls.example.test/in-tmux-by-human/index.v4.json?k=" + ACCESS_TOKEN;
    private static final String SESSION_URL =
        "https://github.com/HiromiShikata/termux-app/issues/1884";

    @Test
    public void addressesTheFileServedBesideTheStoredSessionDefinitionDocument() {
        Assert.assertEquals("https://calls.example.test/in-tmux-by-human/call-to-user/umino/"
                + "https___github_com_HiromiShikata_termux-app_issues_1884.yaml?k=" + ACCESS_TOKEN,
            OwnerCallFileUrl.resolve(SESSION_DEFINITION_URL, "umino", SESSION_URL));
    }

    @Test
    public void addressesExactlyTheFileTheSharedPathRuleNames() {
        String resolved = OwnerCallFileUrl.resolve(SESSION_DEFINITION_URL, "umino", SESSION_URL);

        Assert.assertEquals(directoryOf(SESSION_DEFINITION_URL)
                + OwnerCallFilePath.of("umino", HostTmuxSessionName.normalize(SESSION_URL))
                + "?k=" + ACCESS_TOKEN,
            resolved);
    }

    @Test
    public void keepsThePortOfTheStoredSessionDefinitionUrl() {
        Assert.assertEquals("http://127.0.0.1:8080/in-tmux-by-human/call-to-user/NA/secretary.yaml"
                + "?k=" + ACCESS_TOKEN,
            OwnerCallFileUrl.resolve("http://127.0.0.1:8080/in-tmux-by-human/index.v4.json?k="
                + ACCESS_TOKEN, null, "secretary"));
    }

    @Test
    public void carriesNoQueryWhenTheStoredSessionDefinitionUrlCarriesNoToken() {
        Assert.assertEquals("https://calls.example.test/in-tmux-by-human/call-to-user/NA/app.yaml",
            OwnerCallFileUrl.resolve("https://calls.example.test/in-tmux-by-human/index.v4.json",
                null, "app"));
    }

    @Test
    public void addressesTheRootWhenTheStoredSessionDefinitionDocumentSitsThere() {
        Assert.assertEquals("https://calls.example.test/call-to-user/NA/app.yaml",
            OwnerCallFileUrl.resolve("https://calls.example.test/index.v4.json", null, "app"));
    }

    @Test
    public void resolvesNothingWithoutASessionDefinitionUrlOrASession() {
        Assert.assertNull(OwnerCallFileUrl.resolve("", "umino", SESSION_URL));
        Assert.assertNull(OwnerCallFileUrl.resolve(SESSION_DEFINITION_URL, "umino", null));
        Assert.assertNull(OwnerCallFileUrl.resolve("not a url", "umino", SESSION_URL));
    }

    private static String directoryOf(String url) {
        return url.substring(0, url.lastIndexOf('/') + 1);
    }
}
