package com.termux.app.sessiondefinition;

import com.termux.R;
import com.termux.app.TermuxActivity;
import com.termux.shared.logger.Logger;

import java.util.List;

public final class SessionDefinitionController {

    private static final String LOG_TAG = "SessionDefinitionController";

    private final TermuxActivity activity;
    private final SessionDefinitionLoader loader;

    public SessionDefinitionController(TermuxActivity activity) {
        this(activity, new SessionDefinitionLoader(new HttpSessionDefinitionDocumentFetcher(), new SessionDefinitionParser()));
    }

    public SessionDefinitionController(TermuxActivity activity, SessionDefinitionLoader loader) {
        this.activity = activity;
        this.loader = loader;
    }

    public void loadAndBuildSessions() {
        String baseUrl = activity.getPreferences().getSessionDefinitionUrl().trim();
        if (baseUrl.isEmpty()) {
            activity.showToast(activity.getString(R.string.msg_session_definition_url_not_set), true);
            return;
        }

        new Thread(() -> {
            try {
                List<SessionDefinitionEntry> entries = loader.load(baseUrl);
                activity.runOnUiThread(() -> buildSessions(entries));
            } catch (Exception exception) {
                Logger.logStackTraceWithMessage(LOG_TAG, "Failed to load session definition from " + baseUrl, exception);
                activity.runOnUiThread(() ->
                    activity.showToast(activity.getString(R.string.msg_session_definition_load_failed), true));
            }
        }).start();
    }

    private void buildSessions(List<SessionDefinitionEntry> entries) {
        if (entries.isEmpty()) {
            activity.showToast(activity.getString(R.string.msg_session_definition_no_entries), true);
            return;
        }

        for (SessionDefinitionEntry entry : entries) {
            activity.getTermuxTerminalSessionClient().addNewSession(false, entry.getSessionName());
        }

        activity.getDrawer().closeDrawers();
    }
}
