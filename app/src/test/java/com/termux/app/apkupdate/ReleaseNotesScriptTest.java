package com.termux.app.apkupdate;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ReleaseNotesScriptTest {

    private static final String SCRIPT_RELATIVE_PATH = "../scripts/generate-release-notes.sh";
    private static final String TAG_INITIAL = "v1.0.0";
    private static final String TAG_SECOND = "v1.1.0";
    private static final String MISSING_TAG = "v9.9.9-missing";
    private static final String HEAD_REF = "HEAD";
    private static final long FIRST_COMMIT_EPOCH_SECONDS = 1700000000L;

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private File repositoryDirectory;
    private long nextCommitEpochSeconds;

    @Before
    public void createRepositoryWithHistory() throws IOException, InterruptedException {
        repositoryDirectory = temporaryFolder.newFolder("repo");
        nextCommitEpochSeconds = FIRST_COMMIT_EPOCH_SECONDS;

        runGit("init", "-q", "-b", "main");
        runGit("config", "user.name", "HiromiShikata");
        runGit("config", "user.email", "shikata.hiromi@gmail.com");
        runGit("config", "commit.gpgsign", "false");

        commit("Initial release");
        runGit("tag", TAG_INITIAL);
        commit("Add alpha");
        commit("Fix beta");
        commit("Refactor gamma");
        runGit("tag", TAG_SECOND);

        runGit("checkout", "-q", "-b", "feature");
        commit("Add delta");
        runGit("checkout", "-q", "main");
        commit("Fix epsilon");
        mergeNoFastForward("feature", "Merge feature into main");
    }

    private static final class ScriptCase {
        final String description;
        final String previousTag;
        final String headRef;
        final List<String> expectedLines;

        ScriptCase(String description, String previousTag, String headRef, List<String> expectedLines) {
            this.description = description;
            this.previousTag = previousTag;
            this.headRef = headRef;
            this.expectedLines = expectedLines;
        }
    }

    private static final ScriptCase[] SCRIPT_CASES = {
        new ScriptCase(
            "lists non-merge commits after previous tag oldest first and omits merge commits",
            TAG_INITIAL, HEAD_REF,
            Arrays.asList("- Add alpha", "- Fix beta", "- Refactor gamma", "- Add delta", "- Fix epsilon")),
        new ScriptCase(
            "lists only commits after a later previous tag",
            TAG_SECOND, HEAD_REF,
            Arrays.asList("- Add delta", "- Fix epsilon")),
        new ScriptCase(
            "prints only the head subject when previous tag is empty",
            "", TAG_SECOND,
            Collections.singletonList("- Refactor gamma")),
        new ScriptCase(
            "prints nothing when previous tag and head resolve to the same commit",
            TAG_SECOND, TAG_SECOND,
            Collections.<String>emptyList()),
    };

    @Test
    public void printsExpectedChangeLinesForEachRange() throws IOException, InterruptedException {
        for (ScriptCase scriptCase : SCRIPT_CASES) {
            ScriptResult result = runScript(scriptCase.previousTag, scriptCase.headRef);

            Assert.assertEquals(scriptCase.description + " stderr=" + result.stderr,
                0, result.exitCode);
            Assert.assertEquals(scriptCase.description, scriptCase.expectedLines,
                toLines(result.stdout));
        }
    }

    @Test
    public void exitsNonZeroWithStderrMessageWhenPreviousTagDoesNotResolve()
            throws IOException, InterruptedException {
        ScriptResult result = runScript(MISSING_TAG, HEAD_REF);

        Assert.assertNotEquals("stdout=" + result.stdout, 0, result.exitCode);
        Assert.assertFalse("stderr must describe the failure", result.stderr.trim().isEmpty());
    }

    private void commit(String subject) throws IOException, InterruptedException {
        String epoch = String.valueOf(nextCommitEpochSeconds) + " +0000";
        nextCommitEpochSeconds++;
        runGitWithEnv(
            new String[] {"GIT_AUTHOR_DATE=" + epoch, "GIT_COMMITTER_DATE=" + epoch},
            "commit", "-q", "--allow-empty", "-m", subject);
    }

    private void mergeNoFastForward(String branch, String subject)
            throws IOException, InterruptedException {
        String epoch = String.valueOf(nextCommitEpochSeconds) + " +0000";
        nextCommitEpochSeconds++;
        runGitWithEnv(
            new String[] {"GIT_AUTHOR_DATE=" + epoch, "GIT_COMMITTER_DATE=" + epoch},
            "merge", "-q", "--no-ff", "-m", subject, branch);
    }

    private void runGit(String... arguments) throws IOException, InterruptedException {
        runGitWithEnv(new String[0], arguments);
    }

    private void runGitWithEnv(String[] environmentAssignments, String... arguments)
            throws IOException, InterruptedException {
        List<String> command = new ArrayList<>();
        command.add("git");
        command.addAll(Arrays.asList(arguments));
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.directory(repositoryDirectory);
        for (String assignment : environmentAssignments) {
            int separator = assignment.indexOf('=');
            builder.environment().put(assignment.substring(0, separator), assignment.substring(separator + 1));
        }
        File stderrFile = temporaryFolder.newFile();
        builder.redirectError(stderrFile);
        builder.redirectOutput(temporaryFolder.newFile());
        Process process = builder.start();
        Assert.assertTrue("git timed out: " + command, process.waitFor(60, TimeUnit.SECONDS));
        Assert.assertEquals("git failed: " + command + " stderr="
                + new String(Files.readAllBytes(stderrFile.toPath()), StandardCharsets.UTF_8),
            0, process.exitValue());
    }

    private ScriptResult runScript(String previousTag, String headRef)
            throws IOException, InterruptedException {
        File scriptFile = new File(SCRIPT_RELATIVE_PATH).getAbsoluteFile();
        File stdoutFile = temporaryFolder.newFile();
        File stderrFile = temporaryFolder.newFile();
        ProcessBuilder builder = new ProcessBuilder("bash", scriptFile.getAbsolutePath(), previousTag, headRef);
        builder.directory(repositoryDirectory);
        builder.redirectOutput(stdoutFile);
        builder.redirectError(stderrFile);
        Process process = builder.start();
        Assert.assertTrue("script timed out", process.waitFor(60, TimeUnit.SECONDS));
        return new ScriptResult(
            process.exitValue(),
            new String(Files.readAllBytes(stdoutFile.toPath()), StandardCharsets.UTF_8),
            new String(Files.readAllBytes(stderrFile.toPath()), StandardCharsets.UTF_8));
    }

    private static List<String> toLines(String output) {
        String withoutTrailingNewline = output.endsWith("\n")
            ? output.substring(0, output.length() - 1)
            : output;
        if (withoutTrailingNewline.isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.asList(withoutTrailingNewline.split("\n", -1));
    }

    private static final class ScriptResult {
        final int exitCode;
        final String stdout;
        final String stderr;

        ScriptResult(int exitCode, String stdout, String stderr) {
            this.exitCode = exitCode;
            this.stdout = stdout;
            this.stderr = stderr;
        }
    }
}
