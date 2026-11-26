package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

public class CallToUserNotificationTagsTest {

    @Test
    public void tagIsStablePerSessionAndRoundTripsToTheSessionName() {
        String[] sessionNames = {"worker", "reviewer", "session with spaces", "a:b"};
        for (String sessionName : sessionNames) {
            String tag = CallToUserNotificationTags.tagFor(sessionName);

            Assert.assertEquals(tag, CallToUserNotificationTags.tagFor(sessionName));
            Assert.assertTrue(CallToUserNotificationTags.isCallToUserTag(tag));
            Assert.assertEquals(sessionName, CallToUserNotificationTags.sessionNameFromTag(tag));
        }
    }

    @Test
    public void differentSessionsGetDifferentTags() {
        Assert.assertNotEquals(CallToUserNotificationTags.tagFor("worker"),
            CallToUserNotificationTags.tagFor("reviewer"));
    }

    @Test
    public void foreignTagsAreNotRecognised() {
        Assert.assertFalse(CallToUserNotificationTags.isCallToUserTag("apk-update"));
        Assert.assertFalse(CallToUserNotificationTags.isCallToUserTag(null));
        Assert.assertNull(CallToUserNotificationTags.sessionNameFromTag("apk-update"));
    }
}
