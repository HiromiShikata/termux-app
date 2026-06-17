package com.termux.shared.net.uri;

import android.net.Uri;
import android.os.Build;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = Build.VERSION_CODES.P)
public class UriUtilsTest {

    @Test
    public void getUriFilePathWithFragmentReturnsNullForNullUri() {
        Assert.assertNull(UriUtils.getUriFilePathWithFragment(null));
    }

    @Test
    public void getUriFilePathWithFragmentReturnsPathWhenNoFragment() {
        Assert.assertEquals("/data/local/file.txt",
            UriUtils.getUriFilePathWithFragment(Uri.parse("file:///data/local/file.txt")));
    }

    @Test
    public void getUriFilePathWithFragmentAppendsFragmentToPath() {
        Assert.assertEquals("/data/local/file.txt#section",
            UriUtils.getUriFilePathWithFragment(Uri.parse("file:///data/local/file.txt#section")));
    }

    @Test
    public void getUriFilePathWithFragmentReturnsNullForEmptyPath() {
        Assert.assertNull(UriUtils.getUriFilePathWithFragment(Uri.parse("content://authority")));
    }

    @Test
    public void getUriFileBasenameReturnsNullForNullUri() {
        Assert.assertNull(UriUtils.getUriFileBasename(null, false));
    }

    @Test
    public void getUriFileBasenameReturnsBasenameWithoutFragment() {
        Assert.assertEquals("file.txt",
            UriUtils.getUriFileBasename(Uri.parse("file:///data/local/file.txt"), false));
    }

    @Test
    public void getUriFileBasenameReturnsNullForEmptyPathWithoutFragment() {
        Assert.assertNull(UriUtils.getUriFileBasename(Uri.parse("content://authority"), false));
    }

    @Test
    public void getUriFileBasenameIncludesFragmentWhenRequested() {
        Assert.assertEquals("file.txt#section",
            UriUtils.getUriFileBasename(Uri.parse("file:///data/local/file.txt#section"), true));
    }

    @Test
    public void getFileUriBuildsFileSchemeUriForPath() {
        Uri uri = UriUtils.getFileUri("/data/local/file.txt");
        Assert.assertEquals(UriScheme.SCHEME_FILE, uri.getScheme());
        Assert.assertEquals("/data/local/file.txt", uri.getPath());
    }

    @Test
    public void getFileUriBuildsFileSchemeUriWithAuthority() {
        Uri uri = UriUtils.getFileUri("example.authority", "/data/local/file.txt");
        Assert.assertEquals(UriScheme.SCHEME_FILE, uri.getScheme());
        Assert.assertEquals("example.authority", uri.getAuthority());
        Assert.assertEquals("/data/local/file.txt", uri.getPath());
    }

    @Test
    public void getContentUriBuildsContentSchemeUriForPath() {
        Uri uri = UriUtils.getContentUri("/data/local/file.txt");
        Assert.assertEquals(UriScheme.SCHEME_CONTENT, uri.getScheme());
        Assert.assertEquals("/data/local/file.txt", uri.getPath());
    }

    @Test
    public void getContentUriBuildsContentSchemeUriWithAuthority() {
        Uri uri = UriUtils.getContentUri("example.authority", "/data/local/file.txt");
        Assert.assertEquals(UriScheme.SCHEME_CONTENT, uri.getScheme());
        Assert.assertEquals("example.authority", uri.getAuthority());
        Assert.assertEquals("/data/local/file.txt", uri.getPath());
    }
}
