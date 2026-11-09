package com.termux.app.voicepatrol;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class LiveSessionAnswerTest {

    private static final String ANSWER_SDP = "v=0\r\no=- 1 2 IN IP4 127.0.0.1\r\n";

    @Test
    public void successfulStatusWithSdpBodyIsAcceptedAsRemoteAnswer() {
        int[] successStatuses = {200, 201};
        for (int status : successStatuses) {
            LiveSessionAnswer answer = LiveSessionAnswer.fromHttpResponse(status, ANSWER_SDP);
            assertTrue("status " + status, answer.isAccepted());
            assertEquals(ANSWER_SDP, answer.getSdp());
        }
    }

    @Test
    public void successfulStatusWithBlankBodyIsAFailureNamedAsEmptyAnswer() {
        LiveSessionAnswer answer = LiveSessionAnswer.fromHttpResponse(201, "   ");

        assertFalse(answer.isAccepted());
        assertEquals("empty_sdp_answer", answer.getFailureReason());
    }

    @Test
    public void apiKeyMissingReturnsTheServerErrorCode() {
        LiveSessionAnswer answer = LiveSessionAnswer.fromHttpResponse(503, "{\"error\":\"openai_api_key_missing\"}");

        assertFalse(answer.isAccepted());
        assertEquals("openai_api_key_missing", answer.getFailureReason());
    }

    @Test
    public void liveSessionFailureReturnsTheServerErrorCode() {
        LiveSessionAnswer answer = LiveSessionAnswer.fromHttpResponse(502,
            "{\"error\":\"openai_live_session_failed\",\"status\":500,\"message\":\"upstream\"}");

        assertFalse(answer.isAccepted());
        assertEquals("openai_live_session_failed", answer.getFailureReason());
    }

    @Test
    public void badRequestAndTokenRejectionWithoutJsonFallBackToTheHttpStatus() {
        LiveSessionAnswer badRequest = LiveSessionAnswer.fromHttpResponse(400, "An SDP offer is required");
        LiveSessionAnswer rejectedToken = LiveSessionAnswer.fromHttpResponse(401, "");

        assertEquals("HTTP 400", badRequest.getFailureReason());
        assertEquals("HTTP 401", rejectedToken.getFailureReason());
    }

    @Test
    public void errorBodyWithoutErrorFieldFallsBackToTheHttpStatus() {
        LiveSessionAnswer answer = LiveSessionAnswer.fromHttpResponse(502, "{\"message\":\"x\"}");

        assertEquals("HTTP 502", answer.getFailureReason());
    }
}
