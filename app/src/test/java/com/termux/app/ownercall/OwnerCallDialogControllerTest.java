package com.termux.app.ownercall;

import android.content.Context;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.termux.R;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RunWith(RobolectricTestRunner.class)
public class OwnerCallDialogControllerTest {

    private static final String FIRST_SESSION = "https://github.com/owner/repo/issues/1";
    private static final String SECOND_SESSION = "https://github.com/owner/repo/issues/2";
    private static final String REPEATED_BODY =
        "Decide whether the invoice recipient may be changed.";
    private static final OwnerCall EARLIER_CALL =
        new OwnerCall(FIRST_SESSION, "2026-08-14T08:00:00Z", REPEATED_BODY);
    private static final OwnerCall LATER_CALL =
        new OwnerCall(FIRST_SESSION, "2026-08-14T08:04:00Z", REPEATED_BODY);
    private static final OwnerCall OTHER_SESSION_CALL = new OwnerCall(SECOND_SESSION,
        "2026-08-14T08:02:00Z", "Decide whether the branch may be deleted.");
    private static final OwnerCall OTHER_SESSION_SECOND_CALL = new OwnerCall(SECOND_SESSION,
        "2026-08-14T08:06:00Z", "Decide which release note wording to publish.");
    private static final OwnerCall OTHER_SESSION_THIRD_CALL = new OwnerCall(SECOND_SESSION,
        "2026-08-14T08:08:00Z", "Decide whether the migration may run tonight.");
    private static final long NOW = 1_800_000_000_000L;
    private static final OwnerCallDialogGeometry GEOMETRY =
        OwnerCallDialogGeometry.resolve(2400, 0, 1080, 120, 36);

    @Test
    public void showsTheWaitingCallsOfTheSessionOnScreen() {
        View root = inflateActivityLayout();
        OwnerCallDialogController controller = controllerFor(root);

        controller.showCallsForSession(FIRST_SESSION, NOW);

        Assert.assertEquals(View.VISIBLE, dialog(root).getVisibility());
        Assert.assertEquals("1 / 2", positionText(root));
        Assert.assertEquals(EARLIER_CALL.getBody(), bodyText(root));
    }

    @Test
    public void showsNoDialogForASessionWithNoWaitingCall() {
        View root = inflateActivityLayout();
        OwnerCallDialogController controller = controllerFor(root);

        controller.showCallsForSession("https://github.com/owner/repo/issues/3", NOW);

        Assert.assertEquals(View.GONE, dialog(root).getVisibility());
    }

    @Test
    public void pagesToTheNextWaitingCallAndBack() {
        View root = inflateActivityLayout();
        OwnerCallDialogController controller = controllerFor(root);
        controller.showCallsForSession(FIRST_SESSION, NOW);

        root.findViewById(R.id.owner_call_dialog_next_button).performClick();
        Assert.assertEquals("2 / 2", positionText(root));

        root.findViewById(R.id.owner_call_dialog_previous_button).performClick();
        Assert.assertEquals("1 / 2", positionText(root));
    }

    @Test
    public void closingTheDialogHidesTheWholeDialogNotJustOneCall() {
        View root = inflateActivityLayout();
        OwnerCallDialogController controller = controllerFor(root);
        controller.showCallsForSession(FIRST_SESSION, NOW);

        root.findViewById(R.id.owner_call_dialog_close_button).performClick();

        Assert.assertEquals(View.GONE, dialog(root).getVisibility());
    }

    @Test
    public void reopeningAfterCloseShowsAllCallsUnchanged() {
        View root = inflateActivityLayout();
        OwnerCallDialogController controller = controllerFor(root);
        controller.showCallsForSession(FIRST_SESSION, NOW);
        root.findViewById(R.id.owner_call_dialog_close_button).performClick();

        controller.reopenDialog(NOW);

        Assert.assertEquals(View.VISIBLE, dialog(root).getVisibility());
        Assert.assertEquals("1 / 2", positionText(root));
        Assert.assertEquals(EARLIER_CALL.getBody(), bodyText(root));
    }

    @Test
    public void switchingToAnotherSessionAndReturningShowsAllCallsAgainAfterClose() {
        View root = inflateActivityLayout();
        OwnerCallDialogController controller = controllerFor(root);
        controller.showCallsForSession(FIRST_SESSION, NOW);
        root.findViewById(R.id.owner_call_dialog_close_button).performClick();

        controller.showCallsForSession(SECOND_SESSION, NOW);
        controller.showCallsForSession(FIRST_SESSION, NOW);

        Assert.assertEquals(View.VISIBLE, dialog(root).getVisibility());
        Assert.assertEquals("1 / 2", positionText(root));
        Assert.assertEquals(EARLIER_CALL.getBody(), bodyText(root));
    }

