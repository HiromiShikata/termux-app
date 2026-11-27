package com.termux.app.ownercall;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.termux.app.terminal.HostTmuxSessionName;
import com.termux.shared.logger.Logger;

import java.io.IOException;
import java.util.List;

public final class OwnerCallAlertBody {

    private static final String LOG_TAG = "OwnerCallAlertBody";

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

    @Nullable
    public static String readOrNull(@NonNull OwnerCallFileTransport transport, @NonNull String sessionName,
                                    @NonNull String fileUrl, @Nullable Long answeredThroughMillis, long nowMillis) {
        try {
            return resolve(transport, fileUrl, HostTmuxSessionName.normalize(sessionName),
                answeredThroughMillis, nowMillis);
        } catch (Exception unreadableOwnerCallFile) {
            Logger.logStackTraceWithMessage(LOG_TAG, "The call alert body of " + sessionName
                + " is not known yet: the owner call file at " + OwnerCallFileUrl.describe(fileUrl)
                + " could not be read", unreadableOwnerCallFile);
            return null;
        }
    }
}
