package com.termux.app.terminal.tts;
import static org.junit.Assert.*;
import java.util.*;
import org.junit.Test;
@org.junit.runner.RunWith(org.robolectric.RobolectricTestRunner.class)
public class TtsManagerSpeakCompletionTest {
 static final class Synth implements TtsManager.SpeechSynthesizer { Runnable ready;Runnable failed;final List<String> texts=new ArrayList<>();final List<TtsUtteranceOutcome> outcomes=new ArrayList<>();public void initialize(Runnable r){ready=r;}public void initialize(Runnable r,Runnable f){ready=r;failed=f;}public void speak(String t,int q,String id){texts.add(t);}public void speak(String t,int q,String id,TtsUtteranceOutcome o){texts.add(t);outcomes.add(o);}public void stop(){}public void shutdown(){}
 }
 @Test public void queuesUntilReadyAndReportsEngineCompletion(){Synth s=new Synth();TtsManager m=new TtsManager(s);Outcome o=new Outcome();m.speak("text",o);assertTrue(s.texts.isEmpty());s.ready.run();assertEquals(Arrays.asList("text"),s.texts);s.outcomes.get(0).onSpoken();assertEquals(1,o.spoken);}
 @Test public void engineFailureFailsPendingAndLaterRequests(){Synth s=new Synth();TtsManager m=new TtsManager(s);Outcome first=new Outcome();m.speak("first",first);s.failed.run();assertEquals("tts_engine_unavailable",first.failure);Outcome later=new Outcome();m.speak("later",later);assertEquals("tts_engine_unavailable",later.failure);assertTrue(s.texts.isEmpty());}
 @Test public void emptyTextCompletesWithoutSynthesizer(){Synth s=new Synth();TtsManager m=new TtsManager(s);s.ready.run();Outcome o=new Outcome();m.speak("",o);assertEquals(1,o.spoken);assertTrue(s.texts.isEmpty());}
 @Test public void stopClearsPendingWithoutReporting(){Synth s=new Synth();TtsManager m=new TtsManager(s);Outcome o=new Outcome();m.speak("pending",o);m.stop();s.ready.run();assertEquals(0,o.spoken);assertNull(o.failure);}
 static final class Outcome implements TtsUtteranceOutcome {int spoken;String failure;public void onSpoken(){spoken++;}public void onFailed(String r){failure=r;}}
}
