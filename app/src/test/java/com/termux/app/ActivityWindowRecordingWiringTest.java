package com.termux.app;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ActivityWindowRecordingWiringTest {

    private static String activitySource() throws IOException {
        return new String(Files.readAllBytes(Paths.get("src/main/java/com/termux/app/TermuxActivity.java")),
            StandardCharsets.UTF_8);
    }

    private static String bodyOf(String methodSignature, String endMarker) throws IOException {
        String source = activitySource();
        int start = source.indexOf(methodSignature);
        Assert.assertTrue("The method must exist for this test to mean anything: " + methodSignature,
            start >= 0);
        int end = source.indexOf(endMarker, start);
        Assert.assertTrue("The end of the method must be locatable: " + endMarker, end > start);
        return source.substring(start, end);
    }

    @Test
    public void buildingTheActivityWindowIsCounted() throws IOException {
        String body = bodyOf("public void onCreate(Bundle savedInstanceState) {", "setContentView(");

        Assert.assertTrue("a build that is not counted cannot be told apart from one that never happened,"
                + " and the number of builds is the whole measurement: " + body,
            body.contains("ActivityWindowRecorderHolder.getInstance().recordActivityCreated()"));
    }

    @Test
    public void tearingDownTheActivityWindowIsCounted() throws IOException {
        String body = bodyOf("public void onDestroy() {", "for (ActivityComponent component");

        Assert.assertTrue("without the teardown count every rebuild of the window reads as a build whose"
                + " teardown never ran, which would turn a healthy restart into a false finding: " + body,
            body.contains("ActivityWindowRecorderHolder.getInstance().recordActivityDestroyed()"));
    }

    @Test
    public void aTeardownRunningWhileTheActivityIsInAnInvalidStateIsStillCounted() throws IOException {
        String body = bodyOf("public void onDestroy() {", "for (ActivityComponent component");

        int recordIndex = body.indexOf("recordActivityDestroyed()");
        int invalidStateReturnIndex = body.indexOf("if (mIsInvalidState) return;");
        Assert.assertTrue("the invalid-state early return must still be there: " + body,
            invalidStateReturnIndex >= 0);
        Assert.assertTrue("a window torn down from an invalid state has had its teardown run just as surely"
                + " as any other, so counting after the early return would report that teardown as never"
                + " having run, for the rest of the process: " + body,
            recordIndex >= 0 && recordIndex < invalidStateReturnIndex);
    }

    @Test
    public void theBuildIsCountedBeforeAnythingThatCanReturnEarly() throws IOException {
        String body = bodyOf("public void onCreate(Bundle savedInstanceState) {", "setContentView(");

        int recordIndex = body.indexOf("recordActivityCreated()");
        int firstReturnIndex = body.indexOf("return;");
        Assert.assertTrue("the build must be counted: " + body, recordIndex >= 0);
        Assert.assertTrue("a build counted after a branch that can return leaves the most expensive builds"
                + " — the ones that failed part way through — out of the reading: " + body,
            firstReturnIndex < 0 || recordIndex < firstReturnIndex);
    }
}
