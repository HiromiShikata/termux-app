package com.termux.shared.models;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = Build.VERSION_CODES.P)
public class TextIOInfoTest {

    private TextIOInfo newInfo() {
        return new TextIOInfo("com.example.ACTION", "com.example.sender");
    }

    @Test
    public void constructorStoresActionAndSender() {
        TextIOInfo info = newInfo();
        Assert.assertEquals("com.example.ACTION", info.getAction());
        Assert.assertEquals("com.example.sender", info.getSender());
    }

    @Test
    public void defaultValuesMatchDocumentedDefaults() {
        TextIOInfo info = newInfo();
        Assert.assertFalse(info.shouldShowBackButtonInActionBar());
        Assert.assertFalse(info.isLabelEnabled());
        Assert.assertEquals(14, info.getLabelSize());
        Assert.assertEquals(Color.BLACK, info.getLabelColor());
        Assert.assertEquals("sans-serif", info.getLabelTypeFaceFamily());
        Assert.assertEquals(Typeface.BOLD, info.getLabelTypeFaceStyle());
        Assert.assertEquals(12, info.getTextSize());
        Assert.assertEquals(Color.BLACK, info.getTextColor());
        Assert.assertEquals("sans-serif", info.getTextTypeFaceFamily());
        Assert.assertEquals(Typeface.NORMAL, info.getTextTypeFaceStyle());
        Assert.assertEquals(TextIOInfo.TEXT_SIZE_LIMIT_IN_BYTES, info.getTextLengthLimit());
        Assert.assertFalse(info.isHorizontallyScrollable());
        Assert.assertFalse(info.shouldShowTextCharacterUsage());
        Assert.assertFalse(info.isEditingTextDisabled());
    }

    @Test
    public void titleAndBackButtonSettersWork() {
        TextIOInfo info = newInfo();
        info.setTitle("My Title");
        info.setShowBackButtonInActionBar(true);
        Assert.assertEquals("My Title", info.getTitle());
        Assert.assertTrue(info.shouldShowBackButtonInActionBar());
    }

    @Test
    public void labelSettersStoreValues() {
        TextIOInfo info = newInfo();
        info.setLabelEnabled(true);
        info.setLabel("A label");
        info.setLabelColor(Color.RED);
        info.setLabelTypeFaceFamily("monospace");
        info.setLabelTypeFaceStyle(Typeface.ITALIC);
        Assert.assertTrue(info.isLabelEnabled());
        Assert.assertEquals("A label", info.getLabel());
        Assert.assertEquals(Color.RED, info.getLabelColor());
        Assert.assertEquals("monospace", info.getLabelTypeFaceFamily());
        Assert.assertEquals(Typeface.ITALIC, info.getLabelTypeFaceStyle());
    }

    @Test
    public void labelSizeIgnoresNonPositiveValues() {
        TextIOInfo info = newInfo();
        info.setLabelSize(20);
        Assert.assertEquals(20, info.getLabelSize());
        info.setLabelSize(0);
        Assert.assertEquals(20, info.getLabelSize());
        info.setLabelSize(-5);
        Assert.assertEquals(20, info.getLabelSize());
    }

    @Test
    public void textSettersStoreValues() {
        TextIOInfo info = newInfo();
        info.setText("Hello world");
        info.setTextColor(Color.BLUE);
        info.setTextTypeFaceFamily("serif");
        info.setTextTypeFaceStyle(Typeface.BOLD_ITALIC);
        info.setTextHorizontallyScrolling(true);
        info.setShowTextCharacterUsage(true);
        info.setEditingTextDisabled(true);
        Assert.assertEquals("Hello world", info.getText());
        Assert.assertEquals(Color.BLUE, info.getTextColor());
        Assert.assertEquals("serif", info.getTextTypeFaceFamily());
        Assert.assertEquals(Typeface.BOLD_ITALIC, info.getTextTypeFaceStyle());
        Assert.assertTrue(info.isHorizontallyScrollable());
        Assert.assertTrue(info.shouldShowTextCharacterUsage());
        Assert.assertTrue(info.isEditingTextDisabled());
    }

    @Test
    public void textSizeIgnoresNonPositiveValues() {
        TextIOInfo info = newInfo();
        info.setTextSize(18);
        Assert.assertEquals(18, info.getTextSize());
        info.setTextSize(0);
        Assert.assertEquals(18, info.getTextSize());
    }

    @Test
    public void textLengthLimitOnlyAcceptsValueBelowMaximum() {
        TextIOInfo info = newInfo();
        info.setTextLengthLimit(50);
        Assert.assertEquals(50, info.getTextLengthLimit());
        info.setTextLengthLimit(TextIOInfo.TEXT_SIZE_LIMIT_IN_BYTES + 10);
        Assert.assertEquals(50, info.getTextLengthLimit());
    }

    @Test
    public void textSizeLimitDerivedFromOtherLimits() {
        Assert.assertEquals(
            100000 - TextIOInfo.GENERAL_DATA_SIZE_LIMIT_IN_BYTES - TextIOInfo.LABEL_SIZE_LIMIT_IN_BYTES,
            TextIOInfo.TEXT_SIZE_LIMIT_IN_BYTES);
    }

    @Test
    public void instanceIsSerializableAcrossRoundTrip() throws Exception {
        TextIOInfo info = newInfo();
        info.setTitle("Serializable");
        info.setText("payload");
        info.setLabel("label");

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(info);
        }

        TextIOInfo restored;
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            restored = (TextIOInfo) in.readObject();
        }

        Assert.assertEquals("Serializable", restored.getTitle());
        Assert.assertEquals("payload", restored.getText());
        Assert.assertEquals("label", restored.getLabel());
        Assert.assertEquals("com.example.ACTION", restored.getAction());
    }
}
