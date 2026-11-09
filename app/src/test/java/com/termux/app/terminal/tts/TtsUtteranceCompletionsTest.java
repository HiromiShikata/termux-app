package com.termux.app.terminal.tts;
import static org.junit.Assert.*;
import java.util.*;
import org.junit.Test;
public class TtsUtteranceCompletionsTest {
 static final class Outcome implements TtsUtteranceOutcome { int spoken; String failure; public void onSpoken(){spoken++;} public void onFailed(String r){failure=r;} }
 @Test public void eachRegisteredIdFinishesOnce(){TtsUtteranceCompletions c=new TtsUtteranceCompletions();Outcome o=new Outcome();String id=c.register(o);assertTrue(c.finishSpoken(id));assertFalse(c.finishSpoken(id));assertEquals(1,o.spoken);}
 @Test public void failedCompletionFinishesRegisteredIdOnly(){TtsUtteranceCompletions c=new TtsUtteranceCompletions();Outcome o=new Outcome();String id=c.register(o);assertFalse(c.finishFailed("missing","x"));assertTrue(c.finishFailed(id,"reason"));assertEquals("reason",o.failure);assertFalse(c.finishFailed(id,"again"));}
 @Test public void idsAreUnique(){TtsUtteranceCompletions c=new TtsUtteranceCompletions();Outcome o=new Outcome();assertNotEquals(c.register(o),c.register(o));}
}
