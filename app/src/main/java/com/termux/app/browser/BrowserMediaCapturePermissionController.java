package com.termux.app.browser;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.webkit.PermissionRequest;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.termux.shared.logger.Logger;

import java.util.ArrayList;
import java.util.List;

public final class BrowserMediaCapturePermissionController {

    public static final int REQUEST_BROWSER_MEDIA_CAPTURE_PERMISSION = 3100;

    private static final String LOG_TAG = "BrowserMediaCapturePermissionController";

    public interface Host {

        @NonNull
        Activity getActivity();
    }

    private final Host mHost;

    @Nullable
    private PermissionRequest mPendingRequest;

    @Nullable
    private String[] mPendingResources;

    public BrowserMediaCapturePermissionController(@NonNull Host host) {
        this.mHost = host;
    }

    public void onPermissionRequest(@NonNull PermissionRequest request) {
        String[] requestedResources = requestedMediaCaptureResources(request);
        if (requestedResources.length == 0) {
            request.deny();
            return;
        }

        String[] androidPermissions = androidPermissionsFor(requestedResources);
        if (hasAllPermissions(androidPermissions)) {
            request.grant(requestedResources);
            return;
        }

        if (mPendingRequest != null) {
            request.deny();
            return;
        }

        mPendingRequest = request;
        mPendingResources = requestedResources;
        ActivityCompat.requestPermissions(
            mHost.getActivity(), androidPermissions, REQUEST_BROWSER_MEDIA_CAPTURE_PERMISSION);
    }

    public void onPermissionRequestCanceled(@NonNull PermissionRequest request) {
        if (mPendingRequest == request) {
            mPendingRequest = null;
            mPendingResources = null;
        }
    }

    public boolean onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode != REQUEST_BROWSER_MEDIA_CAPTURE_PERMISSION) return false;

        PermissionRequest request = mPendingRequest;
        String[] resources = mPendingResources;
        mPendingRequest = null;
        mPendingResources = null;
        if (request == null || resources == null) return true;

        boolean allGranted = grantResults.length > 0;
        for (int grantResult : grantResults) {
            if (grantResult != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }

        try {
            if (allGranted) {
                request.grant(resources);
            } else {
                request.deny();
            }
        } catch (RuntimeException e) {
            Logger.logStackTraceWithMessage(LOG_TAG, "Failed to resolve media capture permission", e);
        }
        return true;
    }

    @NonNull
    private String[] requestedMediaCaptureResources(@NonNull PermissionRequest request) {
        List<String> resources = new ArrayList<>();
        for (String resource : request.getResources()) {
            if (PermissionRequest.RESOURCE_VIDEO_CAPTURE.equals(resource)
                || PermissionRequest.RESOURCE_AUDIO_CAPTURE.equals(resource)) {
                resources.add(resource);
            }
        }
        return resources.toArray(new String[0]);
    }

    @NonNull
    private String[] androidPermissionsFor(@NonNull String[] resources) {
        List<String> permissions = new ArrayList<>();
        for (String resource : resources) {
            if (PermissionRequest.RESOURCE_VIDEO_CAPTURE.equals(resource)) {
                permissions.add(Manifest.permission.CAMERA);
            } else if (PermissionRequest.RESOURCE_AUDIO_CAPTURE.equals(resource)) {
                permissions.add(Manifest.permission.RECORD_AUDIO);
            }
        }
        return permissions.toArray(new String[0]);
    }

    private boolean hasAllPermissions(@NonNull String[] permissions) {
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(mHost.getActivity(), permission)
                != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }
}
