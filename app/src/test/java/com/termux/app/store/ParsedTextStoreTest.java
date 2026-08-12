package com.termux.app.store;

import org.json.JSONException;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class ParsedTextStoreTest {

    private static final class RecordingTextAccess implements ParsedTextStore.TextAccess {

        private String mText;
        private final List<String> mWrites = new ArrayList<>();

        RecordingTextAccess(String text) {
            mText = text;
        }

        @Override
        public String read() {
            return mText;
        }

        @Override
        public void write(String text) {
            mText = text;
            mWrites.add(text);
        }
    }

    @Test
    public void aParseFailureLeavesTheStoredTextUntouched() {
        RecordingTextAccess access = new RecordingTextAccess("[{\"url\":");
        ParsedTextStore<String> store = new ParsedTextStore<>(access);

        try {
            store.load(text -> {
                throw new JSONException("unterminated object");
            });
            Assert.fail("the parse failure has to reach the caller instead of being turned into an empty value");
        } catch (JSONException expected) {
            Assert.assertEquals("unterminated object", expected.getMessage());
        }

        Assert.assertEquals("the stored text has to survive a parse failure", "[{\"url\":", access.mText);
        Assert.assertTrue("a parse failure must not write anything to the store", access.mWrites.isEmpty());
    }

    @Test
    public void aWriteAfterAParseFailureIsRefusedSoTheStoredTextSurvives() {
        RecordingTextAccess access = new RecordingTextAccess("[{\"url\":");
        ParsedTextStore<String> store = new ParsedTextStore<>(access);

        try {
            store.load(text -> {
                throw new JSONException("unterminated object");
            });
            Assert.fail("the parse failure has to reach the caller instead of being turned into an empty value");
        } catch (JSONException expected) {
            Assert.assertEquals("unterminated object", expected.getMessage());
        }

        Assert.assertFalse("a write has to be refused while the stored text could not be parsed", store.write("[]"));
        Assert.assertEquals("a refused write must not replace the stored text", "[{\"url\":", access.mText);
        Assert.assertTrue("a refused write must not reach the store at all", access.mWrites.isEmpty());
    }

    @Test
    public void aWriteIsAcceptedWhenTheStoredTextParsed() throws JSONException {
        RecordingTextAccess access = new RecordingTextAccess("[]");
        ParsedTextStore<String> store = new ParsedTextStore<>(access);

        Assert.assertEquals("parsed", store.load(text -> "parsed"));

        Assert.assertTrue("a write has to be accepted after the stored text parsed", store.write("[{}]"));
        Assert.assertEquals("[{}]", access.mText);
    }

    @Test
    public void aSuccessfulLoadAfterAFailedOneAcceptsWritesAgain() throws JSONException {
        RecordingTextAccess access = new RecordingTextAccess("[{\"url\":");
        ParsedTextStore<String> store = new ParsedTextStore<>(access);

        try {
            store.load(text -> {
                throw new JSONException("unterminated object");
            });
            Assert.fail("the parse failure has to reach the caller instead of being turned into an empty value");
        } catch (JSONException expected) {
            Assert.assertEquals("unterminated object", expected.getMessage());
        }

        access.mText = "[]";

        Assert.assertEquals("parsed", store.load(text -> "parsed"));
        Assert.assertTrue("a write has to be accepted once the stored text parses again", store.write("[{}]"));
        Assert.assertEquals("[{}]", access.mText);
    }

    @Test
    public void theParserReceivesTheStoredText() throws JSONException {
        RecordingTextAccess access = new RecordingTextAccess("[{\"url\":\"https://example.test/\"}]");
        ParsedTextStore<String> store = new ParsedTextStore<>(access);

        Assert.assertEquals("[{\"url\":\"https://example.test/\"}]", store.load(text -> text));
    }
}
