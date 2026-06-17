package com.termux.shared.logger;

import android.content.Context;
import android.os.Build;
import android.view.Gravity;
import android.widget.Toast;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = Build.VERSION_CODES.P)
public class LoggerTest {

    private int savedLogLevel;
    private String savedDefaultLogTag;

    private Context context() {
        return RuntimeEnvironment.getApplication();
    }

    @Before
    public void saveState() {
        savedLogLevel = Logger.getLogLevel();
        savedDefaultLogTag = Logger.getDefaultLogTag();
    }

    @After
    public void restoreState() {
        Logger.setLogLevel(null, savedLogLevel);
        Logger.setDefaultLogTag(savedDefaultLogTag);
    }

    @Test
    public void defaultLogLevelIsNormal() {
        Logger.setLogLevel(null, Logger.DEFAULT_LOG_LEVEL);
        Assert.assertEquals(Logger.LOG_LEVEL_NORMAL, Logger.getLogLevel());
    }

    @Test
    public void setLogLevelWithValidValueUpdatesCurrentLevel() {
        Assert.assertEquals(Logger.LOG_LEVEL_VERBOSE, Logger.setLogLevel(null, Logger.LOG_LEVEL_VERBOSE));
        Assert.assertEquals(Logger.LOG_LEVEL_VERBOSE, Logger.getLogLevel());
    }

    @Test
    public void setLogLevelWithInvalidValueFallsBackToDefault() {
        Assert.assertEquals(Logger.DEFAULT_LOG_LEVEL, Logger.setLogLevel(null, 99));
        Assert.assertEquals(Logger.DEFAULT_LOG_LEVEL, Logger.getLogLevel());
    }

    @Test
    public void setLogLevelWithContextShowsToastAndUpdatesLevel() {
        Assert.assertEquals(Logger.LOG_LEVEL_DEBUG, Logger.setLogLevel(context(), Logger.LOG_LEVEL_DEBUG));
        Assert.assertEquals(Logger.LOG_LEVEL_DEBUG, Logger.getLogLevel());
    }

    @Test
    public void isLogLevelValidChecksRange() {
        Assert.assertFalse(Logger.isLogLevelValid(null));
        Assert.assertFalse(Logger.isLogLevelValid(-1));
        Assert.assertTrue(Logger.isLogLevelValid(Logger.LOG_LEVEL_OFF));
        Assert.assertTrue(Logger.isLogLevelValid(Logger.LOG_LEVEL_NORMAL));
        Assert.assertTrue(Logger.isLogLevelValid(Logger.LOG_LEVEL_DEBUG));
        Assert.assertTrue(Logger.isLogLevelValid(Logger.LOG_LEVEL_VERBOSE));
        Assert.assertFalse(Logger.isLogLevelValid(Logger.MAX_LOG_LEVEL + 1));
    }

    @Test
    public void defaultLogTagIsLogger() {
        Logger.setDefaultLogTag("Logger");
        Assert.assertEquals("Logger", Logger.getDefaultLogTag());
    }

    @Test
    public void setDefaultLogTagKeepsShortTagUnchanged() {
        Logger.setDefaultLogTag("ShortTag");
        Assert.assertEquals("ShortTag", Logger.getDefaultLogTag());
    }

    @Test
    public void setDefaultLogTagTruncatesLongTagTo22Chars() {
        String longTag = "ThisLogTagIsDefinitelyLongerThanLimit";
        Logger.setDefaultLogTag(longTag);
        Assert.assertEquals(longTag.substring(0, 22), Logger.getDefaultLogTag());
        Assert.assertEquals(22, Logger.getDefaultLogTag().length());
    }

    @Test
    public void getFullTagReturnsTagWhenEqualToDefault() {
        Logger.setDefaultLogTag("Logger");
        Assert.assertEquals("Logger", Logger.getFullTag("Logger"));
    }

    @Test
    public void getFullTagPrefixesWithDefaultTag() {
        Logger.setDefaultLogTag("Logger");
        Assert.assertEquals("Logger.Child", Logger.getFullTag("Child"));
    }

    @Test
    public void getLogLevelsArrayContainsAllLevels() {
        CharSequence[] levels = Logger.getLogLevelsArray();
        Assert.assertEquals(4, levels.length);
        Assert.assertEquals(String.valueOf(Logger.LOG_LEVEL_OFF), levels[0].toString());
        Assert.assertEquals(String.valueOf(Logger.LOG_LEVEL_VERBOSE), levels[3].toString());
    }

