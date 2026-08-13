package com.termux.app.ownercall;

import android.content.Context;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.termux.R;
import com.termux.app.sessiondefinition.UnansweredOwnerCall;

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
    private static final UnansweredOwnerCall EARLIER_CALL =
        new UnansweredOwnerCall("2027-01-15T08:00:00.000Z", "同じ本文の呼び出し");
    private static final UnansweredOwnerCall LATER_CALL =
        new UnansweredOwnerCall("2027-01-15T08:04:00.000Z", "同じ本文の呼び出し");
    private static final UnansweredOwnerCall OTHER_SESSION_CALL =
        new UnansweredOwnerCall("2027-01-15T08:02:00.000Z", "別セッションの呼び出し");
    private static final UnansweredOwnerCall OTHER_SESSION_SECOND_CALL =
        new UnansweredOwnerCall("2027-01-15T08:06:00.000Z", "別セッションの二件目の呼び出し");
    private static final UnansweredOwnerCall OTHER_SESSION_THIRD_CALL =
        new UnansweredOwnerCall("2027-01-15T08:08:00.000Z", "別セッションの三件目の呼び出し");
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
    public void closingOneCallLeavesTheOtherCallThatCarriesTheSameBody() {
        View root = inflateActivityLayout();
        OwnerCallDialogController controller = controllerFor(root);
        controller.showCallsForSession(FIRST_SESSION, NOW);

        root.findViewById(R.id.owner_call_dialog_close_button).performClick();

        Assert.assertEquals(View.VISIBLE, dialog(root).getVisibility());
        Assert.assertEquals("1 / 1", positionText(root));
        Assert.assertEquals(LATER_CALL.getBody(), bodyText(root));
    }

    @Test
    public void hidesTheDialogOnceEveryWaitingCallIsClosed() {
        View root = inflateActivityLayout();
        OwnerCallDialogController controller = controllerFor(root);
        controller.showCallsForSession(FIRST_SESSION, NOW);

        root.findViewById(R.id.owner_call_dialog_close_button).performClick();
        root.findViewById(R.id.owner_call_dialog_close_button).performClick();

        Assert.assertEquals(View.GONE, dialog(root).getVisibility());
    }

    @Test
    public void keepsAClosedCallClosedWhenTheSameSessionIsShownAgain() {
        View root = inflateActivityLayout();
        OwnerCallDialogController controller = controllerFor(root);
        controller.showCallsForSession(FIRST_SESSION, NOW);
        root.findViewById(R.id.owner_call_dialog_close_button).performClick();

        controller.showCallsForSession(SECOND_SESSION, NOW);
        controller.showCallsForSession(FIRST_SESSION, NOW);

        Assert.assertEquals("1 / 1", positionText(root));
        Assert.assertEquals(LATER_CALL.getBody(), bodyText(root));
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
    public void closingACallOfOneSessionLeavesTheCallsOfTheOtherSessionAlone() {
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
        Map<String, List<UnansweredOwnerCall>> callsBySession = new HashMap<>();
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
        Map<String, List<UnansweredOwnerCall>> callsBySession = new HashMap<>();
        callsBySession.put(FIRST_SESSION, Arrays.asList(EARLIER_CALL, LATER_CALL));
        callsBySession.put(SECOND_SESSION, Arrays.asList(OTHER_SESSION_CALL,
            OTHER_SESSION_SECOND_CALL, OTHER_SESSION_THIRD_CALL));
        return controllerFor(root, callsBySession);
    }

    private static OwnerCallDialogController controllerFor(View root,
                                                           Map<String, List<UnansweredOwnerCall>> callsBySession) {
        return new OwnerCallDialogController(root,
            sessionName -> {
                List<UnansweredOwnerCall> calls = callsBySession.get(sessionName);
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
