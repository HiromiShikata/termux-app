package com.termux.app.browser;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class BrowserHttpAuthRequestTest {

    @Test
    public void keepsEnteredCredentials() {
        BrowserHttpAuthRequest request = new BrowserHttpAuthRequest("guest", "guest");
        Assert.assertEquals("guest", request.getUsername());
        Assert.assertEquals("guest", request.getPassword());
    }

    @Test
    public void treatsNullUsernameAsEmptyString() {
        BrowserHttpAuthRequest request = new BrowserHttpAuthRequest(null, "secret");
        Assert.assertEquals("", request.getUsername());
        Assert.assertEquals("secret", request.getPassword());
    }

    @Test
    public void treatsNullPasswordAsEmptyString() {
        BrowserHttpAuthRequest request = new BrowserHttpAuthRequest("admin", null);
        Assert.assertEquals("admin", request.getUsername());
        Assert.assertEquals("", request.getPassword());
    }

    @Test
    public void treatsBothNullAsEmptyStrings() {
        BrowserHttpAuthRequest request = new BrowserHttpAuthRequest(null, null);
        Assert.assertEquals("", request.getUsername());
        Assert.assertEquals("", request.getPassword());
    }

    @Test
    public void keepsEmptyUsernameAsEntered() {
        BrowserHttpAuthRequest request = new BrowserHttpAuthRequest("", "password-only");
        Assert.assertEquals("", request.getUsername());
        Assert.assertEquals("password-only", request.getPassword());
    }

    @Test
    public void preservesWhitespaceInsideCredentials() {
        BrowserHttpAuthRequest request = new BrowserHttpAuthRequest(" user name ", " pass word ");
        Assert.assertEquals(" user name ", request.getUsername());
        Assert.assertEquals(" pass word ", request.getPassword());
    }
}
