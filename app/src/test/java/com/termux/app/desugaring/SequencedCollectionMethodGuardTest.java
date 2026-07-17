package com.termux.app.desugaring;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SequencedCollectionMethodGuardTest {

    private static final Pattern FORBIDDEN_METHOD_CALL = Pattern.compile(
        "\\.\\s*(addFirst|addLast|getFirst|getLast|removeFirst|removeLast|reversed)\\s*\\(");

    private static final List<String> SCANNED_MAIN_SOURCE_ROOTS = List.of(
        "app/src/main/java",
        "termux-shared/src/main/java");

    @Test
    public void mainSourceContainsNoJava21SequencedCollectionMethodCalls() throws IOException {
        Path repositoryRoot = locateRepositoryRoot();
        List<String> offendingCallSites = new ArrayList<>();
        for (String sourceRoot : SCANNED_MAIN_SOURCE_ROOTS) {
            Path root = repositoryRoot.resolve(sourceRoot);
            Assert.assertTrue(
                "Scanned main source root does not exist: " + root,
                Files.isDirectory(root));
            collectOffendingCallSites(root, offendingCallSites);
        }
        if (!offendingCallSites.isEmpty()) {
            Assert.fail(buildFailureMessage(offendingCallSites));
        }
    }

    private void collectOffendingCallSites(Path sourceRoot, List<String> offendingCallSites)
            throws IOException {
        List<Path> javaFiles;
        try (Stream<Path> paths = Files.walk(sourceRoot)) {
            javaFiles = paths
                .filter(Files::isRegularFile)
                .filter(path -> path.getFileName().toString().endsWith(".java"))
                .sorted()
                .collect(Collectors.toList());
        }
        for (Path javaFile : javaFiles) {
            List<String> lines = Files.readAllLines(javaFile, StandardCharsets.UTF_8);
            for (int lineIndex = 0; lineIndex < lines.size(); lineIndex++) {
                String line = lines.get(lineIndex);
                Matcher matcher = FORBIDDEN_METHOD_CALL.matcher(line);
                while (matcher.find()) {
                    offendingCallSites.add(
                        javaFile + ":" + (lineIndex + 1)
                            + "  ->  ." + matcher.group(1) + "(  in:  " + line.trim());
                }
            }
        }
    }

    private Path locateRepositoryRoot() {
        Path current = Paths.get("").toAbsolutePath();
        while (current != null) {
            if (Files.exists(current.resolve("settings.gradle"))) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException(
            "Could not locate the repository root (a directory containing settings.gradle) starting from "
                + Paths.get("").toAbsolutePath());
    }

    private String buildFailureMessage(List<String> offendingCallSites) {
        StringBuilder message = new StringBuilder();
        message.append("Java 21 SequencedCollection methods are forbidden in app and termux-shared main source.\n");
        message.append("These methods (addFirst, addLast, getFirst, getLast, removeFirst, removeLast, reversed) ");
        message.append("compile and pass JVM unit tests, but the release APK's desugared java.util library ");
        message.append("(j$.util, produced by coreLibraryDesugaring) does NOT implement the Java 21 ");
        message.append("SequencedCollection interface, so every such call throws NoSuchMethodError at runtime ");
        message.append("on the device even though tests are green ");
        message.append("(recent incident: addFirst crashed on every message send).\n");
        message.append("Replace each call with its classic pre-Java-21 equivalent:\n");
        message.append("    addFirst(x)   -> add(0, x)\n");
        message.append("    addLast(x)    -> add(x)\n");
        message.append("    getFirst()    -> get(0)\n");
        message.append("    getLast()     -> get(size() - 1)\n");
        message.append("    removeFirst() -> remove(0)\n");
        message.append("    removeLast()  -> remove(size() - 1)\n");
        message.append("For a Deque, ArrayDeque or LinkedList receiver, use the Deque-native forms that avoid ");
        message.append("these names instead (for example add(x), remove(), pollFirst(), pollLast(), ");
        message.append("peekFirst(), peekLast()).\n");
        message.append("Offending call sites (").append(offendingCallSites.size()).append("):\n");
        for (String offendingCallSite : offendingCallSites) {
            message.append("    ").append(offendingCallSite).append("\n");
        }
        return message.toString();
    }
}
