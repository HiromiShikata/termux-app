package com.termux.app.voicepatrol;

import static org.junit.Assert.assertEquals;

import com.termux.app.terminal.CallingSessionNavigator;

import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class VoicePatrolCallOrderTest {

    @Test
    public void patrolOrderEqualsBellOrderAndStartsAtTheBellTarget() {
        List<Integer> ordered = Arrays.asList(3, 0, 2, 1);
        List<String> names = Arrays.asList("alpha", "beta", "gamma", "delta");
        Set<String> calling = new HashSet<>(Arrays.asList("alpha", "gamma", "delta"));

        List<Integer> patrol = VoicePatrolCallOrder.callingSessionIndexesInBellOrder(ordered, names, calling);

        assertEquals(Arrays.asList(3, 0, 2), patrol);
        assertEquals(CallingSessionNavigator.topmostCallingSessionIndex(ordered, names, calling),
            (int) patrol.get(0));
    }

    @Test
    public void emptyCallingSetYieldsNoPatrolTargets() {
        assertEquals(0, VoicePatrolCallOrder.callingSessionIndexesInBellOrder(
            Arrays.asList(0, 1), Arrays.asList("a", "b"), new HashSet<String>()).size());
    }
}
