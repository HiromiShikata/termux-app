package com.termux.app.terminal;

import com.termux.app.apkupdate.UpdateTagUpdateController;

/**
 * Feeds a session's terminal transcript to the output tags that MUST be detected for every session,
 * regardless of which session is currently viewed or whether the app is in the foreground: the
 * explicit-call tag (so a non-current or backgrounded session that calls the user records its red
 * dot without the owner having to open it) and the app-update tag.
 *
 * <p>The open-URL tag is intentionally NOT scanned here: auto-opening a URL is a foreground action
 * scoped to the session the user is actively viewing, so it stays handled by the activity client for
 * the current session only.
 *
 * <p>This is a thin policy seam shared by both the activity client and the service client so the
 * "which tags are detected for any session" rule lives in one place and is unit-testable without
 * Android terminal types. Per-occurrence deduplication is handled by the controllers (one scanner
 * per session), so feeding the same transcript repeatedly fires each tag exactly once.
 */
public final class BackgroundOutputTagScanner {

    private final CallToUserTagController callToUserTagController;
    private final UpdateTagUpdateController updateTagUpdateController;

    public BackgroundOutputTagScanner(CallToUserTagController callToUserTagController,
                                      UpdateTagUpdateController updateTagUpdateController) {
        this.callToUserTagController = callToUserTagController;
        this.updateTagUpdateController = updateTagUpdateController;
    }

    /**
     * Scans the app-update tag for the given session, and the explicit-call tag only when {@code
     * scanCallToUserTag} is true.
     *
     * <p>The explicit-call (call-to-user) reason/scene scan is the expensive per-output work: it
     * reads the full transcript and extracts the reason text. It runs only when the session actually
     * has a pending call (its statusline {@code call:} token is newer than its {@code reply:} token),
     * or when the session has no statusline at all (a non-Claude session that has no reliable
     * statusline-pending signal and so keeps the tag scan as its sole call-to-user source). The
     * app-update tag scan always runs, because it is unrelated to the call-to-user pending state.
     *
     * @param sessionKey the session handle the scanners deduplicate per; a null key is ignored.
     * @param transcript the current terminal transcript text; a null transcript is ignored.
     * @param scanCallToUserTag whether to run the explicit-call (call-to-user) reason/scene scan.
     */
    public void scan(String sessionKey, String transcript, boolean scanCallToUserTag) {
        if (sessionKey == null) return;
        if (transcript == null) return;

        if (scanCallToUserTag && callToUserTagController != null)
            callToUserTagController.onSessionTextChanged(sessionKey, transcript);

        if (updateTagUpdateController != null)
            updateTagUpdateController.onSessionTextChanged(sessionKey, transcript);
    }
}
