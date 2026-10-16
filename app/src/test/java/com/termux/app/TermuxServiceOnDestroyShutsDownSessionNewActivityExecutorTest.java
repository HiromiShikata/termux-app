package com.termux.app;

import com.termux.app.terminal.SessionNewActivityStore;
import com.termux.app.terminal.session.FileSessionNewActivityPersistence;
import com.termux.shared.termux.settings.preferences.TermuxAppSharedPreferences;
import com.termux.shared.termux.shell.TermuxShellManager;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ServiceController;

import java.lang.reflect.Field;
import java.util.concurrent.ExecutorService;

@RunWith(RobolectricTestRunner.class)
public class TermuxServiceOnDestroyShutsDownSessionNewActivityExecutorTest {

    private ServiceController<TermuxService> controller;
    private TermuxService service;

    @Before
    public void setUp() {
        TermuxShellManager.init(RuntimeEnvironment.getApplication());
        TermuxShellManager.getShellManager().mTermuxSessions.clear();
        TermuxAppSharedPreferences.build(RuntimeEnvironment.getApplication()).setAutosshCommand("");
        controller = Robolectric.buildService(TermuxService.class).create();
        service = controller.get();
    }

    @Test
    public void destroyShutsDownTheFileSessionNewActivityPersistenceWriteExecutor() throws Exception {
        SessionNewActivityStore store = service.getSessionNewActivityStore();
        Field persistenceField = SessionNewActivityStore.class.getDeclaredField("mPersistence");
        persistenceField.setAccessible(true);
        Object persistence = persistenceField.get(store);
        Assert.assertTrue("The service's session-activity store must be backed by "
                + "FileSessionNewActivityPersistence in this Robolectric environment",
            persistence instanceof FileSessionNewActivityPersistence);
        Field writeExecutorField = FileSessionNewActivityPersistence.class.getDeclaredField("mWriteExecutor");
        writeExecutorField.setAccessible(true);
        ExecutorService writeExecutor = (ExecutorService) writeExecutorField.get(persistence);

        controller.destroy();

        Assert.assertTrue("TermuxService.onDestroy() must shut down the write executor of the "
            + "FileSessionNewActivityPersistence it built for itself", writeExecutor.isShutdown());
    }
}
