package com.termux.app.terminal.io;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;

import com.termux.R;
import com.termux.app.TermuxActivity;
import com.termux.app.TermuxService;
import com.termux.app.terminal.SessionNewActivityStore;
import com.termux.app.terminal.TermuxTerminalSessionActivityClient;
import com.termux.shared.termux.settings.preferences.TermuxAppSharedPreferences;
import com.termux.shared.termux.settings.properties.TermuxAppSharedProperties;
import com.termux.shared.termux.shell.TermuxShellManager;
import com.termux.terminal.TerminalEmulator;
import com.termux.terminal.TerminalSession;
import com.termux.view.TerminalView;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;

@RunWith(RobolectricTestRunner.class)
public class TerminalEnterKeyControllerTest {
    private TermuxActivity activity;
    private TerminalSession session;
    private EditText input;

    @Before
    public void setUp() throws Exception {
        activity = Robolectric.buildActivity(TermuxActivity.class).get();
        Context context = RuntimeEnvironment.getApplication();
        TermuxService service = Robolectric.buildService(TermuxService.class).get();
        set(service, TermuxService.class, "mShellManager", new TermuxShellManager(context));
        set(service, TermuxService.class, "mProperties", TermuxAppSharedProperties.init(context));
        TermuxTerminalSessionActivityClient client = new TermuxTerminalSessionActivityClient(activity);
        set(activity, TermuxActivity.class, "mTermuxService", service);
        set(activity, TermuxActivity.class, "mTermuxTerminalSessionActivityClient", client);
        set(activity, TermuxActivity.class, "mPreferences", TermuxAppSharedPreferences.build(context, true));
        set(activity, TermuxActivity.class, "mProperties", TermuxAppSharedProperties.init(context));
        service.setTermuxTerminalSessionClient(client);
        session = newSession(client);
        TerminalView terminalView = new TerminalView(context, null);
        terminalView.mTermSession = session;
        set(activity, TermuxActivity.class, "mTerminalView", terminalView);
        View row = LayoutInflater.from(activity).inflate(R.layout.view_terminal_toolbar_text_input, null);
        FrameLayout root = new FrameLayout(context);
        root.addView(row);
        activity.setContentView(root);
        input = row.findViewById(R.id.terminal_toolbar_text_input);
    }

    @Test
    public void voiceReplyPastesAndEntersWithoutChangingToolbarText() throws Exception {
        input.setText("toolbar draft");
        activity.getSessionNewActivityStore().recordExplicitCall(
            session.mSessionName, 1L, "please answer");
        TerminalEnterKeyController controller = new TerminalEnterKeyController(activity);
        assertTrue(controller.submitVoiceReply("voice reply"));
        assertEquals("toolbar draft", input.getText().toString());
        assertEquals("voice reply" + enterSequence(), readQueue());
        assertFalse(activity.getSessionNewActivityStore().hasPendingExplicitCall(session.mSessionName));
    }

    @Test
    public void emptyVoiceReplyWritesAndRecordsNothing() throws Exception {
        input.setText("toolbar draft");
        activity.getSessionNewActivityStore().recordExplicitCall(
            session.mSessionName, 1L, "please answer");
        assertFalse(new TerminalEnterKeyController(activity).submitVoiceReply(""));
        assertEquals("toolbar draft", input.getText().toString());
        assertEquals("", readQueue());
        assertTrue(activity.getSessionNewActivityStore().hasPendingExplicitCall(session.mSessionName));
    }

    @Test
    public void voiceReplyIsRejectedWhenThereIsNoCurrentSession() throws Exception {
        setUnchecked(activity, TermuxActivity.class, "mTerminalView", null);
        input.setText("toolbar draft");
        activity.getSessionNewActivityStore().recordExplicitCall(
            session.mSessionName, 1L, "please answer");
        assertFalse(new TerminalEnterKeyController(activity).submitVoiceReply("voice reply"));
        assertEquals("toolbar draft", input.getText().toString());
        assertEquals("", readQueue());
        assertTrue(activity.getSessionNewActivityStore().hasPendingExplicitCall(session.mSessionName));
    }

    @Test
    public void buttonClickSubmitsToolbarText() throws Exception {
        TerminalToolbarViewPager.PageAdapter adapter = new TerminalToolbarViewPager.PageAdapter(activity, null);
        ViewPagerHolder holder = new ViewPagerHolder(activity, adapter);
        activity.setContentView(holder.root);
        input = holder.input;
        input.setText("button reply");
        new TerminalEnterKeyController(activity, holder.button);
        holder.button.performClick();
        assertEquals("button reply" + enterSequence(), readQueue());
        assertEquals("", holder.input.getText().toString());
    }

    private String enterSequence() {
        TerminalEmulator emulator = session.getEmulator();
        return TerminalEnterKeyEncoder.enterSequence(emulator.isCursorKeysApplicationMode(),
            emulator.isKeypadApplicationMode());
    }

    private String readQueue() throws Exception {
        byte[] bytes = new byte[4096];
        Field field = TerminalSession.class.getDeclaredField("mTerminalToProcessIOQueue");
        field.setAccessible(true);
        Object queue = field.get(session);
        Method read = queue.getClass().getDeclaredMethod("read", byte[].class, boolean.class);
        read.setAccessible(true);
        int count = (Integer) read.invoke(queue, bytes, false);
        return new String(bytes, 0, count, StandardCharsets.UTF_8);
    }

    private TerminalSession newSession(TermuxTerminalSessionActivityClient client) throws Exception {
        TerminalSession result = new TerminalSession("/system/bin/sh", "/", new String[0], new String[0],
            2000, client);
        result.mSessionName = "voice-reply-session";
        try {
            result.initializeEmulator(80, 24, 10, 20);
        } catch (LinkageError error) {
            Throwable cause = error;
            while (cause.getCause() != null) cause = cause.getCause();
            assertTrue(cause.toString(), cause.toString().contains("UnsatisfiedLinkError")
                || cause.toString().contains("com.termux.terminal.JNI"));
        }
        Field pid = TerminalSession.class.getDeclaredField("mShellPid");
        pid.setAccessible(true);
        pid.setInt(result, 4545);
        assertTrue(result.isRunning());
        return result;
    }

    private static void set(Object target, Class<?> owner, String name, Object value) throws Exception {
        Field field = owner.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static void setUnchecked(Object target, Class<?> owner, String name, Object value) {
        try { set(target, owner, name, value); } catch (Exception error) { throw new AssertionError(error); }
    }

    private static final class ViewPagerHolder {
        final FrameLayout root;
        final EditText input;
        final ImageButton button;
        ViewPagerHolder(TermuxActivity activity, TerminalToolbarViewPager.PageAdapter adapter) {
            Context context = activity;
            root = new FrameLayout(context);
            View row = LayoutInflater.from(context).inflate(R.layout.view_terminal_toolbar_text_input, root, false);
            root.addView(row);
            input = row.findViewById(R.id.terminal_toolbar_text_input);
            button = new ImageButton(context);
            root.addView(button);
            androidx.viewpager.widget.ViewPager pager = new androidx.viewpager.widget.ViewPager(context);
            pager.setId(R.id.terminal_toolbar_view_pager);
            pager.setAdapter(adapter);
            root.addView(pager);
        }
    }
}
