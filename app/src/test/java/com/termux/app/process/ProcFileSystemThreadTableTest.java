package com.termux.app.process;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.List;

public class ProcFileSystemThreadTableTest {

    private File mTaskDirectory;

    @Before
    public void createTaskDirectory() throws IOException {
        File placeholder = File.createTempFile("thread-table", "");
        Assert.assertTrue(placeholder.delete());
        Assert.assertTrue(placeholder.mkdir());
        mTaskDirectory = placeholder;
    }

    @After
    public void removeTaskDirectory() {
        deleteRecursively(mTaskDirectory);
    }

    private void deleteRecursively(File file) {
        File[] children = file.listFiles();
        if (children != null) {
            for (File child : children) {
                deleteRecursively(child);
            }
        }
        file.delete();
    }

    private void givenThreadDirectory(String threadIdentifier) {
        Assert.assertTrue(new File(mTaskDirectory, threadIdentifier).mkdir());
    }

    private void givenThreadWithStatistics(String threadIdentifier, String statistics)
        throws IOException {
        givenThreadDirectory(threadIdentifier);
        File statisticsFile = new File(new File(mTaskDirectory, threadIdentifier), "stat");
        try (OutputStream output = new FileOutputStream(statisticsFile)) {
            output.write(statistics.getBytes(Charset.forName("UTF-8")));
        }
    }

    private static String statisticsLine(String identifier, String name, String schedulerState,
                                         long userTimeTicks, long systemTimeTicks) {
        StringBuilder line = new StringBuilder();
        line.append(identifier).append(" (").append(name).append(") ").append(schedulerState);
        for (int fieldIndex = 0; fieldIndex < 10; fieldIndex++) {
            line.append(' ').append(0);
        }
        line.append(' ').append(userTimeTicks).append(' ').append(systemTimeTicks);
        line.append(" 0 0 0 0 0 0\n");
        return line.toString();
    }

    private ProcFileSystemThreadTable threadTable() {
        return new ProcFileSystemThreadTable(mTaskDirectory);
    }

    private static ProcessThread threadNamed(List<ProcessThread> threads, String name) {
        for (ProcessThread thread : threads) {
            if (name.equals(thread.getName())) {
                return thread;
            }
        }
        return null;
    }

    @Test
    public void aThreadIsReadWithItsNameItsSchedulerStateAndItsProcessorTime() throws IOException {
        givenThreadWithStatistics("2481", statisticsLine("2481", "RenderThread", "R", 19012L, 421L));

        ProcessThread renderThread = threadNamed(threadTable().threads(), "RenderThread");

        Assert.assertNotNull("a stall spent waiting on the render thread can only be attributed when"
            + " that thread is found in the table at all", renderThread);
        Assert.assertEquals("a thread that is running is spending the processor time the caller is"
            + " waiting for, and a thread that is sleeping is not, so the state has to be read",
            "R", renderThread.getSchedulerState());
        Assert.assertEquals("the processor time is what separates a saturated render thread from one"
            + " that is itself blocked, which is the whole distinction the reading exists to make",
            19012L, renderThread.getUserTimeTicks());
        Assert.assertEquals("system time is spent on the caller's behalf just as user time is, so"
            + " leaving it out understates how busy the thread was", 421L,
            renderThread.getSystemTimeTicks());
    }

    @Test
    public void aThreadNameHoldingSpacesAndParenthesesIsReadWholeSoTheLaterFieldsAreNotShifted()
        throws IOException {
        givenThreadWithStatistics("2481", statisticsLine("2481", "od (mystery) ", "S", 77L, 3L));

        ProcessThread thread = threadNamed(threadTable().threads(), "od (mystery) ");

        Assert.assertNotNull("thread names are written unquoted between parentheses and may contain"
            + " both spaces and parentheses, so splitting on the first closing parenthesis would move"
            + " every later field along and report another thread's processor time", thread);
        Assert.assertEquals("the fields after the name have to line up once the name is taken whole",
            77L, thread.getUserTimeTicks());
        Assert.assertEquals("the fields after the name have to line up once the name is taken whole",
            3L, thread.getSystemTimeTicks());
    }

    @Test
    public void onlyTheNumericEntriesAreReadAsThreadsBecauseTheOthersAreNotThreads()
        throws IOException {
        givenThreadWithStatistics("2481", statisticsLine("2481", "RenderThread", "S", 1L, 1L));
        Assert.assertTrue(new File(mTaskDirectory, "notathread").createNewFile());

        List<ProcessThread> threads = threadTable().threads();

        Assert.assertEquals("counting a non-thread entry would put a thread in the table that the"
            + " process is not running. Actual: " + threads.size(), 1, threads.size());
    }

    @Test
    public void aThreadThatExitedBeforeItsStatisticsCouldBeReadIsLeftOutRatherThanFailingTheReading()
        throws IOException {
        givenThreadWithStatistics("2481", statisticsLine("2481", "RenderThread", "S", 5L, 1L));
        givenThreadDirectory("2482");

        List<ProcessThread> threads = threadTable().threads();

        Assert.assertEquals("threads come and go while the table is being walked, so one that ended"
            + " between being listed and being read has to be left out rather than costing the reader"
            + " the whole reading including the render thread. Actual: " + threads.size(),
            1, threads.size());
        Assert.assertNotNull("the threads that were still alive have to survive the one that was not",
            threadNamed(threads, "RenderThread"));
    }

    @Test
    public void aThreadTableThatCannotBeListedFailsRatherThanReportingNoThreads() {
        File missingDirectory = new File(mTaskDirectory, "absent");

        try {
            new ProcFileSystemThreadTable(missingDirectory).threads();
            Assert.fail("an unlistable table reported as an empty list would say the process is running"
                + " no render thread, which reads as a measurement rather than as a failure to measure");
        } catch (IllegalStateException expected) {
            Assert.assertTrue("the failure has to name the path that could not be listed so the report"
                    + " states why the reading is missing. Actual: " + expected.getMessage(),
                expected.getMessage().contains(missingDirectory.getPath()));
        }
    }

    @Test
    public void statisticsThatEndBeforeTheProcessorTimeFailRatherThanReportingNoProcessorTime()
        throws IOException {
        givenThreadWithStatistics("2481", "2481 (RenderThread) S 1 2 3\n");

        try {
            threadTable().threads();
            Assert.fail("a truncated line accepted as zero processor time would report an idle render"
                + " thread, which is the opposite of what a saturated one looks like");
        } catch (IllegalStateException expected) {
            Assert.assertTrue("the failure has to name the thread whose statistics could not be read."
                + " Actual: " + expected.getMessage(), expected.getMessage().contains("2481"));
        }
    }
}
