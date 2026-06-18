package com.termux.shared.android.resource;

import android.content.Context;
import android.os.Build;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = Build.VERSION_CODES.P)
public class ResourceUtilsTest {

    private Context context() {
        return RuntimeEnvironment.getApplication();
    }

    @Test
    public void resourceTypeConstantsMatchAndroidResourceTypeNames() {
        Assert.assertEquals("color", ResourceUtils.RES_TYPE_COLOR);
        Assert.assertEquals("drawable", ResourceUtils.RES_TYPE_DRAWABLE);
        Assert.assertEquals("id", ResourceUtils.RES_TYPE_ID);
        Assert.assertEquals("layout", ResourceUtils.RES_TYPE_LAYOUT);
        Assert.assertEquals("string", ResourceUtils.RES_TYPE_STRING);
        Assert.assertEquals("style", ResourceUtils.RES_TYPE_STYLE);
    }

    @Test
    public void getResourceIdReturnsNullForNullName() {
        Assert.assertNull(ResourceUtils.getResourceId(context(), null, ResourceUtils.RES_TYPE_STRING, false));
    }

    @Test
    public void getResourceIdReturnsNullForEmptyName() {
        Assert.assertNull(ResourceUtils.getResourceId(context(), "", ResourceUtils.RES_TYPE_STRING, false));
    }

    @Test
    public void getResourceIdReturnsNullForUnknownResource() {
        Assert.assertNull(ResourceUtils.getResourceId(context(),
            "a_resource_that_does_not_exist", ResourceUtils.RES_TYPE_STRING, "android", false));
    }

    @Test
    public void getStringResourceIdResolvesKnownAndroidStringResource() {
        Integer resourceId = ResourceUtils.getStringResourceId(context(), "ok", "android", false);
        Assert.assertNotNull(resourceId);
        Assert.assertEquals(android.R.string.ok, resourceId.intValue());
    }

    @Test
    public void getColorResourceIdResolvesKnownAndroidColorResource() {
        Integer resourceId = ResourceUtils.getColorResourceId(context(), "black", "android", false);
        Assert.assertNotNull(resourceId);
        Assert.assertEquals(android.R.color.black, resourceId.intValue());
    }

    @Test
    public void getIdResourceIdReturnsNullForUnknownId() {
        Assert.assertNull(ResourceUtils.getIdResourceId(context(),
            "an_id_that_does_not_exist", "android", false));
    }
}
