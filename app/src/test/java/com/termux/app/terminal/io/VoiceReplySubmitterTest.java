package com.termux.app.terminal.io;
import static org.junit.Assert.*;
import java.util.*;
import org.junit.Test;
public class VoiceReplySubmitterTest {
 static final class Terminal implements VoiceReplySubmitter.ReplyTerminal { boolean accepts=true; final List<String> calls=new ArrayList<>(); public boolean acceptsOwnerInput(){return accepts;} public void pasteOwnerContent(String t){calls.add("paste:"+t);} public void sendEnterAfterOwnerContent(){calls.add("enter");} public void recordGenuineReply(){calls.add("record");} }
 @Test public void submitsExactReplyInPasteEnterRecordOrder(){Terminal t=new Terminal();assertTrue(VoiceReplySubmitter.submit(t,"reply"));assertEquals(Arrays.asList("paste:reply","enter","record"),t.calls);}
 @Test public void rejectedTerminalWritesNothing(){Terminal t=new Terminal();t.accepts=false;assertFalse(VoiceReplySubmitter.submit(t,"reply"));assertTrue(t.calls.isEmpty());}
 @Test public void emptyReplyWritesNothing(){Terminal t=new Terminal();assertFalse(VoiceReplySubmitter.submit(t,""));assertTrue(t.calls.isEmpty());}
}
