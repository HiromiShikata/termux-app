package com.termux.app.voicepatrol;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import org.junit.Test;
public class VoicePatrolEndpointUrlTest {
 @Test public void derivesEndpointForUrlTable() { String[][] c={{"https://pc.example:8443/a?k=t","https://pc.example:8443/api/live/sessions?k=t"},{"http://192.0.2.1/x","http://192.0.2.1/api/live/sessions"},{" https://pc.example/a?k=t ","https://pc.example/api/live/sessions?k=t"}}; for(String[] x:c) assertEquals(x[1],VoicePatrolEndpointUrl.resolve(x[0])); }
 @Test public void unusableUrlsAreRejectedIndividually() { for(String x:new String[]{""," ","relative","/x"}) { try { VoicePatrolEndpointUrl.resolve(x); fail(x); } catch(IllegalArgumentException expected) {} } }

 @Test public void derivesToolCallsEndpointForUrlTable() { String[][] c={{"https://pc.example:8443/a?k=t","https://pc.example:8443/api/live/tool-calls?k=t"},{"http://192.0.2.1/x","http://192.0.2.1/api/live/tool-calls"},{" https://pc.example/a?k=t ","https://pc.example/api/live/tool-calls?k=t"},{"https://pc.example/a?x=1&k=t&y=2","https://pc.example/api/live/tool-calls?k=t"}}; for(String[] x:c) assertEquals(x[1],VoicePatrolEndpointUrl.resolveToolCalls(x[0])); }
 @Test public void unusableUrlsAreRejectedForToolCallsIndividually() { for(String x:new String[]{""," ","relative","/x"}) { try { VoicePatrolEndpointUrl.resolveToolCalls(x); fail(x); } catch(IllegalArgumentException expected) {} } }
}
