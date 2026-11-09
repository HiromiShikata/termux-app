package com.termux.app.voicepatrol;
import static org.junit.Assert.*;
import java.util.*;
import android.util.Log;
import com.termux.app.terminal.tts.TtsUtteranceOutcome;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowLog;
import android.content.Intent;
import android.os.Looper;
@RunWith(RobolectricTestRunner.class)
public class VoicePatrolAdaptersTest {
 static final class Bridge implements VoicePatrolActivityBridge { List<VoicePatrolCall> calls=Arrays.asList(new VoicePatrolCall("s","b")); final List<String> shown=new ArrayList<>(); boolean result=true; public List<VoicePatrolCall> callingSessionsInBellOrder(){return calls;} public void showSession(String n){shown.add(n);} public boolean sendReply(String n,String t){return result;} }
 static final class LogSink implements VoicePatrolLog { final List<String> warnings=new ArrayList<>(); public void warn(String m){warnings.add(m);} }
 @Test public void portsDelegateAndWarnOnRejectedReply(){Bridge b=new Bridge();LogSink l=new LogSink();TermuxVoicePatrolPorts p=new TermuxVoicePatrolPorts(b,l);assertEquals(b.calls,p.callingSessionsInBellOrder());p.showSession("s");assertEquals(Arrays.asList("s"),b.shown);b.result=false;assertFalse(p.sendReply("s","r"));assertEquals(1,l.warnings.size());}
 @Test public void logAdapterWritesWarning(){new AndroidLogVoicePatrolLog().warn("warning");assertEquals("warning",ShadowLog.getLatestLog().msg);assertEquals(Log.WARN,ShadowLog.getLatestLog().type);assertEquals("TermuxVoicePatrol",ShadowLog.getLatestLog().tag);}
 @Test public void ttsAdapterDelegatesSpeechAndStop(){FakeTts t=new FakeTts();TtsVoicePatrolSpeech s=new TtsVoicePatrolSpeech(t);VoicePatrolSpeechOutcome o=new VoicePatrolSpeechOutcome(){public void onSpoken(){}public void onFailed(String r){}};s.speak("x",o);s.stop();assertEquals("x",t.text);assertSame(o,t.outcome);assertTrue(t.stopped);}
 static final class FakeTts extends com.termux.app.terminal.tts.TtsManager { String text; TtsUtteranceOutcome outcome; boolean stopped; FakeTts(){super(null);} public void speak(String x,TtsUtteranceOutcome o){text=x;outcome=o;} public void stop(){stopped=true;} }
}
