package com.termux.app.voicepatrol;

import static org.junit.Assert.*;
import org.junit.Test;

public class LiveSessionAnswerTest {
 @Test public void acceptedAnswerExposesStatusBodyAndSdp() { LiveSessionAnswer a=LiveSessionAnswer.fromHttpResponse(200,"answer-sdp"); assertTrue(a.isAccepted()); assertEquals("answer-sdp",a.getSdp()); assertEquals("",a.getFailureReason()); }
 @Test public void emptyAnswerIsRejected() { LiveSessionAnswer a=LiveSessionAnswer.fromHttpResponse(200," "); assertFalse(a.isAccepted()); assertEquals("empty_sdp_answer",a.getFailureReason()); assertEquals("",a.getSdp()); }
 @Test public void apiKeyMissingIsReportedFor503() { assertEquals("openai_api_key_missing",LiveSessionAnswer.fromHttpResponse(503,"{\"error\":\"openai_api_key_missing\"}").getFailureReason()); }
 @Test public void liveSessionFailureIsReportedFor502() { assertEquals("openai_live_session_failed",LiveSessionAnswer.fromHttpResponse(502,"{\"error\":\"openai_live_session_failed\"}").getFailureReason()); }
 @Test public void badRequestIsReportedFor400() { assertEquals("HTTP 400",LiveSessionAnswer.fromHttpResponse(400,"bad request").getFailureReason()); }
 @Test public void tokenRejectionIsReportedFor401() { assertEquals("HTTP 401",LiveSessionAnswer.fromHttpResponse(401,"unauthorized").getFailureReason()); }
 @Test public void failedFactoryExposesReasonAndRejectedState() { LiveSessionAnswer a=LiveSessionAnswer.failed("network_error"); assertFalse(a.isAccepted()); assertEquals("network_error",a.getFailureReason()); assertEquals("",a.getSdp()); }
 @Test public void httpResponseExposesStatusAndBody() { LiveSessionHttpResponse r=new LiveSessionHttpResponse(503,"body"); assertEquals(503,r.getStatusCode()); assertEquals("body",r.getBody()); }
}
