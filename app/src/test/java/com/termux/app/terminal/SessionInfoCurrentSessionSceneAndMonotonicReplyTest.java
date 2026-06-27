package com.termux.app.terminal;

import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.termux.R;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class SessionInfoCurrentSessionSceneAndMonotonicReplyTest {

    private static final long ONE_SECOND_MILLIS = 1000L;
    private static final long ONE_MINUTE_MILLIS = 60L * ONE_SECOND_MILLIS;
    private static final long NOW = 1_000_000_000L;
    private static final String CURRENT_SESSION = "current-session";
    private static final String OTHER_SESSION = "other-session";

    @Test
    public void currentSessionAreaShowsTheCurrentSessionSceneNotAnotherSessionsPendingReason() {
        View root = inflateActivityLayout();
        View sceneBar = root.findViewById(R.id.session_pending_call_to_user_bar);
        TextView sceneText = root.findViewById(R.id.session_pending_call_to_user_text);

        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordExplicitCall(OTHER_SESSION, NOW, "other session needs approval");
        store.recordExplicitCall(CURRENT_SESSION, NOW, "current session needs approval");

        SessionInfoBottomBarsBinder.bind(root, store, CURRENT_SESSION, NOW, () -> {
        });

        Assert.assertEquals(View.VISIBLE, sceneBar.getVisibility());
        Assert.assertEquals("the current-session area scene must be the current session's reason, "
                + "never another session's pending reason",
            "current session needs approval", sceneText.getText().toString());
    }

    @Test
    public void currentSessionAreaSceneIsHiddenWhenOnlyAnotherSessionHasAPendingReason() {
        View root = inflateActivityLayout();
        View sceneBar = root.findViewById(R.id.session_pending_call_to_user_bar);
        TextView sceneText = root.findViewById(R.id.session_pending_call_to_user_text);

        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordExplicitCall(OTHER_SESSION, NOW, "other session needs approval");

        SessionInfoBottomBarsBinder.bind(root, store, CURRENT_SESSION, NOW, () -> {
        });

        Assert.assertEquals("when only a different session has a pending reason, the current-session "
                + "area must show no scene", View.GONE, sceneBar.getVisibility());
        Assert.assertEquals("", sceneText.getText().toString());
    }

    @Test
    public void olderAppInputDoesNotLowerTheStoredUserInputTime() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordUserInput(CURRENT_SESSION, NOW);

        store.recordUserInput(CURRENT_SESSION, NOW - 51L * ONE_SECOND_MILLIS);

        Assert.assertEquals(Long.valueOf(NOW), store.getLastUserInputTimeMillis(CURRENT_SESSION));
    }

    @Test
    public void newerAppInputAdvancesTheStoredUserInputTime() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordUserInput(CURRENT_SESSION, NOW - 51L * ONE_SECOND_MILLIS);

        store.recordUserInput(CURRENT_SESSION, NOW);

        Assert.assertEquals(Long.valueOf(NOW), store.getLastUserInputTimeMillis(CURRENT_SESSION));
    }

    @Test
    public void recordStatuslineTimesDoesNotPullTheAppInputTimeBackwards() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordUserInput(CURRENT_SESSION, NOW);

        store.recordStatuslineTimes(CURRENT_SESSION, null, null, NOW - 51L * ONE_SECOND_MILLIS);

        Assert.assertEquals("recordStatuslineTimes must never write the statusline reply into the "
                + "app-captured input time", Long.valueOf(NOW),
            store.getLastUserInputTimeMillis(CURRENT_SESSION));
    }

    @Test
    public void olderStatuslineReplyDoesNotLowerTheEffectiveReplyTime() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordUserInput(CURRENT_SESSION, NOW);

        store.recordStatuslineTimes(CURRENT_SESSION, null, null, NOW - 51L * ONE_SECOND_MILLIS);

        Assert.assertEquals("an older statusline reply must not lower the effective reply time",
            Long.valueOf(NOW), store.effectiveReplyTimeMillis(CURRENT_SESSION));
    }

    @Test
    public void aNewerStatuslineReplyAdvancesTheEffectiveReplyTime() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordUserInput(CURRENT_SESSION, NOW - 51L * ONE_SECOND_MILLIS);

        store.recordStatuslineTimes(CURRENT_SESSION, null, null, NOW);

        Assert.assertEquals("a newer reply must advance the effective reply time",
            Long.valueOf(NOW), store.effectiveReplyTimeMillis(CURRENT_SESSION));
    }

    @Test
    public void anOlderStatuslineReplyDoesNotLowerAPreviousStatuslineReply() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordStatuslineTimes(CURRENT_SESSION, null, null, NOW);

        store.recordStatuslineTimes(CURRENT_SESSION, null, null, NOW - 2L * ONE_MINUTE_MILLIS);

        Assert.assertEquals("an older statusline reply must not lower the stored statusline reply",
            Long.valueOf(NOW), store.getStatuslineReplyTimeMillis(CURRENT_SESSION));
    }

    private static View inflateActivityLayout() {
        ContextThemeWrapper themedContext = new ContextThemeWrapper(
            RuntimeEnvironment.getApplication(), R.style.Theme_TermuxActivity_DayNight_NoActionBar);
        return LayoutInflater.from(themedContext).inflate(R.layout.activity_termux, null);
    }
}
