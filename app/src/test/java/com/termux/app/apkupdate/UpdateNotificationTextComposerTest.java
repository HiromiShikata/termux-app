package com.termux.app.apkupdate;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class UpdateNotificationTextComposerTest {

    private static final class CompositionCase {
        final String description;
        final List<ApkRelease> releasesNewestFirst;
        final String expectedText;

        CompositionCase(String description, List<ApkRelease> releasesNewestFirst, String expectedText) {
            this.description = description;
            this.releasesNewestFirst = releasesNewestFirst;
            this.expectedText = expectedText;
        }
    }

    private static final CompositionCase[] COMPOSITION_CASES = {
        new CompositionCase(
            "lists header and change lines for each release, skipping heading and install hint lines",
            Arrays.asList(
                new ApkRelease("0.119.4241",
                    "## Changes\n- Fix A\n- Add B\n\nInstall the APK from the assets list."),
                new ApkRelease("0.119.4240", "- Fix C")),
            "0.119.4241\n- Fix A\n- Add B\n\n0.119.4240\n- Fix C"),
        new CompositionCase(
            "keeps a boilerplate-only release as its header line between other releases",
            Arrays.asList(
                new ApkRelease("0.119.4241", "- Fix A"),
                new ApkRelease("0.119.4240", "Download the APK from the release page to install."),
                new ApkRelease("0.119.4239", "- Fix B")),
            "0.119.4241\n- Fix A\n\n0.119.4240\n\n0.119.4239\n- Fix B"),
        new CompositionCase(
            "ignores blank lines and install hint paragraphs mixed among change lines",
            Arrays.asList(
                new ApkRelease("0.119.4241",
                    "\nInstall hint paragraph here.\n\n- Change one\n\n- Change two\n")),
            "0.119.4241\n- Change one\n- Change two"),
        new CompositionCase(
            "returns only the header line when a release body is empty",
            Collections.singletonList(new ApkRelease("0.119.4241", "")),
            "0.119.4241"),
        new CompositionCase(
            "returns empty text for empty release list",
            Collections.<ApkRelease>emptyList(),
            ""),
    };

    @Test
    public void composesHeaderAndChangeLinesPerRelease() {
        for (CompositionCase compositionCase : COMPOSITION_CASES) {
            Assert.assertEquals(compositionCase.description, compositionCase.expectedText,
                UpdateNotificationTextComposer.composeBigText(compositionCase.releasesNewestFirst));
        }
    }
}
