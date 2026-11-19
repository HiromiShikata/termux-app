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
import java.util.concurrent.TimeUnit;

public class ReleaseBodyScriptTest {

    private static final String SCRIPT_RELATIVE_PATH = "../scripts/generate-release-body.sh";
    private static final String TAG_INITIAL = "v1.0.0";
    private static final String TAG_CURRENT = "v1.1.0";
    private static final String TAG_NEXT = "v1.2.0";
    private static final String MISSING_TAG = "v9.9.9-missing";
    private static final String VERSION_NAME = "0.120.0";
    private static final String BUILD_NUMBER = "4241";
    private static final String INSTALL_HINT_LINE =
        "Install the APK matching your device ABI; termux-app_arm64-v8a.apk is recommended. "
            + "termux-app_universal.apk works on all ABIs. "
            + "The in-app updater reads this release via the anonymous releases/latest API.";

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private File repositoryDirectory;

    @Before
    public void createRepositoryWithTags() throws IOException, InterruptedException {
        repositoryDirectory = temporaryFolder.newFolder("repo");

        runGit("init", "-q", "-b", "main");
        runGit("config", "user.name", "HiromiShikata");
        runGit("config", "user.email", "shikata.hiromi@gmail.com");
        runGit("config", "commit.gpgsign", "false");

        runGit("commit", "-q", "--allow-empty", "-m", "Initial release");
        runGit("tag", TAG_INITIAL);
        runGit("commit", "-q", "--allow-empty", "-m", "Add alpha");
        runGit("commit", "-q", "--allow-empty", "-m", "Fix beta");
        runGit("tag", TAG_CURRENT);
    }

    private static final class BodyCase {
        final String description;
        final String currentTag;
        final String recentTagsContent;
        final String heading;
        final String changesText;

        BodyCase(String description, String currentTag, String recentTagsContent,
                 String heading, String changesText) {
            this.description = description;
            this.currentTag = currentTag;
            this.recentTagsContent = recentTagsContent;
            this.heading = heading;
            this.changesText = changesText;
        }
    }

    private static final BodyCase[] BODY_CASES = {
        new BodyCase(
            "previous tag is the first tag after the current tag, which is skipped",
            TAG_CURRENT,
            TAG_CURRENT + "\n\n" + TAG_INITIAL + "\n",
            "## Changes since " + TAG_INITIAL,
            "- Add alpha\n- Fix beta\n"),
        new BodyCase(
            "only the current tag exists, so this is the first release",
            TAG_CURRENT,
            TAG_CURRENT + "\n",
            "## Changes since the first release",
            "Fix beta\n"),
        new BodyCase(
            "previous tag points at the head commit, so there are no changes",
            TAG_NEXT,
            TAG_NEXT + "\n" + TAG_CURRENT + "\n",
            "## Changes since " + TAG_CURRENT,
            "No changes since the previous release.\n"),
    };

    @Test
    public void printsReleaseBodyForEachRecentTagsCase() throws IOException, InterruptedException {
        String headSha = runGit("rev-parse", "HEAD").trim();

        for (BodyCase bodyCase : BODY_CASES) {
            ScriptResult result = runScript(bodyCase.currentTag, headSha,
                writeRecentTags(bodyCase.recentTagsContent));

            Assert.assertEquals(bodyCase.description + " stderr=" + result.stderr,
                0, result.exitCode);
            Assert.assertEquals(bodyCase.description, expectedBody(headSha, bodyCase), result.stdout);
        }
    }

    private static String expectedBody(String headSha, BodyCase bodyCase) {
        return "Automated build of main at " + headSha.substring(0, 7)
            + " (version " + VERSION_NAME + ", build " + BUILD_NUMBER + ").\n"
            + "\n"
            + INSTALL_HINT_LINE + "\n"
            + "\n"
            + bodyCase.heading + "\n"
            + "\n"
            + bodyCase.changesText;
    }

    private static final class FailureCase {
        final String description;
        final String currentTag;
        final String recentTagsContent;

        FailureCase(String description, String currentTag, String recentTagsContent) {
            this.description = description;
            this.currentTag = currentTag;
            this.recentTagsContent = recentTagsContent;
        }
    }

    private static final FailureCase[] FAILURE_CASES = {
        new FailureCase("recent tags file is missing", TAG_CURRENT, null),
        new FailureCase(
            "previous tag in the recent tags file does not resolve in the repository",
            TAG_CURRENT,
            MISSING_TAG + "\n"),
    };

    @Test
    public void exitsNonZeroWithStderrMessageForEachFailureCase()
            throws IOException, InterruptedException {
        String headSha = runGit("rev-parse", "HEAD").trim();

        for (FailureCase failureCase : FAILURE_CASES) {
            ScriptResult result = runScript(failureCase.currentTag, headSha,
                writeRecentTags(failureCase.recentTagsContent));

            Assert.assertNotEquals(failureCase.description + " stdout=" + result.stdout,
                0, result.exitCode);
            Assert.assertFalse(failureCase.description + " must describe the failure on stderr",
                result.stderr.trim().isEmpty());
        }
    }

    private File writeRecentTags(String content) throws IOException {
        if (content == null) {
            return new File(temporaryFolder.getRoot(), "missing-recent-tags.txt");
        }
        File recentTagsFile = temporaryFolder.newFile();
        Files.write(recentTagsFile.toPath(), content.getBytes(StandardCharsets.UTF_8));
        return recentTagsFile;
    }

    private ScriptResult runScript(String currentTag, String commitSha, File recentTagsFile)
            throws IOException, InterruptedException {
        File scriptFile = new File(SCRIPT_RELATIVE_PATH).getAbsoluteFile();
        File stdoutFile = temporaryFolder.newFile();
        File stderrFile = temporaryFolder.newFile();
        ProcessBuilder builder = new ProcessBuilder(
            "bash",
            scriptFile.getAbsolutePath(),
            currentTag,
            commitSha,
            VERSION_NAME,
            BUILD_NUMBER,
            recentTagsFile.getAbsolutePath());
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

    private String runGit(String... arguments) throws IOException, InterruptedException {
        String[] command = new String[arguments.length + 1];
        command[0] = "git";
        System.arraycopy(arguments, 0, command, 1, arguments.length);
        File outputFile = temporaryFolder.newFile();
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.directory(repositoryDirectory);
        builder.redirectOutput(outputFile);
        builder.redirectErrorStream(true);
        Process process = builder.start();
        Assert.assertTrue("git timed out", process.waitFor(60, TimeUnit.SECONDS));
        String output = new String(Files.readAllBytes(outputFile.toPath()), StandardCharsets.UTF_8);
        Assert.assertEquals("git " + String.join(" ", arguments) + " output=" + output,
            0, process.exitValue());
        return output;
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
