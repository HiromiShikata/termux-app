package com.termux.app.voicepatrol;
import static org.junit.Assert.*;
import org.junit.Test;
public class VoicePatrolCallTest { @Test public void storesSessionNameAndBody(){VoicePatrolCall c=new VoicePatrolCall("session","body");assertEquals("session",c.getSessionName());assertEquals("body",c.getBody());} }
