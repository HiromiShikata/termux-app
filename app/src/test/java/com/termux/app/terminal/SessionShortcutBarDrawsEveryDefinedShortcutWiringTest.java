package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * The shortcut bar is planned from the session definition and from the names the owner pinned to the
 * not-applicable group, so every planned shortcut is a shortcut the configuration asks for. While the
 * bar rendered only the shortcuts whose target session already existed, a shortcut appeared for the
 * first time only after its session had been opened once by some other route, which is local runtime
 * state deciding whether a configured shortcut exists at all.
 */
public class SessionShortcutBarDrawsEveryDefinedShortcutWiringTest {

    private static final String CONTROLLER_RELATIVE_PATH =
        "src/main/java/com/termux/app/terminal/SessionListBottomSheetController.java";

    private String readSource(String relativePath) throws IOException {
        Path moduleRelative = Paths.get(relativePath);
        if (Files.exists(moduleRelative)) {
            return new String(Files.readAllBytes(moduleRelative), StandardCharsets.UTF_8);
        }
        Path repoRelative = Paths.get("app").resolve(relativePath);
        return new String(Files.readAllBytes(repoRelative), StandardCharsets.UTF_8);
    }

    private String methodBody(String source, String methodDeclaration) {
        int methodIndex = source.indexOf(methodDeclaration);
        Assert.assertTrue("the source must declare " + methodDeclaration, methodIndex >= 0);
        int methodEnd = source.indexOf("\n    }", methodIndex);
        Assert.assertTrue("the declaration of " + methodDeclaration + " must be closed", methodEnd > methodIndex);
        return source.substring(methodIndex, methodEnd);
    }

    @Test
    public void everyPlannedShortcutIsRenderedWhetherOrNotItsSessionAlreadyExists() throws IOException {
        String source = readSource(CONTROLLER_RELATIVE_PATH);

        String rebuildBody = methodBody(source, "private void rebuildSessionShortcuts(");
        String fillShortcutRowBody = methodBody(source, "private void fillShortcutRow(");

        Assert.assertTrue("both rows must be planned by the planner alone, so no runtime state decides"
                + " whether a configured shortcut exists. Actual body:\n" + rebuildBody,
            rebuildBody.contains("mSessionShortcutBarPlanner.planRightToLeftShortcutRows("));
        Assert.assertTrue("the upper row must be filled from the planned always-on session list."
                + " Actual body:\n" + rebuildBody,
            rebuildBody.contains("rightToLeftShortcutRows.getAlwaysSessionShortcuts()"));
        Assert.assertTrue("the lower row must be filled from the planned project manager session list."
                + " Actual body:\n" + rebuildBody,
            rebuildBody.contains("rightToLeftShortcutRows.getProjectManagerSessionShortcuts()"));
        Assert.assertTrue("the shortcut bar must render every planned shortcut, so the render list must"
                + " come from the planned list alone. Actual body:\n" + fillShortcutRowBody,
            fillShortcutRowBody.contains(
                "SessionShortcutBarPlanner.renderOrderShortcuts(rightToLeftShortcuts)"));
        Assert.assertFalse("no shortcut may be dropped because its target session does not exist yet."
                + " Actual source:\n" + source,
            source.contains("renderOrderPresentShortcuts"));
        Assert.assertFalse("the render loop must not skip a shortcut whose target session is absent."
                + " Actual source:\n" + source,
            source.contains("if (targetSession == null)"));
    }

    @Test
    public void tappingAShortcutWhoseSessionDoesNotExistOpensItFromTheSessionDefinition() throws IOException {
        String source = readSource(CONTROLLER_RELATIVE_PATH);

        Assert.assertTrue("tapping a shortcut whose target session does not exist yet must open it"
                + " through the same definition-backed path a session row uses, so the owner reaches"
                + " the session without opening it by another route first. Actual source lacked the"
                + " call.",
            source.contains("listController.openDefinitionBackedSession("));
    }
}
