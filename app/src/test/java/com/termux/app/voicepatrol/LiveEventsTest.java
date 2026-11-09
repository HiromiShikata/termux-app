package com.termux.app.voicepatrol;

import static org.junit.Assert.*;
import java.util.*;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class LiveEventsTest {
 @Test public void parsesSessionStarted() { LiveServerEvent e=LiveEvents.parseServerEvent("{\"type\":\"session.started\"}"); assertEquals(LiveServerEventKind.SESSION_STARTED,e.getKind()); }
 @Test public void parsesSessionClosed() { assertEquals(LiveServerEventKind.SESSION_CLOSED,LiveEvents.parseServerEvent("{\"type\":\"session.closed\"}").getKind()); }
 @Test public void parsesFunctionCallAndAllFields() { LiveServerEvent e=LiveEvents.parseServerEvent("{\"type\":\"response.event\",\"event\":{\"type\":\"response.output_item.done\",\"item\":{\"type\":\"function_call\",\"call_id\":\"call-1\",\"name\":\"session_skip\",\"arguments\":\"{\\\"x\\\":1}\"}}}"); assertEquals(LiveServerEventKind.FUNCTION_CALL,e.getKind()); assertEquals("call-1",e.getCallId()); assertEquals("session_skip",e.getName()); assertEquals("{\"x\":1}",e.getArguments()); }
 @Test public void classifiesOtherEvents() { assertEquals(LiveServerEventKind.OTHER,LiveEvents.parseServerEvent("{\"type\":\"response.created\"}").getKind()); }
 @Test public void rejectsInvalidJson() { try { LiveEvents.parseServerEvent("not json"); fail(); } catch (IllegalArgumentException expected) {} }
 @Test public void rejectsFunctionCallWithoutCallId() { try { LiveEvents.parseServerEvent("{\"type\":\"response.output_item.done\",\"item\":{\"type\":\"function_call\",\"name\":\"x\",\"arguments\":\"{}\"}}"); fail(); } catch (IllegalArgumentException expected) { assertTrue(expected.getMessage().contains("call_id")); } }
 @Test public void sessionThinkingAppendsPreserveContentAndDelegationNull() throws Exception { List<String> events=LiveEvents.sessionThinkingAppends("prefix","body"); assertEquals(1,events.size()); JSONObject j=new JSONObject(events.get(0)); assertEquals("session.thinking.append",j.getString("type")); assertEquals("prefix-0",j.getString("event_id")); assertTrue(j.isNull("delegation_id")); assertEquals("body",j.getString("content")); }
 @Test public void sessionThinkingAppendsSplitLongUnicodeContent() throws Exception { String content="あ".repeat(801); List<String> events=LiveEvents.sessionThinkingAppends("p",content); StringBuilder joined=new StringBuilder(); for(String raw:events){JSONObject j=new JSONObject(raw); assertTrue(j.getString("content").codePointCount(0,j.getString("content").length())<=400); joined.append(j.getString("content"));} assertEquals(content,joined.toString()); }
 @Test public void functionCallOutputCarriesEventIdCallIdAndOutput() throws Exception { JSONObject j=new JSONObject(LiveEvents.functionCallOutput("event","call","result")); assertEquals("response.item.create",j.getString("type")); assertEquals("event",j.getString("event_id")); JSONObject item=j.getJSONObject("item"); assertEquals("function_call_output",item.getString("type")); assertEquals("call",item.getString("call_id")); assertEquals("result",item.getString("output")); }
 @Test public void responseCreateAndSessionCloseCarryEventIds() throws Exception { JSONObject response=new JSONObject(LiveEvents.responseCreate("response-id")); JSONObject close=new JSONObject(LiveEvents.sessionClose("close-id")); assertEquals("response.create",response.getString("type")); assertEquals("response-id",response.getString("event_id")); assertEquals("session.close",close.getString("type")); assertEquals("close-id",close.getString("event_id")); }
}
