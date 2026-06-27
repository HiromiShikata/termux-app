package com.termux.app.terminal;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Environment;
import android.os.SystemClock;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.termux.R;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(AndroidJUnit4.class)
public class SessionListRecyclerViewChangeAnimationCrashInstrumentedTest {

    private static final int ROW_COUNT = 24;
    private static final int TICK_ITERATIONS = 80;
    private static final int LIST_WIDTH_PX = 720;
    private static final int LIST_HEIGHT_PX = 480;

    @Test
    public void rapidRelativeTimePayloadTicksWithTapDuringScrollDoNotCrashAndSingleTapNavigates() {
        Context baseContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Context themedContext = new ContextThemeWrapper(baseContext,
            R.style.Theme_TermuxActivity_DayNight_NoActionBar);

        AtomicReference<Throwable> capturedCrash = new AtomicReference<>(null);
        AtomicInteger singleTapNavigationCount = new AtomicInteger(0);
        AtomicReference<RecyclerView> recyclerViewRef = new AtomicReference<>(null);
        AtomicReference<SessionRowTickAdapter> adapterRef = new AtomicReference<>(null);
        AtomicReference<FrameLayout> rootRef = new AtomicReference<>(null);

        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            RecyclerView recyclerView = new RecyclerView(themedContext);
            recyclerView.setLayoutParams(new FrameLayout.LayoutParams(LIST_WIDTH_PX, LIST_HEIGHT_PX));

            SessionRowTickAdapter adapter = new SessionRowTickAdapter(themedContext,
                buildRowLabels(ROW_COUNT), position -> singleTapNavigationCount.incrementAndGet());

            SessionListBottomSheetController.bindSessionListAdapter(recyclerView, adapter);

            FrameLayout root = new FrameLayout(themedContext);
            root.addView(recyclerView);
            layoutOffscreen(root, LIST_WIDTH_PX, LIST_HEIGHT_PX);

            recyclerViewRef.set(recyclerView);
            adapterRef.set(adapter);
            rootRef.set(root);
        });

        RecyclerView recyclerView = recyclerViewRef.get();
        SessionRowTickAdapter adapter = adapterRef.get();
        assertNotNull(recyclerView);
        assertNotNull(adapter);

        RecyclerView.ItemAnimator itemAnimator = recyclerView.getItemAnimator();
        assertTrue("the session list RecyclerView must use a SimpleItemAnimator so change animations can be disabled",
            itemAnimator instanceof SimpleItemAnimator);
        assertTrue("the session list RecyclerView must have change animations disabled to prevent the "
                + "Tmp detached view recycle crash during per-second relative-time payload updates",
            !((SimpleItemAnimator) itemAnimator).getSupportsChangeAnimations());

        for (int iteration = 0; iteration < TICK_ITERATIONS && capturedCrash.get() == null; iteration++) {
            final int scrollTarget = (iteration * 3) % ROW_COUNT;
            final boolean tapThisIteration = iteration % 4 == 0;
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
                try {
                    adapter.dispatchRelativeTimeTick();
                    recyclerView.scrollToPosition(scrollTarget);
                    if (tapThisIteration) {
                        tapFirstVisibleRow(recyclerView);
                    }
                } catch (Throwable throwable) {
                    capturedCrash.compareAndSet(null, throwable);
                }
            });
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        }

        Throwable crash = capturedCrash.get();
        assertNull("rapid relative-time payload updates with tap-during-scroll must not throw "
            + (crash == null ? "" : crash.getClass().getName() + ": " + crash.getMessage()), crash);
        assertTrue("a single tap on a session row must invoke the row click handler (single-tap navigation)",
            singleTapNavigationCount.get() > 0);

        writeRenderedRowsScreenshot(rootRef.get());
    }

    private static void tapFirstVisibleRow(@NonNull RecyclerView recyclerView) {
        if (recyclerView.getChildCount() == 0) {
            return;
        }
        View rowView = recyclerView.getChildAt(0);
        rowView.performClick();
    }

    private static List<String> buildRowLabels(int count) {
        List<String> labels = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            labels.add("claude-session-worker-" + index);
        }
        return labels;
    }

    private static void layoutOffscreen(@NonNull View view, int widthPx, int heightPx) {
        view.measure(
            View.MeasureSpec.makeMeasureSpec(widthPx, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(heightPx, View.MeasureSpec.EXACTLY));
        view.layout(0, 0, widthPx, heightPx);
    }

    private static void writeRenderedRowsScreenshot(View root) {
        if (root == null) {
            return;
        }
        AtomicReference<File> writtenFile = new AtomicReference<>(null);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            Bitmap bitmap = Bitmap.createBitmap(Math.max(1, root.getWidth()),
                Math.max(1, root.getHeight()), Bitmap.Config.ARGB_8888);
            bitmap.eraseColor(0xFF101010);
            Canvas canvas = new Canvas(bitmap);
            root.draw(canvas);
            File directory = sharedScreenshotDirectory();
            File file = new File(directory, "session-list-recyclerview-aligned-rows.png");
            try (FileOutputStream out = new FileOutputStream(file)) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
            writtenFile.set(file);
        });
        File file = writtenFile.get();
        assertTrue("rendered session list screenshot must be written for the CI artifact",
            file != null && file.exists() && file.length() > 0);
    }

    private static File sharedScreenshotDirectory() {
        File directory = new File(Environment.getExternalStorageDirectory(),
            "termux-instrumentation-screenshots");
        if (!directory.exists()) {
            assertTrue("shared screenshot directory must be creatable so the rendered PNG survives "
                + "the post-test APK uninstall and can be pulled as a CI artifact", directory.mkdirs());
        }
        return directory;
    }

    private static final class SessionRowTickAdapter
        extends RecyclerView.Adapter<SessionRowTickAdapter.RowViewHolder> {

        private final LayoutInflater inflater;
        private final List<String> rowLabels;
        private final RowClickListener rowClickListener;

        interface RowClickListener {
            void onRowClicked(int position);
        }

        SessionRowTickAdapter(@NonNull Context context, @NonNull List<String> rowLabels,
                              @NonNull RowClickListener rowClickListener) {
            this.inflater = LayoutInflater.from(context);
            this.rowLabels = rowLabels;
            this.rowClickListener = rowClickListener;
            setHasStableIds(true);
        }

        void dispatchRelativeTimeTick() {
            for (int position = 0; position < rowLabels.size(); position++) {
                notifyItemChanged(position, TermuxSessionsListViewController.RELATIVE_TIME_PAYLOAD);
            }
        }

        @Override
        public long getItemId(int position) {
            return rowLabels.get(position).hashCode();
        }

        @NonNull
        @Override
        public RowViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View rowView = inflater.inflate(R.layout.item_terminal_sessions_list, parent, false);
            RowViewHolder holder = new RowViewHolder(rowView);
            rowView.setOnClickListener(view -> {
                int position = holder.getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    rowClickListener.onRowClicked(position);
                }
            });
            return holder;
        }

        @Override
        public void onBindViewHolder(@NonNull RowViewHolder holder, int position) {
            bindRowTimes(holder, position);
            TextView title = holder.itemView.findViewById(R.id.session_title);
            title.setText(rowLabels.get(position));
            title.setCompoundDrawablesRelativeWithIntrinsicBounds(
                R.drawable.ic_session_activity_dot_gray, 0, 0, 0);
        }

        @Override
        public void onBindViewHolder(@NonNull RowViewHolder holder, int position,
                                     @NonNull List<Object> payloads) {
            if (payloads.contains(TermuxSessionsListViewController.RELATIVE_TIME_PAYLOAD)) {
                bindRowTimes(holder, position);
                return;
            }
            super.onBindViewHolder(holder, position, payloads);
        }

        private void bindRowTimes(@NonNull RowViewHolder holder, int position) {
            TextView times = holder.itemView.findViewById(R.id.session_row_times);
            times.setText("call " + (SystemClock.elapsedRealtime() % 1000) + "s ago  "
                + "out " + position + "m ago");
            times.setVisibility(View.VISIBLE);
        }

        @Override
        public int getItemCount() {
            return rowLabels.size();
        }

        static final class RowViewHolder extends RecyclerView.ViewHolder {
            RowViewHolder(@NonNull View itemView) {
                super(itemView);
            }
        }
    }
}