    @Test
    public void callListIsUnchangedAfterCloseAndReopen() {
        View root = inflateActivityLayout();
        OwnerCallDialogController controller = controllerFor(root);
        controller.showCallsForSession(FIRST_SESSION, NOW);
        String positionBeforeClose = positionText(root);
        String bodyBeforeClose = bodyText(root);

        root.findViewById(R.id.owner_call_dialog_close_button).performClick();
        controller.reopenDialog(NOW);

        Assert.assertEquals(positionBeforeClose, positionText(root));
        Assert.assertEquals(bodyBeforeClose, bodyText(root));
    }

    @Test
    public void switchesToTheWaitingCallOfTheNewlyDisplayedSession() {
        View root = inflateActivityLayout();
        OwnerCallDialogController controller = controllerFor(root);
        controller.showCallsForSession(FIRST_SESSION, NOW);
        root.findViewById(R.id.owner_call_dialog_next_button).performClick();

        controller.showCallsForSession(SECOND_SESSION, NOW);

        Assert.assertEquals("1 / 3", positionText(root));
        Assert.assertEquals(OTHER_SESSION_CALL.getBody(), bodyText(root));
    }

    @Test
    public void closingForOneSessionDoesNotAffectTheCallsOfAnotherSession() {
        View root = inflateActivityLayout();
        OwnerCallDialogController controller = controllerFor(root);
        controller.showCallsForSession(FIRST_SESSION, NOW);
        root.findViewById(R.id.owner_call_dialog_close_button).performClick();

        controller.showCallsForSession(SECOND_SESSION, NOW);

        Assert.assertEquals("1 / 3", positionText(root));
    }

    @Test
    public void keepsShowingTheCallTheOwnerIsReadingWhenAnEarlierCallIsAnswered() {
        View root = inflateActivityLayout();
        Map<String, List<OwnerCall>> callsBySession = new HashMap<>();
        callsBySession.put(FIRST_SESSION, Arrays.asList(EARLIER_CALL, LATER_CALL));
        OwnerCallDialogController controller = controllerFor(root, callsBySession);
        controller.showCallsForSession(FIRST_SESSION, NOW);
        root.findViewById(R.id.owner_call_dialog_next_button).performClick();
        Assert.assertEquals("2 / 2", positionText(root));

        callsBySession.put(FIRST_SESSION, Collections.singletonList(LATER_CALL));
        controller.showCallsForSession(FIRST_SESSION, NOW);

        Assert.assertEquals("1 / 1", positionText(root));
        Assert.assertEquals(LATER_CALL.getBody(), bodyText(root));
    }

    private static OwnerCallDialogController controllerFor(View root) {
        Map<String, List<OwnerCall>> callsBySession = new HashMap<>();
        callsBySession.put(FIRST_SESSION, Arrays.asList(EARLIER_CALL, LATER_CALL));
        callsBySession.put(SECOND_SESSION, Arrays.asList(OTHER_SESSION_CALL,
            OTHER_SESSION_SECOND_CALL, OTHER_SESSION_THIRD_CALL));
        return controllerFor(root, callsBySession);
    }

    private static OwnerCallDialogController controllerFor(View root,
                                                           Map<String, List<OwnerCall>> callsBySession) {
        return new OwnerCallDialogController(root,
            sessionName -> {
                List<OwnerCall> calls = callsBySession.get(sessionName);
                return calls == null ? Collections.emptyList() : calls;
            },
            () -> GEOMETRY);
    }

    private static View dialog(View root) {
        return root.findViewById(R.id.owner_call_dialog);
    }

    private static String positionText(View root) {
        return ((TextView) root.findViewById(R.id.owner_call_dialog_position)).getText().toString();
    }

    private static String bodyText(View root) {
        return ((TextView) root.findViewById(R.id.owner_call_dialog_body)).getText().toString();
    }

    private static View inflateActivityLayout() {
        Context context = new ContextThemeWrapper(RuntimeEnvironment.getApplication(),
            R.style.Theme_TermuxActivity_DayNight_NoActionBar);
        return LayoutInflater.from(context)
            .inflate(R.layout.activity_termux, new FrameLayout(context), false);
    }
}
