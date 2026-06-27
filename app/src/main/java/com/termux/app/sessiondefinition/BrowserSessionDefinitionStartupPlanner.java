package com.termux.app.sessiondefinition;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

/**
 * Decides how a session created from the in-app browser (address bar or link) should be launched.
 *
 * <p>A browser-created session is named after its URL. When that URL matches one of the configured
 * session definitions (per {@link SessionDefinitionEntryMatcher}), the session must be launched with
 * the same configured startup command that a definition-built session receives, rather than as a bare
 * shell. When no definition matches the URL, no startup command is applied and the session launches as
 * a plain shell, leaving the previous behavior unchanged.
 *
 * <p>This planner performs no I/O and holds no Android dependencies so it can be unit tested directly.
 */
public final class BrowserSessionDefinitionStartupPlanner {

    private final SessionDefinitionEntryMatcher matcher;
    private final SessionDefinitionPlanner planner;

    public BrowserSessionDefinitionStartupPlanner() {
        this(new SessionDefinitionEntryMatcher(), new SessionDefinitionPlanner());
    }

    public BrowserSessionDefinitionStartupPlanner(@NonNull SessionDefinitionEntryMatcher matcher,
                                                  @NonNull SessionDefinitionPlanner planner) {
        this.matcher = matcher;
        this.planner = planner;
    }

    /**
     * @param entries         the currently loaded session definitions
     * @param sessionName     the URL the browser-created session is named after
     * @param commandTemplate the configured session startup command template (with {@code {name}}
     *                        placeholder); may be {@code null} or empty when none is configured
     * @return a planned session whose command is set only when {@code sessionName} matches a definition
     *         and a non-empty template is configured; otherwise a plain named session with no command
     */
    @NonNull
    public SessionDefinitionPlannedSession plan(@NonNull List<SessionDefinitionEntry> entries,
                                                @Nullable String sessionName,
                                                @Nullable String commandTemplate) {
        SessionDefinitionEntry matchedEntry = matcher.findEntryForSessionName(entries, sessionName);
        if (matchedEntry == null) {
            return new SessionDefinitionPlannedSession(sessionName, null);
        }
        return planner.planNamedSession(sessionName, commandTemplate);
    }
}
