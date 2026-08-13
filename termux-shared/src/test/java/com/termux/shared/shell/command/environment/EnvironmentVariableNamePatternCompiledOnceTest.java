package com.termux.shared.shell.command.environment;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;

public class EnvironmentVariableNamePatternCompiledOnceTest {

    private static final String SOURCE_PATH =
        "termux-shared/src/main/java/com/termux/shared/shell/command/environment/ShellEnvironmentUtils.java";

    private String readSource() throws IOException {
        Path fromModule = Paths.get("..").resolve(SOURCE_PATH);
        if (Files.exists(fromModule)) {
            return new String(Files.readAllBytes(fromModule), StandardCharsets.UTF_8);
        }
        return new String(Files.readAllBytes(Paths.get(SOURCE_PATH)), StandardCharsets.UTF_8);
    }

    @Test
    public void theEnvironmentVariableNamePatternIsCompiledOnceForTheProcess() throws IOException {
        Assert.assertTrue("session creation validates every environment variable name on the main"
                + " thread, so the pattern behind that validation has to be compiled once and held,"
                + " not rebuilt per name",
            readSource().contains("private static final Pattern "));
    }

    @Test
    public void validatingAnEnvironmentVariableNameCompilesNoPattern() throws IOException {
        String source = readSource();
        int start = source.indexOf("public static boolean isValidEnvironmentVariableName(");
        Assert.assertTrue("the environment variable name validation is the path the main thread stall"
                + " was sampled in", start >= 0);
        String body = source.substring(start, source.indexOf("\n    }", start));

        Assert.assertFalse("String.matches compiles a regular expression and throws it away on every"
                + " call, and this call runs once per environment variable of every session created,"
                + " which was measured blocking the main thread for 926 ms in one run",
            body.contains(".matches(\""));
    }

    @Test
    public void everyValidNameSurvivesAndEveryInvalidNameIsDroppedAcrossAWholeEnvironment() {
        HashMap<String, String> environment = new HashMap<>();
        for (int index = 0; index < 200; index++) {
            environment.put("VALID_NAME_" + index, "value" + index);
            environment.put(index + "INVALID_LEADING_DIGIT", "value");
            environment.put("INVALID-DASH-" + index, "value");
        }

        List<String> environ = ShellEnvironmentUtils.convertEnvironmentToEnviron(environment);

        Assert.assertEquals(200, environ.size());
        Assert.assertTrue(environ.contains("VALID_NAME_0=value0"));
        Assert.assertTrue(environ.contains("VALID_NAME_199=value199"));
    }
}
