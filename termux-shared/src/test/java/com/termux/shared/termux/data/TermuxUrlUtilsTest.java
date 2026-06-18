package com.termux.shared.termux.data;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.regex.Pattern;

public class TermuxUrlUtilsTest {

    @Test
    public void extractsHttpsUrlWithoutSurroundingText() {
        Assert.assertEquals(Arrays.asList("https://example.com"),
            extract("Visit https://example.com for info"));
    }

    @Test
    public void extractsHttpUrlIncludingPath() {
        Assert.assertEquals(Arrays.asList("http://example.com/path"),
            extract("go to http://example.com/path now"));
    }

    @Test
    public void extractsFtpAndFtpsSchemes() {
        Assert.assertEquals(Arrays.asList("ftp://ftp.example.com", "ftps://secure.example.com"),
            extract("ftp://ftp.example.com ftps://secure.example.com"));
    }

    @Test
    public void extractsGitScheme() {
        Assert.assertEquals(Arrays.asList("git://github.com/user/repo.git"),
            extract("git://github.com/user/repo.git"));
    }

    @Test
    public void extractsSshFamilyWebsocketAndFileSchemes() {
        Assert.assertEquals(
            Arrays.asList("sftp://host.example.com/dir", "ws://host.io", "wss://host.io", "file:///etc/hosts"),
            extract("sftp://host.example.com/dir ws://host.io wss://host.io file:///etc/hosts"));
    }

    @Test
    public void extractsUrlWithUserInfo() {
        Assert.assertEquals(Arrays.asList("https://user:pass@example.com/account"),
            extract("login at https://user:pass@example.com/account"));
    }

    @Test
    public void extractsUrlWithIpv4Host() {
        Assert.assertEquals(Arrays.asList("http://192.168.0.1/dashboard"),
            extract("server http://192.168.0.1/dashboard"));
    }

    @Test
    public void extractsUrlWithPort() {
        Assert.assertEquals(Arrays.asList("http://example.com:8080/v1"),
            extract("api http://example.com:8080/v1"));
    }

    @Test
    public void extractsUrlWithQueryString() {
        Assert.assertEquals(Arrays.asList("https://example.com/search?q=android&page=2"),
            extract("search https://example.com/search?q=android&page=2"));
    }

    @Test
    public void extractsUrlWithFragment() {
        Assert.assertEquals(Arrays.asList("https://example.com/docs#section-2"),
            extract("anchor https://example.com/docs#section-2"));
    }

    @Test
    public void extractsMultipleUrlsInInsertionOrderCollapsingDuplicates() {
        Assert.assertEquals(Arrays.asList("https://a.example.com", "https://b.example.com"),
            extract("first https://a.example.com then https://b.example.com and https://a.example.com again"));
    }

    @Test
    public void textWithoutAnyUrlReturnsEmptySet() {
        Assert.assertTrue(TermuxUrlUtils.extractUrls("no url here just plain text and a.b without scheme").isEmpty());
    }

    @Test
    public void getUrlMatchRegexReturnsSameCachedInstanceOnSecondCall() {
        Pattern first = TermuxUrlUtils.getUrlMatchRegex();
        Pattern second = TermuxUrlUtils.getUrlMatchRegex();
        Assert.assertSame(first, second);
    }

    private static List<CharSequence> extract(String text) {
        LinkedHashSet<CharSequence> result = TermuxUrlUtils.extractUrls(text);
        return new ArrayList<>(result);
    }
}
