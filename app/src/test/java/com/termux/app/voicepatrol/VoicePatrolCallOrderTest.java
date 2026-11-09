package com.termux.app.voicepatrol;

import static org.junit.Assert.assertEquals;
import com.termux.app.terminal.CallingSessionNavigator;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import org.junit.Test;

public class VoicePatrolCallOrderTest {
    @Test public void bellOrderMatchesNavigatorForSeveralInputs() {
        List<Object[]> cases = Arrays.asList(
            new Object[]{Arrays.asList(3, 0, 2, 1), Arrays.asList("a", "b", "c", "d"), new HashSet<>(Arrays.asList("a", "c", "d")), Arrays.asList(3, 0, 2)},
            new Object[]{Arrays.asList(2, 1, 0), Arrays.asList("x", "y", "z"), new HashSet<>(Arrays.asList("x", "z")), Arrays.asList(2, 0)},
            new Object[]{Arrays.asList(0, 1), Arrays.asList("a", "b"), new HashSet<>(Arrays.asList("b")), Arrays.asList(1)});
        for (Object[] c : cases) {
            List<Integer> actual = VoicePatrolCallOrder.callingSessionIndexesInBellOrder((List<Integer>) c[0], (List<String>) c[1], (java.util.Set<String>) c[2]);
            assertEquals(c[3], actual);
            assertEquals(CallingSessionNavigator.topmostCallingSessionIndex((List<Integer>) c[0], (List<String>) c[1], (java.util.Set<String>) c[2]), (int) actual.get(0));
        }
    }
    @Test public void noCallingSessionsProducesEmptyOrder() { assertEquals(Arrays.asList(), VoicePatrolCallOrder.callingSessionIndexesInBellOrder(Arrays.asList(0), Arrays.asList("a"), new HashSet<>())); }
}
