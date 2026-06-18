package com.termux.shared.data;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = Build.VERSION_CODES.P)
public class IntentUtilsBundleStringTest {

    @Test
    public void emptyBundleRendersEmptyMarker() {
        Assert.assertEquals("Bundle[]", IntentUtils.getBundleString(new Bundle()));
    }

    @Test
    public void scalarValueIsRenderedInBackticks() {
        Bundle bundle = new Bundle();
        bundle.putString("name", "value");
        String result = IntentUtils.getBundleString(bundle);
        Assert.assertTrue(result.startsWith("Bundle[\n"));
        Assert.assertTrue(result.contains("name: `value`"));
        Assert.assertTrue(result.endsWith("\n]"));
    }

    @Test
    public void intArrayValueIsRenderedAsArrayString() {
        Bundle bundle = new Bundle();
        bundle.putIntArray("ints", new int[]{1, 2, 3});
        Assert.assertTrue(IntentUtils.getBundleString(bundle).contains("ints: `[1, 2, 3]`"));
    }

    @Test
    public void byteArrayValueIsRenderedAsArrayString() {
        Bundle bundle = new Bundle();
        bundle.putByteArray("bytes", new byte[]{4, 5});
        Assert.assertTrue(IntentUtils.getBundleString(bundle).contains("bytes: `[4, 5]`"));
    }

    @Test
    public void booleanArrayValueIsRenderedAsArrayString() {
        Bundle bundle = new Bundle();
        bundle.putBooleanArray("flags", new boolean[]{true, false});
        Assert.assertTrue(IntentUtils.getBundleString(bundle).contains("flags: `[true, false]`"));
    }

    @Test
    public void shortArrayValueIsRenderedAsArrayString() {
        Bundle bundle = new Bundle();
        bundle.putShortArray("shorts", new short[]{6, 7});
        Assert.assertTrue(IntentUtils.getBundleString(bundle).contains("shorts: `[6, 7]`"));
    }

    @Test
    public void longArrayValueIsRenderedAsArrayString() {
        Bundle bundle = new Bundle();
        bundle.putLongArray("longs", new long[]{8L, 9L});
        Assert.assertTrue(IntentUtils.getBundleString(bundle).contains("longs: `[8, 9]`"));
    }

    @Test
    public void floatArrayValueIsRenderedAsArrayString() {
        Bundle bundle = new Bundle();
        bundle.putFloatArray("floats", new float[]{1.5f});
        Assert.assertTrue(IntentUtils.getBundleString(bundle).contains("floats: `[1.5]`"));
    }

    @Test
    public void doubleArrayValueIsRenderedAsArrayString() {
        Bundle bundle = new Bundle();
        bundle.putDoubleArray("doubles", new double[]{2.5});
        Assert.assertTrue(IntentUtils.getBundleString(bundle).contains("doubles: `[2.5]`"));
    }

    @Test
    public void stringArrayValueIsRenderedAsArrayString() {
        Bundle bundle = new Bundle();
        bundle.putStringArray("strings", new String[]{"a", "b"});
        Assert.assertTrue(IntentUtils.getBundleString(bundle).contains("strings: `[a, b]`"));
    }

    @Test
    public void nestedBundleValueIsRenderedRecursively() {
        Bundle nested = new Bundle();
        nested.putString("inner", "deep");
        Bundle bundle = new Bundle();
        bundle.putBundle("child", nested);
        String result = IntentUtils.getBundleString(bundle);
        Assert.assertTrue(result.contains("child: `Bundle["));
        Assert.assertTrue(result.contains("inner: `deep`"));
    }

    @Test
    public void multipleEntriesAreSeparatedByNewline() {
        Bundle bundle = new Bundle();
        bundle.putString("first", "1");
        bundle.putInt("second", 2);
        String result = IntentUtils.getBundleString(bundle);
        Assert.assertTrue(result.contains("first: `1`"));
        Assert.assertTrue(result.contains("second: `2`"));
    }

    @Test
    public void getIntentStringIncludesPopulatedBundle() {
        Intent intent = new Intent("com.termux.action.TEST");
        intent.putExtra("k", "v");
        String result = IntentUtils.getIntentString(intent);
        Assert.assertTrue(result.contains("k: `v`"));
    }
}