    @Test
    public void getLogLevelLabelsArrayReturnsLabelForEachLevel() {
        CharSequence[] levels = Logger.getLogLevelsArray();
        CharSequence[] labels = Logger.getLogLevelLabelsArray(context(), levels, true);
        Assert.assertNotNull(labels);
        Assert.assertEquals(levels.length, labels.length);
        for (CharSequence label : labels)
            Assert.assertNotNull(label);
    }

    @Test
    public void getLogLevelLabelsArrayReturnsNullForNullLevels() {
        Assert.assertNull(Logger.getLogLevelLabelsArray(context(), null, false));
    }

    @Test
    public void getLogLevelLabelAddsDefaultSuffixForDefaultLevel() {
        String label = Logger.getLogLevelLabel(context(), Logger.DEFAULT_LOG_LEVEL, true);
        Assert.assertTrue(label.endsWith("(default)"));
    }

    @Test
    public void getLogLevelLabelOmitsDefaultSuffixWhenNotRequested() {
        String label = Logger.getLogLevelLabel(context(), Logger.DEFAULT_LOG_LEVEL, false);
        Assert.assertFalse(label.endsWith("(default)"));
    }

    @Test
    public void getLogLevelLabelForUnknownLevelReturnsNonNull() {
        Assert.assertNotNull(Logger.getLogLevelLabel(context(), 999, false));
    }

    @Test
    public void shouldEnableLoggingForCustomLogLevelReturnsFalseWhenLoggingOff() {
        Logger.setLogLevel(null, Logger.LOG_LEVEL_OFF);
        Assert.assertFalse(Logger.shouldEnableLoggingForCustomLogLevel(Logger.LOG_LEVEL_VERBOSE));
    }

    @Test
    public void shouldEnableLoggingForCustomLogLevelUsesAppLevelWhenCustomNull() {
        Logger.setLogLevel(null, Logger.LOG_LEVEL_VERBOSE);
        Assert.assertTrue(Logger.shouldEnableLoggingForCustomLogLevel(null));
        Logger.setLogLevel(null, Logger.LOG_LEVEL_NORMAL);
        Assert.assertFalse(Logger.shouldEnableLoggingForCustomLogLevel(null));
    }

    @Test
    public void shouldEnableLoggingForCustomLogLevelReturnsFalseForOffCustomLevel() {
        Logger.setLogLevel(null, Logger.LOG_LEVEL_VERBOSE);
        Assert.assertFalse(Logger.shouldEnableLoggingForCustomLogLevel(Logger.LOG_LEVEL_OFF));
    }

    @Test
    public void shouldEnableLoggingForCustomLogLevelComparesAgainstCurrentLevel() {
        Logger.setLogLevel(null, Logger.LOG_LEVEL_NORMAL);
        Assert.assertTrue(Logger.shouldEnableLoggingForCustomLogLevel(Logger.LOG_LEVEL_DEBUG));
        Logger.setLogLevel(null, Logger.LOG_LEVEL_VERBOSE);
        Assert.assertFalse(Logger.shouldEnableLoggingForCustomLogLevel(Logger.LOG_LEVEL_DEBUG));
    }

    @Test
    public void shouldEnableLoggingForCustomLogLevelClampsInvalidCustomLevelToVerbose() {
        Logger.setLogLevel(null, Logger.LOG_LEVEL_VERBOSE);
        Assert.assertTrue(Logger.shouldEnableLoggingForCustomLogLevel(99));
    }

    @Test
    public void getStackTraceStringReturnsNullForNullThrowable() {
        Assert.assertNull(Logger.getStackTraceString(null));
    }

    @Test
    public void getStackTraceStringContainsExceptionDetails() {
        String trace = Logger.getStackTraceString(new IllegalStateException("boom"));
        Assert.assertNotNull(trace);
        Assert.assertTrue(trace.contains("IllegalStateException"));
        Assert.assertTrue(trace.contains("boom"));
    }

    @Test
    public void getStackTracesStringArrayReturnsNullForNullList() {
        Assert.assertNull(Logger.getStackTracesStringArray((List<Throwable>) null));
    }

    @Test
    public void getStackTracesStringArrayForSingleThrowable() {
        String[] traces = Logger.getStackTracesStringArray(new RuntimeException("single"));
        Assert.assertEquals(1, traces.length);
        Assert.assertTrue(traces[0].contains("single"));
    }

