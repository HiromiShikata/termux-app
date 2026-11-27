package com.termux.app.ownercall;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.util.List;

public final class OwnerCallAlertBody {

    private OwnerCallAlertBody() {
    }

    @Nullable
    public static String resolve(@NonNull OwnerCallFileTransport transport, @NonNull String fileUrl,
                                 @NonNull String hostTmuxSessionName, @Nullable Long answeredThroughMillis,
                                 long nowMillis) throws IOException {
        List<OwnerCall> sessionCalls = OwnerCallInbox.callsOfSession(
            new OwnerCallDocumentParser().parse(transport.fetch(fileUrl)), hostTmuxSessionName);
        List<OwnerCall> unansweredCalls =
            OwnerCallUnansweredSelection.of(sessionCalls, answeredThroughMillis, nowMillis);
        if (unansweredCalls.isEmpty()) {
            return null;
        }
        OwnerCall oldestUnansweredCall = unansweredCalls.get(0);
        return OwnerCallBodyDisplayText.of(oldestUnansweredCall.getBody()).getText();
    }
}
