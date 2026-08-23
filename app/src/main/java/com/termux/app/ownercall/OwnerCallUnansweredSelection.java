package com.termux.app.ownercall;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The owner calls of one session that the owner has not answered yet, taken from everything that
 * session's owner call file still holds.
 *
 * <p>The file is append-only on the host and is emptied only when the app deletes it, so a session
 * the owner has been called on repeatedly keeps every past call in the file. Showing that whole file
 * puts calls the owner answered hours ago in front of the one call that is actually waiting. A call
 * placed at or before the session's genuine reply time was answered by that reply, so only the calls
 * after it are still waiting.
 *
 * <p>A call whose {@code calledAt} cannot be read carries no time to compare, so it is kept: an
 * unreadable timestamp is not evidence that the owner answered it. A reply time later than now is
 * likewise not evidence of an answer — the owner cannot have replied in the future, so it is a
 * corrupted time and every call is kept rather than the whole dialog being emptied by it.
 */
public final class OwnerCallUnansweredSelection {

    private OwnerCallUnansweredSelection() {
    }

    @NonNull
    public static List<OwnerCall> of(@NonNull List<OwnerCall> calls,
                                     @Nullable Long answeredThroughMillis,
                                     long nowMillis) {
        if (answeredThroughMillis == null || answeredThroughMillis > nowMillis) {
            return calls;
        }
        List<OwnerCall> unanswered = new ArrayList<>();
        for (OwnerCall call : calls) {
            Long calledAtMillis = OwnerCallCalledAt.toEpochMillis(call.getCalledAt());
            if (calledAtMillis == null || calledAtMillis > answeredThroughMillis) {
                unanswered.add(call);
            }
        }
        return Collections.unmodifiableList(unanswered);
    }
}
