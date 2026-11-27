package com.termux.app.ownercall;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.util.List;

/**
 * The text of the owner call a call-to-user alert is about: the same displayed body the in-app owner
 * call window shows for the newest call the owner has not answered yet.
 */
public final class OwnerCallAlertBody {

    private OwnerCallAlertBody() {
    }

    /**
     * Reads the owner call file at {@code fileUrl} and returns the displayed body of the newest call of
     * {@code hostTmuxSessionName} that is still unanswered, or {@code null} when there is none.
     *
     * @throws IOException when the file cannot be fetched, so the caller can log why the body is unknown
     */
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
        OwnerCall newestCall = unansweredCalls.get(unansweredCalls.size() - 1);
        return OwnerCallBodyDisplayText.of(newestCall.getBody()).getText();
    }
}
