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

    @Test
    public void explicitCallReasonLineReceivesARelativeSizeOfAboutOneHalfSoItStopsDominatingTheRow() {
        String sessionName = "mySession";
        String reasonLabel = "\nMANUAL CALL: needs your decision";
        String fullTitle = sessionName + reasonLabel;
        SpannableString styled = new SpannableString(fullTitle);
        int reasonStart = sessionName.length();
        int reasonEnd = fullTitle.length();

        TermuxSessionsListViewController.applyExplicitCallReasonStyling(
            styled, reasonStart, reasonEnd, new StyleSpan(Typeface.BOLD));

        RelativeSizeSpan[] sizeSpans = styled.getSpans(reasonStart, reasonEnd, RelativeSizeSpan.class);
        Assert.assertEquals(1, sizeSpans.length);
        Assert.assertEquals(0.5f, sizeSpans[0].getSizeChange(), 0.0001f);
        Assert.assertEquals(reasonStart, styled.getSpanStart(sizeSpans[0]));
        Assert.assertEquals(reasonEnd, styled.getSpanEnd(sizeSpans[0]));
    }

    @Test
    public void explicitCallReasonLineRelativeSizeIsSmallerThanTheTimestampAndBellLines() {
        String reasonLabel = "MANUAL CALL: needs your decision";
        SpannableString styled = new SpannableString(reasonLabel);

        TermuxSessionsListViewController.applyExplicitCallReasonStyling(
            styled, 0, reasonLabel.length(), new StyleSpan(Typeface.BOLD));

        RelativeSizeSpan[] sizeSpans = styled.getSpans(0, reasonLabel.length(), RelativeSizeSpan.class);
        Assert.assertEquals(1, sizeSpans.length);
        Assert.assertTrue(sizeSpans[0].getSizeChange() < 0.65f);
        Assert.assertTrue(sizeSpans[0].getSizeChange() < 0.75f);
    }

    @Test
    public void explicitCallReasonStylingKeepsTheLineBoldWhileShrinkingIt() {
        String reasonLabel = "MANUAL CALL: needs your decision";
        SpannableString styled = new SpannableString(reasonLabel);
        StyleSpan boldSpan = new StyleSpan(Typeface.BOLD);

        TermuxSessionsListViewController.applyExplicitCallReasonStyling(styled, 0, reasonLabel.length(), boldSpan);

        StyleSpan[] styleSpans = styled.getSpans(0, reasonLabel.length(), StyleSpan.class);
        Assert.assertEquals(1, styleSpans.length);
        Assert.assertEquals(Typeface.BOLD, styleSpans[0].getStyle());
    }

    @Test
    public void explicitCallReasonStylingDoesNothingWhenStartIsNegative() {
        SpannableString styled = new SpannableString("MANUAL CALL: needs your decision");

        TermuxSessionsListViewController.applyExplicitCallReasonStyling(
            styled, -1, styled.length(), new StyleSpan(Typeface.BOLD));

        Assert.assertEquals(0, styled.getSpans(0, styled.length(), RelativeSizeSpan.class).length);
        Assert.assertEquals(0, styled.getSpans(0, styled.length(), StyleSpan.class).length);
    }

    @Test
    public void sessionTitleReceivesARelativeSizeSmallerThanOneSoItFitsAsSecondaryInformation() {
        String sessionName = "mySession";
        String sessionTitle = "\nterminal title";
        String fullTitle = sessionName + sessionTitle;
        SpannableString styled = new SpannableString(fullTitle);
        int titleStart = sessionName.length();
        int titleEnd = fullTitle.length();

        TermuxSessionsListViewController.applySessionTitleStyling(
            styled, titleStart, titleEnd, new StyleSpan(Typeface.ITALIC));

        RelativeSizeSpan[] sizeSpans = styled.getSpans(titleStart, titleEnd, RelativeSizeSpan.class);
        Assert.assertEquals(1, sizeSpans.length);
        Assert.assertEquals(0.75f, sizeSpans[0].getSizeChange(), 0.0001f);
        Assert.assertTrue(sizeSpans[0].getSizeChange() < 1.0f);
        Assert.assertEquals(titleStart, styled.getSpanStart(sizeSpans[0]));
        Assert.assertEquals(titleEnd, styled.getSpanEnd(sizeSpans[0]));
    }

    @Test
    public void sessionTitleStylingAppliesToItalicSpanOnTheTitleRange() {
        String sessionName = "mySession";
        String sessionTitle = "\nterminal title";
        String fullTitle = sessionName + sessionTitle;
        SpannableString styled = new SpannableString(fullTitle);
        StyleSpan italicSpan = new StyleSpan(Typeface.ITALIC);
        int titleStart = sessionName.length();
        int titleEnd = fullTitle.length();

        TermuxSessionsListViewController.applySessionTitleStyling(
            styled, titleStart, titleEnd, italicSpan);

        StyleSpan[] styleSpans = styled.getSpans(titleStart, titleEnd, StyleSpan.class);
        Assert.assertEquals(1, styleSpans.length);
        Assert.assertEquals(Typeface.ITALIC, styleSpans[0].getStyle());
    }

    @Test
    public void sessionTitleStylingDoesNothingWhenStartIsNegative() {
        SpannableString styled = new SpannableString("mySession\nterminal title");

        TermuxSessionsListViewController.applySessionTitleStyling(
            styled, -1, styled.length(), new StyleSpan(Typeface.ITALIC));

        Assert.assertEquals(0, styled.getSpans(0, styled.length(), RelativeSizeSpan.class).length);
        Assert.assertEquals(0, styled.getSpans(0, styled.length(), StyleSpan.class).length);
    }

    @Test
    public void bellNotificationLabelKeepsItalicWhenSessionTitleIsAlsoPresent() {
        String sessionName = "mySession";
        String bellLabel = " 🔔 5秒前";
        String sessionTitle = "\nterminal title";
        String fullText = sessionName + bellLabel + sessionTitle;
        SpannableString styled = new SpannableString(fullText);
        int bellStart = sessionName.length();
        int bellEnd = bellStart + bellLabel.length();
        int titleStart = bellEnd;
        int titleEnd = fullText.length();

        TermuxSessionsListViewController.applyBellNotificationLabelStyling(
            styled, bellStart, bellEnd, new StyleSpan(Typeface.ITALIC));
        TermuxSessionsListViewController.applySessionTitleStyling(
            styled, titleStart, titleEnd, new StyleSpan(Typeface.ITALIC));

        StyleSpan[] bellSpans = styled.getSpans(bellStart, bellEnd, StyleSpan.class);
        Assert.assertEquals(1, bellSpans.length);
        Assert.assertEquals(Typeface.ITALIC, bellSpans[0].getStyle());
        StyleSpan[] titleSpans = styled.getSpans(titleStart, titleEnd, StyleSpan.class);
        Assert.assertEquals(1, titleSpans.length);
        Assert.assertEquals(Typeface.ITALIC, titleSpans[0].getStyle());
    }
}
