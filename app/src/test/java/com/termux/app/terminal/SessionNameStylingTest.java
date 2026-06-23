package com.termux.app.terminal;

import android.graphics.Typeface;
import android.text.Spanned;
import android.text.SpannableString;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class SessionNameStylingTest {

    @Test
    public void appliesARelativeSizeSmallerThanOneToTheSessionNameSoLongNamesShrinkInsteadOfWrapping() {
        String sessionName = "github.com/HiromiShikata/termux-app/issues/154";
        String fullTitle = sessionName + "\nsecondary definition line";
        SpannableString styled = new SpannableString(fullTitle);

        TermuxSessionsListViewController.applySessionNameStyling(
            styled, 0, sessionName.length(), new StyleSpan(Typeface.BOLD));

        RelativeSizeSpan[] sizeSpans = styled.getSpans(0, sessionName.length(), RelativeSizeSpan.class);
        Assert.assertEquals(1, sizeSpans.length);
        Assert.assertEquals(SessionRow.SESSION_NAME_RELATIVE_SIZE,
            sizeSpans[0].getSizeChange(), 0.0001f);
        Assert.assertTrue(sizeSpans[0].getSizeChange() < 1.0f);
        Assert.assertEquals(0, styled.getSpanStart(sizeSpans[0]));
        Assert.assertEquals(sessionName.length(), styled.getSpanEnd(sizeSpans[0]));
    }

    @Test
    public void doesNotShrinkTheSecondaryDefinitionAndTitleLinesBelowTheSessionName() {
        String sessionName = "github.com/HiromiShikata/termux-app";
        String fullTitle = sessionName + "\nsecondary definition line";
        SpannableString styled = new SpannableString(fullTitle);

        TermuxSessionsListViewController.applySessionNameStyling(
            styled, 0, sessionName.length(), new StyleSpan(Typeface.BOLD));

        RelativeSizeSpan[] secondaryLineSpans =
            styled.getSpans(sessionName.length() + 1, fullTitle.length(), RelativeSizeSpan.class);
        Assert.assertEquals(0, secondaryLineSpans.length);
    }

    @Test
    public void keepsTheSessionNameBoldSoItStaysTheVisualPrimaryLine() {
        String sessionName = "github.com/HiromiShikata/termux-app";
        SpannableString styled = new SpannableString(sessionName);
        StyleSpan boldSpan = new StyleSpan(Typeface.BOLD);

        TermuxSessionsListViewController.applySessionNameStyling(styled, 0, sessionName.length(), boldSpan);

        StyleSpan[] styleSpans = styled.getSpans(0, sessionName.length(), StyleSpan.class);
        Assert.assertEquals(1, styleSpans.length);
        Assert.assertEquals(Typeface.BOLD, styleSpans[0].getStyle());
    }

    @Test
    public void appliesNoNameSpansWhenTheSessionNameIsEmpty() {
        SpannableString styled = new SpannableString("only a secondary line");

        TermuxSessionsListViewController.applySessionNameStyling(styled, 0, 0, new StyleSpan(Typeface.BOLD));

        Assert.assertEquals(0, styled.getSpans(0, styled.length(), RelativeSizeSpan.class).length);
        Assert.assertEquals(0, styled.getSpans(0, styled.length(), StyleSpan.class).length);
    }

    @Test
    public void bellNotificationLabelReceivesARelativeSizeSmallerThanOneToDistinguishItFromTheSessionName() {
        String sessionName = "mySession";
        String ageLabel = " 5秒前";
        String fullTitle = sessionName + ageLabel;
        SpannableString styled = new SpannableString(fullTitle);
        int ageLabelStart = sessionName.length();
        int ageLabelEnd = fullTitle.length();

        TermuxSessionsListViewController.applyBellNotificationLabelStyling(
            styled, ageLabelStart, ageLabelEnd, new StyleSpan(Typeface.ITALIC));

        RelativeSizeSpan[] sizeSpans = styled.getSpans(ageLabelStart, ageLabelEnd, RelativeSizeSpan.class);
        Assert.assertEquals(1, sizeSpans.length);
        Assert.assertTrue(sizeSpans[0].getSizeChange() < 1.0f);
        Assert.assertEquals(ageLabelStart, styled.getSpanStart(sizeSpans[0]));
        Assert.assertEquals(ageLabelEnd, styled.getSpanEnd(sizeSpans[0]));
    }

    @Test
    public void bellNotificationLabelStylingAppliesToOnlyTheAgeLabelAndNotTheSessionName() {
        String sessionName = "mySession";
        String ageLabel = " 3分前";
        String fullTitle = sessionName + ageLabel;
        SpannableString styled = new SpannableString(fullTitle);

        TermuxSessionsListViewController.applyBellNotificationLabelStyling(
            styled, sessionName.length(), fullTitle.length(), new StyleSpan(Typeface.ITALIC));

        RelativeSizeSpan[] sessionNameSpans = styled.getSpans(0, sessionName.length(), RelativeSizeSpan.class);
        Assert.assertEquals(0, sessionNameSpans.length);
    }

    @Test
    public void bellNotificationLabelStylingAppliesToItalicSpanOnTheAgeLabelRange() {
        String sessionName = "mySession";
        String ageLabel = " 2時間前";
        String fullTitle = sessionName + ageLabel;
        SpannableString styled = new SpannableString(fullTitle);
        StyleSpan italicSpan = new StyleSpan(Typeface.ITALIC);
        int ageLabelStart = sessionName.length();
        int ageLabelEnd = fullTitle.length();

        TermuxSessionsListViewController.applyBellNotificationLabelStyling(
            styled, ageLabelStart, ageLabelEnd, italicSpan);

        StyleSpan[] styleSpans = styled.getSpans(ageLabelStart, ageLabelEnd, StyleSpan.class);
        Assert.assertEquals(1, styleSpans.length);
        Assert.assertEquals(Typeface.ITALIC, styleSpans[0].getStyle());
    }

    @Test
    public void bellNotificationLabelStylingDoesNothingWhenStartIsNegative() {
        SpannableString styled = new SpannableString("mySession 5秒前");

        TermuxSessionsListViewController.applyBellNotificationLabelStyling(
            styled, -1, styled.length(), new StyleSpan(Typeface.ITALIC));

        Assert.assertEquals(0, styled.getSpans(0, styled.length(), RelativeSizeSpan.class).length);
        Assert.assertEquals(0, styled.getSpans(0, styled.length(), StyleSpan.class).length);
    }
}
