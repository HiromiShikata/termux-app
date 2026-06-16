package com.termux.app.browser;

import android.content.Intent;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class BrowserBulkOpenControllerTest {

    @Test
    public void buildViewIntentsReturnsEmptyForEmptyInput() {
        Assert.assertTrue(
            BrowserBulkOpenController.buildViewIntents(Collections.emptyList()).isEmpty());
    }

    @Test
    public void buildViewIntentsPreservesOrderAndCount() {
        List<String> urls = Arrays.asList(
            "https://github.com/o/r/issues/1",
            "https://github.com/o/r/pull/2",
            "https://github.com/o/r/issues/3");
        List<Intent> intents = BrowserBulkOpenController.buildViewIntents(urls);
        Assert.assertEquals(3, intents.size());
        Assert.assertEquals("https://github.com/o/r/issues/1", intents.get(0).getDataString());
        Assert.assertEquals("https://github.com/o/r/pull/2", intents.get(1).getDataString());
        Assert.assertEquals("https://github.com/o/r/issues/3", intents.get(2).getDataString());
    }

    @Test
    public void buildViewIntentsUsesActionViewWithNewTaskFlags() {
        Intent intent = BrowserBulkOpenController.buildViewIntents(
            Collections.singletonList("https://github.com/o/r/issues/1")).get(0);
        Assert.assertEquals(Intent.ACTION_VIEW, intent.getAction());
        Assert.assertTrue((intent.getFlags() & Intent.FLAG_ACTIVITY_NEW_TASK) != 0);
        Assert.assertTrue((intent.getFlags() & Intent.FLAG_ACTIVITY_MULTIPLE_TASK) != 0);
    }
}
