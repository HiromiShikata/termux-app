package com.termux.app.browser;

import android.content.Context;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;

import com.termux.R;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RunWith(RobolectricTestRunner.class)
public class ProjectBrowserOverlayBottomFlushTest {

    private static final int PHONE_WIDTH_DP = 360;
    private static final int PHONE_HEIGHT_DP = 720;

    private static final String CONTROLLER_RELATIVE_PATH =
        "src/main/java/com/termux/app/browser/ProjectBrowserOverlayController.java";

    @Test
    public void overlayBottomSitsFlushAgainstSessionInfoBottomContainerTop() {
        Context context = themedContext();
        View root = inflateRoot(context);

        View overlay = root.findViewById(R.id.project_browser_overlay);
        View sessionInfoBottomContainer = root.findViewById(R.id.session_info_bottom_container);
        View sessionNameBar = root.findViewById(R.id.session_name_bar);

        overlay.setVisibility(View.VISIBLE);
        sessionInfoBottomContainer.setVisibility(View.VISIBLE);
        sessionNameBar.setVisibility(View.VISIBLE);

        layoutAtPhoneSize(context, root);

        int overlayBottom = overlay.getBottom();
        int containerTop = sessionInfoBottomContainer.getTop();

        Assert.assertTrue("session_info_bottom_container must occupy height",
            sessionInfoBottomContainer.getHeight() > 0);
        Assert.assertEquals(
            "Project browser overlay bottom must sit flush against the session info bottom container top with no gap and no overlap",
            containerTop,
            overlayBottom);
    }

    @Test
    public void overlayDoesNotReorderChildrenWithBringToFrontSoRelativeLayoutAnchorsStayValid() throws IOException {
        String controllerSource = readControllerSource();
        Assert.assertFalse(
            "show() must not call bringToFront(): reordering the overlay child invalidates the "
                + "RelativeLayout layout_above dependency resolution and leaves a gap above the bottom button area; "
                + "the overlay relies on its android:elevation for draw order instead",
            controllerSource.contains("bringToFront"));
    }

    private static void layoutAtPhoneSize(Context context, View root) {
        int widthPx = dpToPx(context, PHONE_WIDTH_DP);
        int heightPx = dpToPx(context, PHONE_HEIGHT_DP);
        root.measure(
            View.MeasureSpec.makeMeasureSpec(widthPx, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(heightPx, View.MeasureSpec.EXACTLY));
        root.layout(0, 0, root.getMeasuredWidth(), root.getMeasuredHeight());
    }

    private static View inflateRoot(Context context) {
        return LayoutInflater.from(context)
            .inflate(R.layout.activity_termux, new FrameLayout(context), false);
    }

    private static String readControllerSource() throws IOException {
        Path moduleRelative = Paths.get(CONTROLLER_RELATIVE_PATH);
        if (Files.exists(moduleRelative)) {
            return new String(Files.readAllBytes(moduleRelative), StandardCharsets.UTF_8);
        }
        Path repoRelative = Paths.get("app").resolve(CONTROLLER_RELATIVE_PATH);
        return new String(Files.readAllBytes(repoRelative), StandardCharsets.UTF_8);
    }

    private static int dpToPx(Context context, int dp) {
        return Math.round(TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, dp, context.getResources().getDisplayMetrics()));
    }

    private static Context themedContext() {
        return new ContextThemeWrapper(RuntimeEnvironment.getApplication(),
            R.style.Theme_TermuxActivity_DayNight_NoActionBar);
    }
}
