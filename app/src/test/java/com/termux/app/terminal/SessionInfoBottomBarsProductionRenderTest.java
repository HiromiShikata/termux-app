package com.termux.app.terminal;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.termux.R;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.GraphicsMode;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

@RunWith(RobolectricTestRunner.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class SessionInfoBottomBarsProductionRenderTest {

    private static final long ONE_SECOND_MILLIS = 1000L;
    private static final long ONE_MINUTE_MILLIS = 60L * ONE_SECOND_MILLIS;
    private static final long ONE_HOUR_MILLIS = 60L * ONE_MINUTE_MILLIS;
    private static final String SESSION_NAME = "claude-main";
    private static final int RENDER_WIDTH_PIXELS = 720;

    @Test
    public void productionBinderRendersBothTimesAndSceneInTheRealInfoArea() {
        long now = 2_000_000_000L;
        View root = inflateActivityLayout();
        SessionNewActivityStore store = storeWithTimesAndScene(now);

        SessionInfoBottomBarsBinder.bind(root, store, SESSION_NAME, now, () -> {
        });

        TextView timesBar = root.findViewById(R.id.session_last_reply_bar);
        View sceneBar = root.findViewById(R.id.session_pending_call_to_user_bar);
        TextView sceneText = root.findViewById(R.id.session_pending_call_to_user_text);
        View bottomContainer = root.findViewById(R.id.session_info_bottom_container);

        Assert.assertEquals(View.VISIBLE, timesBar.getVisibility());
        Assert.assertEquals(View.VISIBLE, sceneBar.getVisibility());
        Assert.assertEquals("call: 3h  out: 12m  reply: 45s", timesBar.getText().toString());
        Assert.assertEquals("needs approval to deploy\nwaiting for the secret value",
            sceneText.getText().toString());
        Assert.assertTrue(isDescendantOf(timesBar, bottomContainer));
        Assert.assertTrue(isDescendantOf(sceneBar, bottomContainer));
    }

    @Test
    public void productionBinderHidesSceneWhenThereIsNoUnacknowledgedCall() {
        long now = 2_000_000_000L;
        View root = inflateActivityLayout();
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordStatuslineTimes(SESSION_NAME, now - 3L * ONE_HOUR_MILLIS,
            now - 12L * ONE_MINUTE_MILLIS, now - 45L * ONE_SECOND_MILLIS);

        SessionInfoBottomBarsBinder.bind(root, store, SESSION_NAME, now, () -> {
        });

        View sceneBar = root.findViewById(R.id.session_pending_call_to_user_bar);
        TextView timesBar = root.findViewById(R.id.session_last_reply_bar);
        Assert.assertEquals(View.VISIBLE, timesBar.getVisibility());
        Assert.assertEquals(View.GONE, sceneBar.getVisibility());
    }

    @Test
    public void renderedInfoAreaScreenshotShowsSceneAlongsideTimes() throws IOException {
        long now = 2_000_000_000L;
        View root = inflateActivityLayout();
        SessionNewActivityStore store = storeWithTimesAndScene(now);

        SessionInfoBottomBarsBinder.bind(root, store, SESSION_NAME, now, () -> {
        });

        View bottomContainer = root.findViewById(R.id.session_info_bottom_container);
        Bitmap bitmap = renderToBitmap(bottomContainer);

        File output = new File(System.getProperty("java.io.tmpdir"),
            "session-info-area-scene-render.png");
        try (FileOutputStream stream = new FileOutputStream(output)) {
            Assert.assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream));
        }
        Assert.assertTrue("rendered info area screenshot must be written for device-equivalent proof",
            output.exists() && output.length() > 0);
        Assert.assertTrue(bitmap.getWidth() > 0 && bitmap.getHeight() > 0);
    }

    private static SessionNewActivityStore storeWithTimesAndScene(long now) {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordStatuslineTimes(SESSION_NAME, now - 3L * ONE_HOUR_MILLIS,
            now - 12L * ONE_MINUTE_MILLIS, now - 45L * ONE_SECOND_MILLIS);
        store.recordExplicitCall(SESSION_NAME, now, "needs approval to deploy");
        store.recordExplicitCall(SESSION_NAME, now, "waiting for the secret value");
        return store;
    }

    private static Bitmap renderToBitmap(View view) {
        view.measure(
            View.MeasureSpec.makeMeasureSpec(RENDER_WIDTH_PIXELS, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        int height = Math.max(1, view.getMeasuredHeight());
        view.layout(0, 0, view.getMeasuredWidth(), height);
        Bitmap bitmap = Bitmap.createBitmap(view.getMeasuredWidth(), height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(0xFF101010);
        view.draw(canvas);
        return bitmap;
    }

    private static boolean isDescendantOf(View view, View ancestor) {
        View current = view;
        while (current != null) {
            if (current == ancestor) {
                return true;
            }
            if (current.getParent() instanceof View) {
                current = (View) current.getParent();
            } else {
                return false;
            }
        }
        return false;
    }

    private static View inflateActivityLayout() {
        Context context = new ContextThemeWrapper(RuntimeEnvironment.getApplication(),
            R.style.Theme_TermuxActivity_DayNight_NoActionBar);
        return LayoutInflater.from(context)
            .inflate(R.layout.activity_termux, new FrameLayout(context), false);
    }
}
