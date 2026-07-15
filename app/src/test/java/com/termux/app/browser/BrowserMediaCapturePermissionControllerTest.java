package com.termux.app.browser;

import android.Manifest;
import android.app.Activity;
import android.net.Uri;
import android.webkit.PermissionRequest;

import androidx.annotation.NonNull;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowApplication;

@RunWith(RobolectricTestRunner.class)
public class BrowserMediaCapturePermissionControllerTest {

    private static final class RecordingPermissionRequest extends PermissionRequest {

        private final String[] resources;
        String[] grantedResources;
        boolean denied;

        RecordingPermissionRequest(String[] resources) {
            this.resources = resources;
        }

        @Override
        public Uri getOrigin() {
            return Uri.parse("https://meet.google.com");
        }

        @Override
        public String[] getResources() {
            return resources;
        }

        @Override
        public void grant(String[] resources) {
            this.grantedResources = resources;
        }

        @Override
        public void deny() {
            this.denied = true;
        }
    }

    private Activity buildActivity() {
        return Robolectric.buildActivity(Activity.class).create().get();
    }

    private BrowserMediaCapturePermissionController controllerFor(@NonNull Activity activity) {
        return new BrowserMediaCapturePermissionController(() -> activity);
    }

    @Test
    public void grantsImmediatelyWhenAndroidPermissionsAlreadyHeld() {
        Activity activity = buildActivity();
        ShadowApplication shadowApplication = Shadows.shadowOf(activity.getApplication());
        shadowApplication.grantPermissions(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO);

        RecordingPermissionRequest request = new RecordingPermissionRequest(new String[]{
            PermissionRequest.RESOURCE_VIDEO_CAPTURE, PermissionRequest.RESOURCE_AUDIO_CAPTURE});

        controllerFor(activity).onPermissionRequest(request);

        Assert.assertNotNull(request.grantedResources);
        Assert.assertEquals(2, request.grantedResources.length);
        Assert.assertFalse(request.denied);
    }

    @Test
    public void deniesWhenNoMediaCaptureResourcesRequested() {
        Activity activity = buildActivity();
        RecordingPermissionRequest request = new RecordingPermissionRequest(new String[]{
            PermissionRequest.RESOURCE_PROTECTED_MEDIA_ID});

        controllerFor(activity).onPermissionRequest(request);

        Assert.assertTrue(request.denied);
        Assert.assertNull(request.grantedResources);
    }

    @Test
    public void grantsAfterRuntimePermissionResultIsGranted() {
        Activity activity = buildActivity();
        RecordingPermissionRequest request = new RecordingPermissionRequest(new String[]{
            PermissionRequest.RESOURCE_VIDEO_CAPTURE});
        BrowserMediaCapturePermissionController controller = controllerFor(activity);

        controller.onPermissionRequest(request);
        Assert.assertNull(request.grantedResources);
        Assert.assertFalse(request.denied);

        boolean handled = controller.onRequestPermissionsResult(
            BrowserMediaCapturePermissionController.REQUEST_BROWSER_MEDIA_CAPTURE_PERMISSION,
            new String[]{Manifest.permission.CAMERA},
            new int[]{android.content.pm.PackageManager.PERMISSION_GRANTED});

        Assert.assertTrue(handled);
        Assert.assertNotNull(request.grantedResources);
        Assert.assertEquals(1, request.grantedResources.length);
        Assert.assertEquals(PermissionRequest.RESOURCE_VIDEO_CAPTURE, request.grantedResources[0]);
    }

    @Test
    public void deniesAfterRuntimePermissionResultIsDenied() {
        Activity activity = buildActivity();
        RecordingPermissionRequest request = new RecordingPermissionRequest(new String[]{
            PermissionRequest.RESOURCE_VIDEO_CAPTURE});
        BrowserMediaCapturePermissionController controller = controllerFor(activity);

        controller.onPermissionRequest(request);
        boolean handled = controller.onRequestPermissionsResult(
            BrowserMediaCapturePermissionController.REQUEST_BROWSER_MEDIA_CAPTURE_PERMISSION,
            new String[]{Manifest.permission.CAMERA},
            new int[]{android.content.pm.PackageManager.PERMISSION_DENIED});

        Assert.assertTrue(handled);
        Assert.assertTrue(request.denied);
        Assert.assertNull(request.grantedResources);
    }

    @Test
    public void ignoresUnrelatedRequestCodes() {
        Activity activity = buildActivity();
        boolean handled = controllerFor(activity).onRequestPermissionsResult(
            9999, new String[]{Manifest.permission.CAMERA},
            new int[]{android.content.pm.PackageManager.PERMISSION_GRANTED});

        Assert.assertFalse(handled);
    }
}
