package com.termux.app;

import com.termux.shared.termux.data.TermuxUrlUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import com.termux.app.terminal.TermuxSessionsListViewController;
import com.termux.shared.termux.shell.command.runner.terminal.TermuxSession;
import com.termux.terminal.TerminalSession;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Arrays;
import java.lang.reflect.Field;
import java.lang.reflect.Constructor;

@RunWith(RobolectricTestRunner.class)
public class TermuxActivityTest {

    private void assertUrlsAre(String text, String... urls) {
        LinkedHashSet<String> expected = new LinkedHashSet<>();
        Collections.addAll(expected, urls);
        Assert.assertEquals(expected, TermuxUrlUtils.extractUrls(text));
    }

    @Test
    public void testExtractUrls() {
        assertUrlsAre("hello http://example.com world", "http://example.com");

        assertUrlsAre("http://example.com\nhttp://another.com", "http://example.com", "http://another.com");

        assertUrlsAre("hello http://example.com world and http://more.example.com with secure https://more.example.com",
            "http://example.com", "http://more.example.com", "https://more.example.com");

        assertUrlsAre("hello https://example.com/#bar https://example.com/foo#bar",
            "https://example.com/#bar", "https://example.com/foo#bar");
    }

    @Test
    public void voicePatrolTerminalSessionMethodsExposeRunningSessionNamesAndReplySubmission() throws Exception {
        TermuxActivity activity = org.robolectric.Robolectric.buildActivity(TermuxActivity.class).get();
        TerminalSession alphaTerminal = new TerminalSession("sh", "/", new String[0], new String[0], 24, null);
        alphaTerminal.mSessionName = "alpha";
        TerminalSession betaTerminal = new TerminalSession("sh", "/", new String[0], new String[0], 24, null);
        betaTerminal.mSessionName = "beta";
        Constructor<TermuxSession> constructor = TermuxSession.class.getDeclaredConstructor(
            TerminalSession.class, com.termux.shared.shell.command.ExecutionCommand.class,
            TermuxSession.TermuxSessionClient.class, boolean.class);
        constructor.setAccessible(true);
        TermuxSessionsListViewController sessions = new TermuxSessionsListViewController(activity,
            Arrays.asList(constructor.newInstance(alphaTerminal, null, null, false),
                constructor.newInstance(betaTerminal, null, null, false)));
        Field field = TermuxActivity.class.getDeclaredField("mTermuxSessionListViewController");
        field.setAccessible(true);
        field.set(activity, sessions);
        assertEquals(Arrays.asList("alpha", "beta"), activity.sessionNames());
        assertFalse(activity.submitVoiceReplyToCurrentSession("reply"));
    }

}
