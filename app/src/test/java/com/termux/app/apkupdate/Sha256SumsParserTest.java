package com.termux.app.apkupdate;

import org.junit.Assert;
import org.junit.Test;

public class Sha256SumsParserTest {

    private static final String SAMPLE_SUMS =
        "7860f1c88d3ef49099117f8c9a21a70cd31c05e9ff0d58cd9fa52cbcbcbfd5d4  "
            + "termux-app_v0.119.2821+2d5b084-apt-android-7-github-debug_universal.apk\n"
            + "ace457f83d3fb03266f76d5e98245e8fb6f16070dc8e3122f25962135ebb71ca  "
            + "termux-app_v0.119.2821+2d5b084-apt-android-7-github-debug_arm64-v8a.apk\n"
            + "c19d85829bc96ec0be1521f03aa3a0f0c4b4cabc7b6386292357505fad1068ca  "
            + "termux-app_v0.119.2821+2d5b084-apt-android-7-github-debug_armeabi-v7a.apk\n";

    private final Sha256SumsParser parser = new Sha256SumsParser();

    @Test
    public void findsExpectedHashByAbiSuffixForShortenedAssetName() {
        String hash = parser.findExpectedSha256(SAMPLE_SUMS, "termux-app_arm64-v8a.apk");

        Assert.assertEquals("ace457f83d3fb03266f76d5e98245e8fb6f16070dc8e3122f25962135ebb71ca", hash);
    }

    @Test
    public void findsExpectedHashForUniversalAsset() {
        String hash = parser.findExpectedSha256(SAMPLE_SUMS, "termux-app_universal.apk");

        Assert.assertEquals("7860f1c88d3ef49099117f8c9a21a70cd31c05e9ff0d58cd9fa52cbcbcbfd5d4", hash);
    }

    @Test
    public void returnsNullWhenNoLineMatchesAbiSuffix() {
        Assert.assertNull(parser.findExpectedSha256(SAMPLE_SUMS, "termux-app_x86_64.apk"));
    }

    @Test
    public void returnsNullForNullContent() {
        Assert.assertNull(parser.findExpectedSha256(null, "termux-app_universal.apk"));
    }

    @Test
    public void returnsNullForAssetNameWithoutUnderscore() {
        Assert.assertNull(parser.findExpectedSha256(SAMPLE_SUMS, "universal.apk"));
    }

    @Test
    public void parsesBinaryModeStarPrefixedFileName() {
        String expectedHash = "aa" + repeatZeros(62);
        String sums = expectedHash + " *termux-app_v1+build_universal.apk\n";

        String hash = parser.findExpectedSha256(sums, "termux-app_universal.apk");

        Assert.assertEquals(expectedHash, hash);
    }

    private static String repeatZeros(int count) {
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < count; index++) {
            builder.append('0');
        }
        return builder.toString();
    }
}