    @Test
    public void getStackTracesStringArrayForThrowableList() {
        List<Throwable> throwables = Arrays.asList(new RuntimeException("a"), new IllegalArgumentException("b"));
        String[] traces = Logger.getStackTracesStringArray(throwables);
        Assert.assertEquals(2, traces.length);
        Assert.assertTrue(traces[0].contains("a"));
        Assert.assertTrue(traces[1].contains("b"));
    }

    @Test
    public void getStackTracesStringUsesDefaultLabelAndDashForEmptyArray() {
        String result = Logger.getStackTracesString(null, new String[0]);
        Assert.assertTrue(result.startsWith("StackTraces:"));
        Assert.assertTrue(result.contains(" -"));
    }

    @Test
    public void getStackTracesStringForSingleEntryHasNoStacktraceHeading() {
        String result = Logger.getStackTracesString("Label:", new String[]{"trace-content"});
        Assert.assertTrue(result.startsWith("Label:"));
        Assert.assertTrue(result.contains("trace-content"));
        Assert.assertFalse(result.contains("Stacktrace 1"));
    }

    @Test
    public void getStackTracesStringForMultipleEntriesAddsHeadings() {
        String result = Logger.getStackTracesString("Label:", new String[]{"one", "two"});
        Assert.assertTrue(result.contains("Stacktrace 1"));
        Assert.assertTrue(result.contains("Stacktrace 2"));
    }

    @Test
    public void getStackTracesMarkdownStringUsesDefaultLabelAndDashForNullArray() {
        String result = Logger.getStackTracesMarkdownString(null, null);
        Assert.assertTrue(result.startsWith("### StackTraces"));
        Assert.assertTrue(result.contains("`-`"));
    }

    @Test
    public void getStackTracesMarkdownStringForMultipleEntriesAddsHeadings() {
        String result = Logger.getStackTracesMarkdownString("Errors", new String[]{"one", "two"});
        Assert.assertTrue(result.contains("### Errors"));
        Assert.assertTrue(result.contains("#### Stacktrace 1"));
        Assert.assertTrue(result.contains("#### Stacktrace 2"));
    }

    @Test
    public void getMessageAndStackTraceStringReturnsNullWhenBothNull() {
        Assert.assertNull(Logger.getMessageAndStackTraceString(null, null));
    }

    @Test
    public void getMessageAndStackTraceStringReturnsMessageWhenThrowableNull() {
        Assert.assertEquals("only message", Logger.getMessageAndStackTraceString("only message", null));
    }

    @Test
    public void getMessageAndStackTraceStringReturnsTraceWhenMessageNull() {
        String result = Logger.getMessageAndStackTraceString(null, new RuntimeException("trace-only"));
        Assert.assertNotNull(result);
        Assert.assertTrue(result.contains("trace-only"));
    }

    @Test
    public void getMessageAndStackTraceStringCombinesMessageAndTrace() {
        String result = Logger.getMessageAndStackTraceString("msg", new RuntimeException("trace"));
        Assert.assertTrue(result.startsWith("msg:\n"));
        Assert.assertTrue(result.contains("trace"));
    }

    @Test
    public void getMessageAndStackTracesStringReturnsNullWhenBothEmpty() {
        Assert.assertNull(Logger.getMessageAndStackTracesString(null, new ArrayList<>()));
        Assert.assertNull(Logger.getMessageAndStackTracesString(null, null));
    }

    @Test
    public void getMessageAndStackTracesStringReturnsMessageWhenListEmpty() {
        Assert.assertEquals("msg", Logger.getMessageAndStackTracesString("msg", Collections.emptyList()));
    }

    @Test
    public void getMessageAndStackTracesStringReturnsTracesWhenMessageNull() {
        String result = Logger.getMessageAndStackTracesString(null, Arrays.asList(new RuntimeException("only-trace")));
        Assert.assertNotNull(result);
        Assert.assertTrue(result.contains("only-trace"));
    }

    @Test
    public void getMessageAndStackTracesStringCombinesMessageAndTraces() {
        String result = Logger.getMessageAndStackTracesString("header", Arrays.asList(new RuntimeException("trace-x")));
        Assert.assertTrue(result.startsWith("header:\n"));
        Assert.assertTrue(result.contains("trace-x"));
    }

    @Test
    public void getSingleLineLogStringEntryWrapsNonNullObject() {
        Assert.assertEquals("Key: `value`", Logger.getSingleLineLogStringEntry("Key", "value", "-"));
    }

