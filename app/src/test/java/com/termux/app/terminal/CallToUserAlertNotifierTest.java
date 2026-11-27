package com.termux.app.terminal;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.shadows.ShadowNotificationManager;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class CallToUserAlertNotifierTest {

    private static final String SESSION_NAME = "worker";
    private static final String BODY = "Please review the plan.";

    private Context mContext;
    private ShadowNotificationManager mNotifications;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.getApplication();
        ShadowApplication.getInstance().grantPermissions(Manifest.permission.POST_NOTIFICATIONS);
        CallToUserAlertNotifier.setupChannel(mContext);
        mNotifications = Shadows.shadowOf((NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE));
    }

    @Test
    public void postedAlertIsNotCancelledByTappingIt() {
        CallToUserAlertNotifier.postAlert(mContext, SESSION_NAME, "🔴");

        Notification notification = alertNotification();
        Assert.assertEquals(0, notification.flags & Notification.FLAG_AUTO_CANCEL);
    }

    @Test
    public void bodyUpdateReplacesTheAlertTextInPlaceWithoutSoundingAgain() {
        CallToUserAlertNotifier.postAlert(mContext, SESSION_NAME, "🔴");

        CallToUserAlertNotifier.updateAlertBody(mContext, SESSION_NAME, BODY);

        Notification notification = alertNotification();
        Assert.assertEquals(BODY, notification.extras.getCharSequence(Notification.EXTRA_TEXT).toString());
        Assert.assertNotEquals(0, notification.flags & Notification.FLAG_ONLY_ALERT_ONCE);
        Assert.assertEquals(0, notification.flags & Notification.FLAG_AUTO_CANCEL);
        Assert.assertEquals(1, mNotifications.getAllNotifications().size());
    }

    @Test
    public void bodyUpdateIsIgnoredWhenNoAlertIsShownForTheSession() {
        CallToUserAlertNotifier.updateAlertBody(mContext, SESSION_NAME, BODY);

        Assert.assertEquals(0, mNotifications.getAllNotifications().size());
    }

    private Notification alertNotification() {
        Assert.assertEquals(1, mNotifications.getAllNotifications().size());
        return mNotifications.getAllNotifications().get(0);
    }
}
