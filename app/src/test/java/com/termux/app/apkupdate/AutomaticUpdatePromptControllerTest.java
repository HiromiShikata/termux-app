package com.termux.app.apkupdate;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AutomaticUpdatePromptControllerTest {

    private static final ApkUpdateAvailability VERSION_1_2_3 =
        ApkUpdateAvailability.available("1.2.3", "https://example.com/app.apk", "app.apk", 0L);

    private static final ApkUpdateAvailability VERSION_1_2_4 =
        ApkUpdateAvailability.available("1.2.4", "https://example.com/app.apk", "app.apk", 0L);

    private static final class InMemoryStore implements ApkUpdatePendingState.Store {

        private final Map<String, String> values = new HashMap<>();

        @Nullable
        @Override
        public String getString(String key) {
            return values.get(key);
        }

        @Override
        public void putString(String key, String value) {
            values.put(key, value);
        }

        @Override
        public void remove(String key) {
            values.remove(key);
        }
    }

    private static final class RecordingIndicatorView
        implements ApkUpdateFloatingIndicatorController.IndicatorView {

        final List<String> shownVersions = new ArrayList<>();
        int hideCount;

        @Override
        public void showUpdateAvailable(String latestVersionName, Runnable onTapped) {
            shownVersions.add(latestVersionName);
        }

        @Override
        public void hide() {
            hideCount++;
        }
    }

    private static final class RecordingUpdateTrigger
        implements ApkUpdateFloatingIndicatorController.UpdateTrigger {

        final List<String> startedVersions = new ArrayList<>();

        @Override
        public void startUpdate(ApkUpdateAvailability availability) {
            startedVersions.add(availability.getLatestVersionName());
        }
    }

    private static final class RecordingDialog implements AutomaticUpdatePromptController.Dialog {

        final List<String> openedVersions = new ArrayList<>();
        @Nullable Runnable onInstallChosen;
        @Nullable Runnable onCancelled;

        @Override
        public void openUpdateDialog(@NonNull String latestVersionName, @NonNull Runnable onInstallChosen,
                                     @NonNull Runnable onCancelled) {
            openedVersions.add(latestVersionName);
            this.onInstallChosen = onInstallChosen;
            this.onCancelled = onCancelled;
        }
    }

    private final InMemoryStore store = new InMemoryStore();
    private final RecordingIndicatorView indicatorView = new RecordingIndicatorView();
    private final RecordingUpdateTrigger updateTrigger = new RecordingUpdateTrigger();
    private final RecordingDialog dialog = new RecordingDialog();

    private AutomaticUpdatePromptController newController() {
        return new AutomaticUpdatePromptController(dialog,
            new ApkUpdateFloatingIndicatorController(indicatorView, updateTrigger),
            new DeclinedUpdateVersion(store));
    }

    @Test
    public void anAvailableUpdateOpensTheDialogAndAlsoLeavesTheFloatingButtonBehindIt() {
        newController().onUpdateAvailable(VERSION_1_2_3);

        Assert.assertEquals("the owner must not have to press anything for the dialog to appear",
            1, dialog.openedVersions.size());
        Assert.assertEquals("1.2.3", dialog.openedVersions.get(0));
        Assert.assertEquals("the floating button has to stay available behind the dialog",
            1, indicatorView.shownVersions.size());
        Assert.assertTrue("no install may start before the owner chooses one",
            updateTrigger.startedVersions.isEmpty());
    }

    @Test
    public void choosingToInstallFromTheDialogStartsTheInstallOfThatVersion() {
        newController().onUpdateAvailable(VERSION_1_2_3);
        dialog.onInstallChosen.run();

        Assert.assertEquals("choosing to install has to start the install the button would have started",
            1, updateTrigger.startedVersions.size());
        Assert.assertEquals("1.2.3", updateTrigger.startedVersions.get(0));
    }

    @Test
    public void cancellingTheDialogLeavesTheFloatingButtonAndDoesNotReopenTheDialogForThatVersion() {
        AutomaticUpdatePromptController controller = newController();
        controller.onUpdateAvailable(VERSION_1_2_3);
        dialog.onCancelled.run();

        controller.onUpdateAvailable(VERSION_1_2_3);

        Assert.assertEquals("the dialog must not reopen for a version the owner already cancelled",
            1, dialog.openedVersions.size());
        Assert.assertEquals("the floating button has to stay offered after cancelling",
            2, indicatorView.shownVersions.size());
        Assert.assertEquals("1.2.3", indicatorView.shownVersions.get(1));
    }

    @Test
    public void aVersionNewerThanTheCancelledOneOpensTheDialogAgain() {
        AutomaticUpdatePromptController controller = newController();
        controller.onUpdateAvailable(VERSION_1_2_3);
        dialog.onCancelled.run();

        controller.onUpdateAvailable(VERSION_1_2_4);

        Assert.assertEquals("a version the owner never cancelled has to open the dialog",
            2, dialog.openedVersions.size());
        Assert.assertEquals("1.2.4", dialog.openedVersions.get(1));
    }

    @Test
    public void theCancelledVersionSurvivesANewControllerSoARestartedCheckDoesNotNagAgain() {
        newController().onUpdateAvailable(VERSION_1_2_3);
        dialog.onCancelled.run();

        newController().onUpdateAvailable(VERSION_1_2_3);

        Assert.assertEquals("a later check must not reopen the dialog for the cancelled version",
            1, dialog.openedVersions.size());
    }

    @Test
    public void beingUpToDateHidesTheFloatingButtonAndOpensNoDialog() {
        newController().onUpdateAvailable(ApkUpdateAvailability.upToDate("1.2.3"));

        Assert.assertTrue("no dialog may open when there is nothing to install",
            dialog.openedVersions.isEmpty());
        Assert.assertEquals("the floating button has to be hidden when there is nothing to install",
            1, indicatorView.hideCount);
    }
}
