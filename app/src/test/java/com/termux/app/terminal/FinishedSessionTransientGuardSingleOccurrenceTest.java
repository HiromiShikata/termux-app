package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FinishedSessionTransientGuardSingleOccurrenceTest {

    private static final String CLIENT_RELATIVE_PATH =
        "src/main/java/com/termux/app/terminal/TermuxTerminalSessionActivityClient.java";

    private static final String TRANSIENT_GUARD =
        "if (TransientCommandSessionName.isTransient(finishedSession.mSessionName)) {";

    @Test
    public void theFinishedSessionTransientGuardAppearsExactlyOnceSoNoRebaseCanLeaveADeadSecondCopyBehind()
            throws IOException {
        String source = readClientSource();
        int occurrences = 0;
        for (int index = source.indexOf(TRANSIENT_GUARD); index >= 0;
                index = source.indexOf(TRANSIENT_GUARD, index + TRANSIENT_GUARD.length())) {
            occurrences++;
        }

        Assert.assertEquals("a second copy of this guard is unreachable dead code that no behavioural test "
                + "can observe, so its absence is asserted here on the source itself", 1, occurrences);
    }

    private String readClientSource() throws IOException {
        Path moduleRelative = Paths.get(CLIENT_RELATIVE_PATH);
        if (Files.exists(moduleRelative)) {
            return new String(Files.readAllBytes(moduleRelative), StandardCharsets.UTF_8);
        }
        return new String(Files.readAllBytes(Paths.get("app").resolve(CLIENT_RELATIVE_PATH)),
            StandardCharsets.UTF_8);
    }
}
