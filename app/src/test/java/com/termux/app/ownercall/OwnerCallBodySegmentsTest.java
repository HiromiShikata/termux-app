package com.termux.app.ownercall;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class OwnerCallBodySegmentsTest {

    private static final class Case {
        private final String mName;
        private final String mBody;
        private final List<String> mExpected;

        Case(String name, String body, String... expected) {
            mName = name;
            mBody = body;
            mExpected = Arrays.asList(expected);
        }
    }

    @Test
    public void splitsTheDisplayedBodyIntoTextAndTableSegmentsAsGithubFlavoredMarkdownDefines() {
        Case[] cases = {
            new Case("a body without any pipe stays one text segment",
                "Hello there.\nSecond line.",
                "TEXT(Hello there.\nSecond line.)"),
            new Case("an empty body is one empty text segment",
                "",
                "TEXT()"),
            new Case("a pipe line without a delimiter row stays plain text",
                "compare a | b\nwith nothing",
                "TEXT(compare a | b\nwith nothing)"),
            new Case("a header followed by a non-delimiter row stays plain text",
                "| a | b |\n| c | d |",
                "TEXT(| a | b |\n| c | d |)"),
            new Case("a header and delimiter row make a table with its body rows",
                "| Fruit | Price |\n|---|---|\n| Apple | 100 |\n| Peach | 250 |",
                "TABLE align=[LEFT,LEFT] header=[Fruit][Price] row=[Apple][100] row=[Peach][250]"),
            new Case("colons in the delimiter row set left, center and right alignment",
                "| a | b | c |\n|:---|:---:|---:|\n| 1 | 2 | 3 |",
                "TABLE align=[LEFT,CENTER,RIGHT] header=[a][b][c] row=[1][2][3]"),
            new Case("a delimiter row without outer pipes still forms a table",
                "a | b\n--- | ---\n1 | 2",
                "TABLE align=[LEFT,LEFT] header=[a][b] row=[1][2]"),
            new Case("a delimiter row with a different column count is not a table",
                "| a | b |\n|---|\n| 1 | 2 |",
                "TEXT(| a | b |\n|---|\n| 1 | 2 |)"),
            new Case("a delimiter cell that is not dashes is not a table",
                "| a | b |\n| x | y |\n| 1 | 2 |",
                "TEXT(| a | b |\n| x | y |\n| 1 | 2 |)"),
            new Case("text before and after a table becomes separate text segments",
                "Intro line\n| a |\n|---|\n| 1 |\nOutro line",
                "TEXT(Intro line)",
                "TABLE align=[LEFT] header=[a] row=[1]",
                "TEXT(Outro line)"),
            new Case("a blank line ends the table rows and the text after it stays text",
                "| a |\n|---|\n| 1 |\n\nafter the table",
                "TABLE align=[LEFT] header=[a] row=[1]",
                "TEXT(after the table)"),
            new Case("a short body row is padded with empty cells",
                "| a | b |\n|---|---|\n| 1 |",
                "TABLE align=[LEFT,LEFT] header=[a][b] row=[1][]"),
            new Case("an escaped pipe inside a cell does not split the cell",
                "| a\\|b | c |\n|---|---|\n| 1 | 2 |",
                "TABLE align=[LEFT,LEFT] header=[a\\|b][c] row=[1][2]"),
            new Case("two tables separated by text are both segmented",
                "| a |\n|---|\n| 1 |\nbetween\n| b |\n|---|\n| 2 |",
                "TABLE align=[LEFT] header=[a] row=[1]",
                "TEXT(between)",
                "TABLE align=[LEFT] header=[b] row=[2]"),
        };
        for (Case testCase : cases) {
            OwnerCallBodyDisplayText displayText = OwnerCallBodyDisplayText.of(testCase.mBody);
            Assert.assertEquals(testCase.mName, testCase.mExpected,
                describe(displayText.getText(), OwnerCallBodySegments.of(displayText)));
        }
    }

    @Test
    public void cellRangesPointAtTheDisplayedTextSoCopyAndUrlTapsKeepWorking() {
        OwnerCallBodyDisplayText displayText = OwnerCallBodyDisplayText.of(
            "🔴\n\n| name | link |\n|---|---|\n| <copy>gh pr view</copy> | "
                + "https://github.com/HiromiShikata/termux-app/pull/1925 |");

        List<OwnerCallBodySegment> segments = OwnerCallBodySegments.of(displayText);

        Assert.assertEquals(1, segments.size());
        OwnerCallBodyTable table = segments.get(0).getTable();
        Assert.assertNotNull(table);
        OwnerCallBodyRange copyCell = table.getRows().get(0).get(0);
        Assert.assertEquals("gh pr view", copyCell.getText());
        Assert.assertEquals(copyCell.getText(),
            displayText.getText().substring(copyCell.getStartIndex(), copyCell.getEndIndex()));
        OwnerCallBodyRange urlCell = table.getRows().get(0).get(1);
        Assert.assertEquals("https://github.com/HiromiShikata/termux-app/pull/1925",
            urlCell.getText());
        Assert.assertEquals(urlCell.getText(),
            displayText.getText().substring(urlCell.getStartIndex(), urlCell.getEndIndex()));
    }

    @Test
    public void segmentRangesCoverTheDisplayedTextWithoutOverlap() {
        OwnerCallBodyDisplayText displayText = OwnerCallBodyDisplayText.of(
            "Intro\n| a |\n|---|\n| 1 |\nOutro");

        List<OwnerCallBodySegment> segments = OwnerCallBodySegments.of(displayText);

        Assert.assertEquals("Intro", displayText.getText().substring(
            segments.get(0).getStartIndex(), segments.get(0).getEndIndex()));
        Assert.assertEquals("| a |\n|---|\n| 1 |", displayText.getText().substring(
            segments.get(1).getStartIndex(), segments.get(1).getEndIndex()));
        Assert.assertEquals("Outro", displayText.getText().substring(
            segments.get(2).getStartIndex(), segments.get(2).getEndIndex()));
    }

    @Test
    public void readsQuotedLinesWithoutTheirMarkersSoTablesWrittenInsideQuotesAreTables() {
        Case[] cases = {
            new Case("a quoted header, delimiter and body rows make a table",
                "> | h1 | h2 |\n> |---|---|\n> | a | b |",
                "TABLE align=[LEFT,LEFT] header=[h1][h2] row=[a][b]"),
            new Case("a quoted line before a table keeps the table",
                "> note\n| a |\n|---|\n| 1 |",
                "TEXT(note)",
                "TABLE align=[LEFT] header=[a] row=[1]"),
            new Case("a quoted line after a table is not absorbed as a table row",
                "| a |\n|---|\n| 1 |\n> after",
                "TABLE align=[LEFT] header=[a] row=[1]",
                "TEXT(after)"),
            new Case("a quoted pipe line right after a table is not a table row",
                "| a | b |\n|---|---|\n| 1 | 2 |\n> | 3 | 4 |",
                "TABLE align=[LEFT,LEFT] header=[a][b] row=[1][2]",
                "TEXT(| 3 | 4 |)"),
            new Case("a quoted delimiter row under a plain header does not make a table",
                "| a | b |\n> |---|---|\n| 1 | 2 |",
                "TEXT(| a | b |)",
                "TEXT(|---|---|)",
                "TEXT(| 1 | 2 |)"),
            new Case("quoted lines around a table keep their order",
                "> one\n> two\n| a |\n|---|\n| 1 |\n> three",
                "TEXT(one\ntwo)",
                "TABLE align=[LEFT] header=[a] row=[1]",
                "TEXT(three)"),
            new Case("a greater-than sign inside a line is not a quote",
                "a > b\n| x |\n|---|",
                "TEXT(a > b)",
                "TABLE align=[LEFT] header=[x]"),
        };
        for (Case testCase : cases) {
            OwnerCallBodyDisplayText displayText = OwnerCallBodyDisplayText.of(testCase.mBody);
            Assert.assertEquals(testCase.mName, testCase.mExpected,
                describe(displayText.getText(), OwnerCallBodySegments.of(displayText)));
        }
    }

    @Test
    public void cellRangesInsideAQuotedTablePointAtTheDisplayedTextToo() {
        OwnerCallBodyDisplayText displayText = OwnerCallBodyDisplayText.of(
            "> | name | link |\n> |---|---|\n> | <copy>gh pr view</copy> | "
                + "https://github.com/HiromiShikata/termux-app/pull/1925 |");

        List<OwnerCallBodySegment> segments = OwnerCallBodySegments.of(displayText);

        Assert.assertEquals(1, segments.size());
        OwnerCallBodyTable table = segments.get(0).getTable();
        Assert.assertNotNull(table);
        OwnerCallBodyRange copyCell = table.getRows().get(0).get(0);
        Assert.assertEquals("gh pr view", copyCell.getText());
        Assert.assertEquals(copyCell.getText(),
            displayText.getText().substring(copyCell.getStartIndex(), copyCell.getEndIndex()));
        OwnerCallBodyRange urlCell = table.getRows().get(0).get(1);
        Assert.assertEquals(urlCell.getText(),
            displayText.getText().substring(urlCell.getStartIndex(), urlCell.getEndIndex()));
    }

    private static List<String> describe(String displayed, List<OwnerCallBodySegment> segments) {
        List<String> described = new ArrayList<>();
        for (OwnerCallBodySegment segment : segments) {
            if (!segment.isTable()) {
                described.add("TEXT(" + textOf(displayed, segment) + ")");
                continue;
            }
            OwnerCallBodyTable table = segment.getTable();
            List<String> alignments = new ArrayList<>();
            for (OwnerCallBodyTableAlignment alignment : table.getAlignments()) {
                alignments.add(alignment.name());
            }
            StringBuilder line = new StringBuilder("TABLE align=[")
                .append(String.join(",", alignments)).append("] header=")
                .append(cells(table.getHeaderCells()));
            for (List<OwnerCallBodyRange> row : table.getRows()) {
                line.append(" row=").append(cells(row));
            }
            described.add(line.toString());
        }
        return described;
    }

    private static String textOf(String displayed, OwnerCallBodySegment segment) {
        return displayed.substring(segment.getStartIndex(), segment.getEndIndex());
    }

    private static String cells(List<OwnerCallBodyRange> cells) {
        StringBuilder builder = new StringBuilder();
        for (OwnerCallBodyRange cell : cells) {
            builder.append('[').append(cell.getText()).append(']');
        }
        return builder.toString();
    }
}
