package com.termux.app.diagnostics;

import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class AStallIsAttributedToWhatHeldItTest {

    private static final long STALL_THRESHOLD_MILLIS = 250L;

    private static final String PATH_HELD_FIRST = "parseShellOutput";

    private static final String PATH_HELD_SECOND = "writeSessionState";

    private static StackTraceElement[] stackNaming(String methodName) {
        return new StackTraceElement[]{
            new StackTraceElement("com.termux.app.Blocking", methodName, "Blocking.java", 42),
            new StackTraceElement("android.os.Looper", "loop", "Looper.java", 223),
        };
    }

    private static long blockedMillisOf(List<MainThreadStallHotPath> hotPaths, String methodName) {
        for (MainThreadStallHotPath hotPath : hotPaths) {
            if (hotPath.getStackTrace().contains(methodName)) {
                return hotPath.getTotalBlockedMillis();
            }
        }
        return 0L;
    }

    private static MainThreadStallRecorder recorderDrivenThroughAStallHeldByTwoPaths() {
        MainThreadStallRecorder recorder = new MainThreadStallRecorder(STALL_THRESHOLD_MILLIS);
        recorder.heartbeatPosted(1000L);
        recorder.sampleWhileOutstanding(1300L, stackNaming(PATH_HELD_FIRST));
        recorder.sampleWhileOutstanding(1700L, stackNaming(PATH_HELD_SECOND));
        recorder.heartbeatRan(2200L);
        return recorder;
    }

    @Test
    public void aStallHeldByTwoPathsInTurnAttributesItsTimeToBoth() {
        List<MainThreadStallHotPath> hotPaths =
            recorderDrivenThroughAStallHeldByTwoPaths().getHotPathsByTotalBlockedMillis();

        Assert.assertEquals("the path the main thread was in until the next sample must carry the time up to"
                + " that sample, because crediting the whole stall to one sample names work that did not"
                + " cost what the report says it cost",
            700L, blockedMillisOf(hotPaths, PATH_HELD_FIRST));
        Assert.assertEquals("the path the main thread moved to must carry the rest of the stall, because it"
                + " is what held the main thread until it answered",
            500L, blockedMillisOf(hotPaths, PATH_HELD_SECOND));
    }

    @Test
    public void theTimeAttributedAcrossAStallAddsUpToTheWholeStall() {
        List<MainThreadStallHotPath> hotPaths =
            recorderDrivenThroughAStallHeldByTwoPaths().getHotPathsByTotalBlockedMillis();

        long attributed = 0L;
        for (MainThreadStallHotPath hotPath : hotPaths) {
            attributed += hotPath.getTotalBlockedMillis();
        }

        Assert.assertEquals("every millisecond the main thread was blocked must be attributed to some path,"
                + " so the ranking is a division of the stall rather than a sample of it",
            1200L, attributed);
    }

    @Test
    public void aPathSeenTwiceInOneStallIsStillOneStallForThatPath() {
        MainThreadStallRecorder recorder = new MainThreadStallRecorder(STALL_THRESHOLD_MILLIS);
        recorder.heartbeatPosted(1000L);
        recorder.sampleWhileOutstanding(1300L, stackNaming(PATH_HELD_FIRST));
        recorder.sampleWhileOutstanding(1700L, stackNaming(PATH_HELD_SECOND));
        recorder.sampleWhileOutstanding(2000L, stackNaming(PATH_HELD_FIRST));
        recorder.heartbeatRan(2400L);

        List<MainThreadStallHotPath> hotPaths = recorder.getHotPathsByTotalBlockedMillis();

        for (MainThreadStallHotPath hotPath : hotPaths) {
            if (hotPath.getStackTrace().contains(PATH_HELD_FIRST)) {
                Assert.assertEquals("a path the main thread returned to inside one stall was blocked once,"
                        + " not twice, so counting samples would overstate how often it blocks",
                    1L, hotPath.getStallCount());
                return;
            }
        }
        Assert.fail("the path the main thread was in must appear in the ranking");
    }

    @Test
    public void theLongestStallNamesThePathThatHeldItLongest() {
        MainThreadStallRecorder recorder = recorderDrivenThroughAStallHeldByTwoPaths();

        Assert.assertTrue("the stack shown for the longest stall must be the one that held the main thread"
                + " longest during it, because that is the work a reader has to remove: "
                + recorder.getMaxStallStackTrace(),
            recorder.getMaxStallStackTrace().contains(PATH_HELD_FIRST));
    }
}
