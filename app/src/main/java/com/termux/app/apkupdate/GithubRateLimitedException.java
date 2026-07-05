package com.termux.app.apkupdate;

import java.io.IOException;

/**
 * Thrown when the GitHub REST API answers an update check with a rate-limit response
 * (HTTP 403 or 429 with the unauthenticated {@code X-RateLimit-Remaining: 0} budget exhausted).
 *
 * <p>The unauthenticated {@code releases/latest} endpoint allows only 60 requests per hour per IP.
 * Repeated manual re-checks, the auto-check-on-launch, and the settings-open check share that
 * budget, so a burst of checks can exhaust it and make the endpoint answer 403/429. Treating that
 * response as a generic failure hid the real cause and silently surfaced "no update"; this distinct
 * type lets callers both recognise the rate limit (to fall back to the Atom feed) and report it to
 * the user as a rate-limit condition rather than as "up to date".
 */
public final class GithubRateLimitedException extends IOException {

    public GithubRateLimitedException(String message) {
        super(message);
    }
}
