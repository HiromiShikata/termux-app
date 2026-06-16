package com.termux.shared.shell;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class ArgumentTokenizerTest {

    @Test
    public void splitsPlainArgumentsOnWhitespace() {
        Assert.assertEquals(Arrays.asList("ls", "-la", "/home"),
            ArgumentTokenizer.tokenize("ls -la /home"));
    }

    @Test
    public void collapsesRepeatedWhitespaceBetweenArguments() {
        Assert.assertEquals(Arrays.asList("a", "b", "c"),
            ArgumentTokenizer.tokenize("a   b\t\tc"));
    }

    @Test
    public void returnsEmptyListForEmptyString() {
        Assert.assertTrue(ArgumentTokenizer.tokenize("").isEmpty());
    }

    @Test
    public void returnsEmptyListForWhitespaceOnlyString() {
        Assert.assertTrue(ArgumentTokenizer.tokenize("   \t  ").isEmpty());
    }

    @Test
    public void treatsSingleQuotedSpanAsOneArgument() {
        Assert.assertEquals(Arrays.asList("echo", "hello world"),
            ArgumentTokenizer.tokenize("echo 'hello world'"));
    }

    @Test
    public void treatsDoubleQuotedSpanAsOneArgument() {
        Assert.assertEquals(Arrays.asList("echo", "hello world"),
            ArgumentTokenizer.tokenize("echo \"hello world\""));
    }

    @Test
    public void keepsSingleQuotesLiteralInsideDoubleQuotes() {
        Assert.assertEquals(Arrays.asList("it's here"),
            ArgumentTokenizer.tokenize("\"it's here\""));
    }

    @Test
    public void escapesNextCharacterWithBackslash() {
        Assert.assertEquals(Arrays.asList("a b"),
            ArgumentTokenizer.tokenize("a\\ b"));
    }

    @Test
    public void escapedBackslashInDoubleQuotesYieldsSingleBackslash() {
        Assert.assertEquals(Arrays.asList("a\\b"),
            ArgumentTokenizer.tokenize("\"a\\\\b\""));
    }

    @Test
    public void escapedDoubleQuoteInDoubleQuotesIsLiteral() {
        Assert.assertEquals(Arrays.asList("say \"hi\""),
            ArgumentTokenizer.tokenize("\"say \\\"hi\\\"\""));
    }

    @Test
    public void unrecognizedDoubleQuoteEscapeKeepsBackslashAndCharacter() {
        Assert.assertEquals(Arrays.asList("a\\nb"),
            ArgumentTokenizer.tokenize("\"a\\nb\""));
    }

    @Test
    public void trailingBackslashIsPreservedAsLiteralBackslash() {
        Assert.assertEquals(Arrays.asList("a\\"),
            ArgumentTokenizer.tokenize("a\\"));
    }

    @Test
    public void adjacentQuotedAndUnquotedFormSingleArgument() {
        Assert.assertEquals(Arrays.asList("foobar"),
            ArgumentTokenizer.tokenize("foo'bar'"));
    }

    @Test
    public void stringifyWrapsEachArgumentInQuotes() {
        List<String> result = ArgumentTokenizer.tokenize("a b c", true);
        Assert.assertEquals(Arrays.asList("\"a\"", "\"b\"", "\"c\""), result);
    }

    @Test
    public void stringifyEscapesNewlineInsideQuotedArgument() {
        List<String> result = ArgumentTokenizer.tokenize("'line1\nline2'", true);
        Assert.assertEquals(Arrays.asList("\"line1\\nline2\""), result);
    }

    @Test
    public void stringifyEscapesBackslashAndQuoteCharacters() {
        List<String> result = ArgumentTokenizer.tokenize("\"a\\\\\\\"b\"", true);
        Assert.assertEquals(Arrays.asList("\"a\\\\\\\"b\""), result);
    }
}
