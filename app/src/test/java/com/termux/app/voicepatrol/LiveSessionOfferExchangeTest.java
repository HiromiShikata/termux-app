package com.termux.app.voicepatrol;
import static org.junit.Assert.*;
import java.io.IOException;
import org.junit.Test;
@org.junit.runner.RunWith(org.robolectric.RobolectricTestRunner.class)
public class LiveSessionOfferExchangeTest {
 static final class Client implements LiveSessionHttpClient { String endpoint, offer; LiveSessionHttpResponse response; IOException failure; public LiveSessionHttpResponse post(String e,String o)throws IOException {endpoint=e;offer=o;if(failure!=null)throw failure;return response;} }
 @Test public void delegatesOfferUnchangedAndMapsAnswer(){Client c=new Client();c.response=new LiveSessionHttpResponse(200,"answer");LiveSessionAnswer a=new LiveSessionOfferExchange(c).exchange("endpoint","offer");assertTrue(a.isAccepted());assertEquals("endpoint",c.endpoint);assertEquals("offer",c.offer);assertEquals("answer",a.getSdp());}
 @Test public void ioExceptionBecomesNetworkError(){Client c=new Client();c.failure=new IOException();assertEquals("network_error",new LiveSessionOfferExchange(c).exchange("e","o").getFailureReason());}
 @Test public void serverFailuresKeepServerReasons(){String[][] x={{"503","{\"error\":\"openai_api_key_missing\"}","openai_api_key_missing"},{"502","{\"error\":\"openai_live_session_failed\"}","openai_live_session_failed"},{"400","bad","HTTP 400"},{"401","","HTTP 401"}};for(String[] r:x){Client c=new Client();c.response=new LiveSessionHttpResponse(Integer.parseInt(r[0]),r[1]);assertEquals(r[2],new LiveSessionOfferExchange(c).exchange("e","o").getFailureReason());}}
}
