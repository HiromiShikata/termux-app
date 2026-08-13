package com.termux.app.terminal;

import static org.junit.Assert.assertEquals;

import com.termux.app.outputtag.OutputTagOccurrence;

import java.util.List;

import org.junit.Test;

public class BodylessCallToUserTagDetectionTest {

    @Test
    public void detectsATagThatCarriesNoBodyAsACall() {
        CallToUserTagScanner scanner = new CallToUserTagScanner();

        List<OutputTagOccurrence> calls =
            scanner.newCalls("prompt <call-to-user></call-to-user> prompt");

        assertEquals(1, calls.size());
    }

    @Test
    public void detectsATagWhoseBodyIsOnlyWhitespaceAsACall() {
        CallToUserTagScanner scanner = new CallToUserTagScanner();

        List<OutputTagOccurrence> calls =
            scanner.newCalls("prompt <call-to-user>\n   \n</call-to-user> prompt");

        assertEquals(1, calls.size());
    }

    @Test
    public void doesNotReFireABodylessTagThatIsStillVisibleOnTheNextScan() {
        CallToUserTagScanner scanner = new CallToUserTagScanner();
        String output = "prompt <call-to-user></call-to-user> prompt";

        assertEquals(1, scanner.newCalls(output).size());
        assertEquals(0, scanner.newCalls(output).size());
    }
}
