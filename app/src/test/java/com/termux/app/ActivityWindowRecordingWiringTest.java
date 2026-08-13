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
    public void creatingTheActivityWindowIsCounted() throws IOException {
        String body = bodyOf("public void onCreate(Bundle savedInstanceState) {", "setContentView(");

        Assert.assertTrue("a window that is created without being counted cannot be told apart from one"
                + " that was never created, and the count of windows the process still holds is the whole"
                + " measurement: " + body,
            body.contains("ActivityWindowRecorderHolder.getInstance().recordActivityCreated()"));
    }

    @Test
    public void destroyingTheActivityWindowIsCounted() throws IOException {
        String body = bodyOf("public void onDestroy() {", "for (ActivityComponent component");

        Assert.assertTrue("without the destroyed count every recreation of the window reads as a window"
                + " the process is still holding, which would turn a healthy restart into a false leak: "
                + body,
            body.contains("ActivityWindowRecorderHolder.getInstance().recordActivityDestroyed()"));
    }

    @Test
    public void aWindowDestroyedWhileTheActivityIsInAnInvalidStateIsStillCounted() throws IOException {
        String body = bodyOf("public void onDestroy() {", "for (ActivityComponent component");

        int recordIndex = body.indexOf("recordActivityDestroyed()");
        int invalidStateReturnIndex = body.indexOf("if (mIsInvalidState) return;");
        Assert.assertTrue("the invalid-state early return must still be there: " + body,
            invalidStateReturnIndex >= 0);
        Assert.assertTrue("a window torn down from an invalid state is gone just as surely as any other,"
                + " so counting after the early return would report it as still held forever and produce"
                + " exactly the false leak this measurement exists to detect: " + body,
            recordIndex >= 0 && recordIndex < invalidStateReturnIndex);
    }

    @Test
    public void theCreationIsCountedBeforeAnythingThatCanReturnEarly() throws IOException {
        String body = bodyOf("public void onCreate(Bundle savedInstanceState) {", "setContentView(");

        int recordIndex = body.indexOf("recordActivityCreated()");
        int firstReturnIndex = body.indexOf("return;");
        Assert.assertTrue("the creation must be counted: " + body, recordIndex >= 0);
        Assert.assertTrue("a creation counted after a branch that can return leaves the window uncounted"
                + " while its views stay attached and keep posting, which is the state being measured: "
                + body,
            firstReturnIndex < 0 || recordIndex < firstReturnIndex);
    }
}