    @Test
    public void getSingleLineLogStringEntryUsesDefaultForNullObject() {
        Assert.assertEquals("Key: -", Logger.getSingleLineLogStringEntry("Key", null, "-"));
    }

    @Test
    public void getMultiLineLogStringEntryWrapsNonNullObject() {
        String result = Logger.getMultiLineLogStringEntry("Key", "value", "-");
        Assert.assertTrue(result.startsWith("Key:\n```\n"));
        Assert.assertTrue(result.contains("value"));
    }

    @Test
    public void getMultiLineLogStringEntryUsesDefaultForNullObject() {
        Assert.assertEquals("Key: -", Logger.getMultiLineLogStringEntry("Key", null, "-"));
    }

    @Test
    public void logExtendedMessageReturnsWithoutErrorForNullMessage() {
        Logger.setLogLevel(null, Logger.LOG_LEVEL_VERBOSE);
        Logger.logExtendedMessage(android.util.Log.ERROR, "Tag", null);
    }

    @Test
    public void logExtendedMessageSplitsLargeMessage() {
        Logger.setLogLevel(null, Logger.LOG_LEVEL_VERBOSE);
        StringBuilder largeMessage = new StringBuilder();
        for (int i = 0; i < 600; i++)
            largeMessage.append("line-").append(i).append("\n");
        Logger.logExtendedMessage(android.util.Log.ERROR, "Tag", largeMessage.toString());
    }

    @Test
    public void logMessageHandlesAllPrioritiesAtVerboseLevel() {
        Logger.setLogLevel(null, Logger.LOG_LEVEL_VERBOSE);
        Logger.logError("Tag", "error message");
        Logger.logWarn("Tag", "warn message");
        Logger.logInfo("Tag", "info message");
        Logger.logDebug("Tag", "debug message");
        Logger.logVerbose("Tag", "verbose message");
    }

    @Test
    public void logHelpersWithoutTagDoNotCrash() {
        Logger.setLogLevel(null, Logger.LOG_LEVEL_VERBOSE);
        Logger.logError("error");
        Logger.logErrorExtended("error extended");
        Logger.logWarn("warn");
        Logger.logWarnExtended("warn extended");
        Logger.logInfo("info");
        Logger.logInfoExtended("info extended");
        Logger.logDebug("debug");
        Logger.logDebugExtended("debug extended");
        Logger.logVerbose("verbose");
        Logger.logVerboseExtended("verbose extended");
        Logger.logVerboseForce("Tag", "forced");
    }

    @Test
    public void logPrivateHelpersRunAtDebugLevel() {
        Logger.setLogLevel(null, Logger.LOG_LEVEL_DEBUG);
        Logger.logErrorPrivate("Tag", "private error");
        Logger.logErrorPrivate("private error");
        Logger.logErrorPrivateExtended("Tag", "private error extended");
        Logger.logErrorPrivateExtended("private error extended");
    }

    @Test
    public void logStackTraceHelpersDoNotCrash() {
        Logger.setLogLevel(null, Logger.LOG_LEVEL_VERBOSE);
        RuntimeException throwable = new RuntimeException("trace");
        Logger.logStackTraceWithMessage("Tag", "message", throwable);
        Logger.logStackTraceWithMessage("message", throwable);
        Logger.logStackTrace("Tag", throwable);
        Logger.logStackTrace(throwable);
        Logger.logStackTracesWithMessage("Tag", "message", Arrays.asList(throwable));
    }

    @Test
    public void logAndShowToastHelpersDoNotCrash() {
        Logger.setLogLevel(null, Logger.LOG_LEVEL_VERBOSE);
        Logger.logInfoAndShowToast(context(), "Tag", "info toast");
        Logger.logInfoAndShowToast(context(), "info toast");
        Logger.logErrorAndShowToast(context(), "Tag", "error toast");
        Logger.logErrorAndShowToast(context(), "error toast");
        Logger.logDebugAndShowToast(context(), "Tag", "debug toast");
        Logger.logDebugAndShowToast(context(), "debug toast");
    }

    @Test
    public void showToastIgnoresNullContext() {
        Logger.showToast(null, "text", true);
    }

    @Test
    public void showToastIgnoresEmptyText() {
        Logger.showToast(context(), "", true);
    }

    @Test
    public void createTopGravityToastUsesTopGravity() {
        Toast toast = Logger.createTopGravityToast(context(), "hello", true);
        Assert.assertNotNull(toast);
        Assert.assertEquals(Gravity.TOP, toast.getGravity());
    }
}
