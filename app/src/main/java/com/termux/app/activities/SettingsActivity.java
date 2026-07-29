package com.termux.app.activities;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;

import com.termux.BuildConfig;
import com.termux.R;
import com.termux.app.apkupdate.ApkUpdateCheckTimeFormatter;
import com.termux.app.apkupdate.ApkRevertUiController;
import com.termux.app.apkupdate.ApkUpdateManager;
import com.termux.app.apkupdate.ApkUpdateSettingsOpenCheckThrottle;
import com.termux.app.apkupdate.ApkUpdateUiController;
import com.termux.app.TermuxActivity;
import com.termux.app.diagnostics.DiagnosticsShareAction;
import com.termux.app.diagnostics.TermuxActivityHolder;
import com.termux.app.style.TermuxStyleLauncher;
import com.termux.app.terminal.session.PersistedSessionClearer;
import com.termux.app.terminal.session.PersistedSessionStore;
import com.termux.shared.termux.settings.preferences.TermuxAppSharedPreferences;
import com.termux.shared.activities.ReportActivity;
import com.termux.shared.file.FileUtils;
import com.termux.shared.models.ReportInfo;
import com.termux.app.models.UserAction;
import com.termux.shared.activity.ActivityUtils;
import com.termux.shared.termux.settings.preferences.TermuxAPIAppSharedPreferences;
import com.termux.shared.termux.settings.preferences.TermuxFloatAppSharedPreferences;
import com.termux.shared.termux.settings.preferences.TermuxTaskerAppSharedPreferences;
import com.termux.shared.termux.settings.preferences.TermuxWidgetAppSharedPreferences;
import com.termux.shared.android.AndroidUtils;
import com.termux.shared.termux.TermuxConstants;
import com.termux.shared.termux.TermuxUtils;
import com.termux.shared.activity.media.AppCompatActivityUtils;
import com.termux.shared.theme.NightMode;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AppCompatActivityUtils.setNightMode(this, NightMode.getAppNightMode().getName(), true);

        setContentView(R.layout.activity_settings);
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings, new RootPreferencesFragment())
                .commit();
        }

        AppCompatActivityUtils.setToolbar(this, com.termux.shared.R.id.toolbar);
        AppCompatActivityUtils.setShowBackButtonInActionBar(this, true);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    public static class RootPreferencesFragment extends PreferenceFragmentCompat {

        private final ApkUpdateCheckTimeFormatter checkTimeFormatter = new ApkUpdateCheckTimeFormatter();
        private SharedPreferences sharedPreferences;
        private SharedPreferences.OnSharedPreferenceChangeListener lastCheckSummaryUpdater;

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            Context context = getContext();
            if (context == null) return;

            setPreferencesFromResource(R.xml.root_preferences, rootKey);

            registerLastCheckSummaryUpdater(context);
            checkForApkUpdateOnSettingsOpen(context);

            configureStylePreference(context);
            configureAutosshConfigPreference(context);
            configureResetSessionConfigPreference(context);
            configureKillSessionCommandConfigPreference(context);
            configureSessionDefinitionConfigPreference(context);
            configureAlwaysNaSessionNamesPreference(context);
            configureShareDiagnosticsPreference(context);
            configureCrashLogViewerPreference(context);
            configureClearSavedSessionsPreference(context);
            configureUpdateApkPreference(context);
            configureRevertApkPreference();
            configureAboutPreference(context);

            configurePluginPreferenceVisibility(context);
        }

        private void configurePluginPreferenceVisibility(@NonNull Context context) {
            new Thread() {
                @Override
                public void run() {
                    boolean termuxAPIInstalled = TermuxAPIAppSharedPreferences.build(context, false) != null;
                    boolean termuxFloatInstalled = TermuxFloatAppSharedPreferences.build(context, false) != null;
                    boolean termuxTaskerInstalled = TermuxTaskerAppSharedPreferences.build(context, false) != null;
                    boolean termuxWidgetInstalled = TermuxWidgetAppSharedPreferences.build(context, false) != null;
                    postWhenLayoutIdle(() -> applyPluginPreferenceVisibility(
                        termuxAPIInstalled, termuxFloatInstalled, termuxTaskerInstalled, termuxWidgetInstalled));
                }
            }.start();
        }

        private void applyPluginPreferenceVisibility(boolean termuxAPIInstalled, boolean termuxFloatInstalled,
                                                     boolean termuxTaskerInstalled, boolean termuxWidgetInstalled) {
            setPreferenceVisible("termux_api", termuxAPIInstalled);
            setPreferenceVisible("termux_float", termuxFloatInstalled);
            setPreferenceVisible("termux_tasker", termuxTaskerInstalled);
            setPreferenceVisible("termux_widget", termuxWidgetInstalled);
        }

        private void setPreferenceVisible(@NonNull String key, boolean visible) {
            Preference preference = findPreference(key);
            if (preference != null) {
                preference.setVisible(visible);
            }
        }

        private void configureStylePreference(@NonNull Context context) {
            Preference stylePreference = findPreference("style");
            if (stylePreference != null) {
                stylePreference.setOnPreferenceClickListener(preference -> {
                    TermuxStyleLauncher.showStylingDialog(requireActivity());
                    return true;
                });
            }
        }

        private void configureAutosshConfigPreference(@NonNull Context context) {
            Preference autosshConfigPreference = findPreference("autossh_config");
            if (autosshConfigPreference != null) {
                autosshConfigPreference.setOnPreferenceClickListener(preference -> {
                    ActivityUtils.startActivity(context, new Intent(context, AutosshConfigActivity.class));
                    return true;
                });
            }
        }

        private void configureResetSessionConfigPreference(@NonNull Context context) {
            Preference resetSessionConfigPreference = findPreference("reset_session_config");
            if (resetSessionConfigPreference != null) {
                resetSessionConfigPreference.setOnPreferenceClickListener(preference -> {
                    ActivityUtils.startActivity(context, new Intent(context, ResetSessionConfigActivity.class));
                    return true;
                });
            }
        }

        private void configureKillSessionCommandConfigPreference(@NonNull Context context) {
            Preference killSessionCommandConfigPreference = findPreference("kill_session_command_config");
            if (killSessionCommandConfigPreference != null) {
                killSessionCommandConfigPreference.setOnPreferenceClickListener(preference -> {
                    ActivityUtils.startActivity(context, new Intent(context, KillSessionCommandConfigActivity.class));
                    return true;
                });
            }
        }

        private void configureSessionDefinitionConfigPreference(@NonNull Context context) {
            Preference sessionDefinitionConfigPreference = findPreference("session_definition_config");
            if (sessionDefinitionConfigPreference != null) {
                sessionDefinitionConfigPreference.setOnPreferenceClickListener(preference -> {
                    ActivityUtils.startActivity(context, new Intent(context, SessionDefinitionConfigActivity.class));
                    return true;
                });
            }
        }

        private void configureAlwaysNaSessionNamesPreference(@NonNull Context context) {
            Preference alwaysNaSessionNamesPreference = findPreference("always_na_session_names");
            if (alwaysNaSessionNamesPreference != null) {
                alwaysNaSessionNamesPreference.setOnPreferenceClickListener(preference -> {
                    ActivityUtils.startActivity(context, new Intent(context, AlwaysNaSessionNamesConfigActivity.class));
                    return true;
                });
            }
        }

        private void configureShareDiagnosticsPreference(@NonNull Context context) {
            Preference shareDiagnosticsPreference = findPreference("share_diagnostics");
            if (shareDiagnosticsPreference != null) {
                shareDiagnosticsPreference.setOnPreferenceClickListener(preference -> {
                    TermuxActivity termuxActivity = TermuxActivityHolder.get();
                    if (termuxActivity == null) {
                        Toast.makeText(context, R.string.msg_diagnostics_unavailable, Toast.LENGTH_SHORT).show();
                        return true;
                    }
                    new DiagnosticsShareAction().shareFrom(termuxActivity);
                    return true;
                });
            }
        }

        private void configureCrashLogViewerPreference(@NonNull Context context) {
            Preference crashLogViewerPreference = findPreference("crash_log_viewer");
            if (crashLogViewerPreference != null) {
                crashLogViewerPreference.setOnPreferenceClickListener(preference -> {
                    ActivityUtils.startActivity(context, new Intent(context, CrashLogViewerActivity.class));
                    return true;
                });
            }
        }

        private void configureClearSavedSessionsPreference(@NonNull Context context) {
            Preference clearSavedSessionsPreference = findPreference("clear_saved_sessions");
            if (clearSavedSessionsPreference != null) {
                clearSavedSessionsPreference.setOnPreferenceClickListener(preference -> {
                    new AlertDialog.Builder(requireActivity())
                        .setTitle(R.string.title_confirm_clear_saved_sessions)
                        .setMessage(R.string.msg_confirm_clear_saved_sessions)
                        .setPositiveButton(android.R.string.yes, (dialog, which) -> clearSavedSessions(context))
                        .setNegativeButton(android.R.string.no, null)
                        .show();
                    return true;
                });
            }
        }

        private void clearSavedSessions(@NonNull Context context) {
            TermuxAppSharedPreferences preferences = TermuxAppSharedPreferences.build(context, true);
            if (preferences == null) return;

            PersistedSessionStore store = new PersistedSessionStore() {
                @Override
                public String getPersistedSessions() {
                    return preferences.getPersistedSessions();
                }

                @Override
                public void setPersistedSessions(String value) {
                    preferences.setPersistedSessions(value);
                }
            };
            PersistedSessionClearer.clear(store);
            Toast.makeText(context, R.string.msg_saved_sessions_cleared, Toast.LENGTH_SHORT).show();
        }

        private void configureUpdateApkPreference(@NonNull Context context) {
            Preference updateApkPreference = findPreference("update_apk");
            if (updateApkPreference != null) {
                updateApkPreference.setOnPreferenceClickListener(preference -> {
                    new ApkUpdateUiController(requireActivity()).checkAndPrompt(true);
                    return true;
                });
            }
            updateLastCheckSummary(context);
        }

        private void configureRevertApkPreference() {
            Preference revertApkPreference = findPreference("revert_apk");
            if (revertApkPreference != null) {
                revertApkPreference.setOnPreferenceClickListener(preference -> {
                    new ApkRevertUiController(requireActivity()).startRevert();
                    return true;
                });
            }
        }

        private void checkForApkUpdateOnSettingsOpen(@NonNull Context context) {
            ApkUpdateSettingsOpenCheckThrottle throttle = new ApkUpdateSettingsOpenCheckThrottle(context);
            long nowMillis = System.currentTimeMillis();
            if (!throttle.shouldCheckNow(nowMillis)) {
                return;
            }
            throttle.recordCheckedAt(nowMillis);
            new ApkUpdateUiController(requireActivity()).checkAndPrompt(false);
        }

        private void registerLastCheckSummaryUpdater(@NonNull Context context) {
            sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
            lastCheckSummaryUpdater = (preferences, key) -> {
                if (ApkUpdateManager.PREFERENCE_KEY_LAST_CHECK_TIME.equals(key)) {
                    updateLastCheckSummary(context);
                }
            };
            sharedPreferences.registerOnSharedPreferenceChangeListener(lastCheckSummaryUpdater);
        }

        private void updateLastCheckSummary(@NonNull Context context) {
            postWhenLayoutIdle(() -> applyLastCheckSummary(context));
        }

        private void postWhenLayoutIdle(@NonNull Runnable action) {
            if (Looper.myLooper() != Looper.getMainLooper()) {
                new Handler(Looper.getMainLooper()).post(() -> postWhenLayoutIdle(action));
                return;
            }
            RecyclerView listView = getListView();
            if (listView == null) {
                new Handler(Looper.getMainLooper()).post(action);
                return;
            }
            listView.post(new Runnable() {
                @Override
                public void run() {
                    if (listView.isComputingLayout()) {
                        listView.post(this);
                        return;
                    }
                    action.run();
                }
            });
        }

        private void applyLastCheckSummary(@NonNull Context context) {
            Preference updateApkPreference = findPreference("update_apk");
            if (updateApkPreference == null) return;

            long lastCheckTime = ApkUpdateManager.getLastCheckTime(context);
            if (checkTimeFormatter.hasCheckTime(lastCheckTime)) {
                updateApkPreference.setSummary(context.getString(R.string.update_apk_last_check_summary,
                    checkTimeFormatter.format(lastCheckTime)));
            } else {
                updateApkPreference.setSummary(R.string.update_apk_never_checked_summary);
            }
        }

        @Override
        public void onDestroy() {
            if (sharedPreferences != null && lastCheckSummaryUpdater != null) {
                sharedPreferences.unregisterOnSharedPreferenceChangeListener(lastCheckSummaryUpdater);
            }
            super.onDestroy();
        }

        private void configureAboutPreference(@NonNull Context context) {
            LongClickCopyPreference aboutPreference = findPreference("about");
            if (aboutPreference != null) {
                aboutPreference.setSummary(context.getString(R.string.about_preference_summary_version,
                    BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE));
                aboutPreference.setCopyText(BuildConfig.VERSION_NAME + " (" + BuildConfig.VERSION_CODE + ")");
                aboutPreference.setCopyConfirmationToast(context.getString(R.string.msg_version_copied));
                aboutPreference.setOnPreferenceClickListener(preference -> {
                    new Thread() {
                        @Override
                        public void run() {
                            String title = "About";

                            StringBuilder aboutString = new StringBuilder();
                            aboutString.append(TermuxUtils.getAppInfoMarkdownString(context, TermuxUtils.AppInfoMode.TERMUX_AND_PLUGIN_PACKAGES));
                            aboutString.append("\n\n").append(AndroidUtils.getDeviceInfoMarkdownString(context, true));
                            aboutString.append("\n\n").append(TermuxUtils.getImportantLinksMarkdownString(context));

                            String userActionName = UserAction.ABOUT.getName();

                            ReportInfo reportInfo = new ReportInfo(userActionName,
                                TermuxConstants.TERMUX_APP.TERMUX_SETTINGS_ACTIVITY_NAME, title);
                            reportInfo.setReportString(aboutString.toString());
                            reportInfo.setReportSaveFileLabelAndPath(userActionName,
                                Environment.getExternalStorageDirectory() + "/" +
                                    FileUtils.sanitizeFileName(TermuxConstants.TERMUX_APP_NAME + "-" + userActionName + ".log", true, true));

                            ReportActivity.startReportActivity(context, reportInfo);
                        }
                    }.start();

                    return true;
                });
            }
        }
    }

}
