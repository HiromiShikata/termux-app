package com.termux.ci;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class WorkflowTriggerConcurrencyScanner {

    private static final String EVENT_NAME_EXPRESSION = "github.event_name";
    private static final String PUSH_EVENT = "push";
    private static final String PULL_REQUEST_EVENT = "pull_request";
    private static final String BRANCHES_FILTER = "branches";

    private WorkflowTriggerConcurrencyScanner() {
    }

    public static File findWorkflowDirectory(File startDirectory) {
        File directory = startDirectory.getAbsoluteFile();
        while (directory != null) {
            File candidate = new File(new File(directory, ".github"), "workflows");
            if (candidate.isDirectory()) {
                return candidate;
            }
            directory = directory.getParentFile();
        }
        throw new IllegalStateException("No .github/workflows directory found at or above "
            + startDirectory.getAbsolutePath());
    }

    public static List<String> findViolations(File workflowDirectory) throws IOException {
        List<String> violations = new ArrayList<>();
        for (File workflowFile : listWorkflowFiles(workflowDirectory)) {
            List<String> lines = Files.readAllLines(workflowFile.toPath(), StandardCharsets.UTF_8);
            Map<String, Map<String, List<String>>> triggers = readTriggers(lines);
            String concurrencyGroup = readConcurrencyGroup(lines);
            if (triggers.size() > 1 && concurrencyGroup != null
                && !concurrencyGroup.contains(EVENT_NAME_EXPRESSION)) {
                violations.add(workflowFile.getName() + " triggers on " + triggers.keySet()
                    + " and its concurrency group '" + concurrencyGroup + "' does not contain "
                    + EVENT_NAME_EXPRESSION
                    + ", so runs raised by different events for one branch cancel each other");
            }
            if (triggers.containsKey(PUSH_EVENT) && triggers.containsKey(PULL_REQUEST_EVENT)) {
                List<String> pushBranches = triggers.get(PUSH_EVENT).get(BRANCHES_FILTER);
                if (pushBranches == null || pushBranches.isEmpty() || containsWildcard(pushBranches)) {
                    violations.add(workflowFile.getName()
                        + " triggers on both push and pull_request while its push branch filter is "
                        + (pushBranches == null || pushBranches.isEmpty() ? "unrestricted" : pushBranches)
                        + ", so a pull request head commit is built by two runs that report check runs"
                        + " of the same name");
                }
            }
        }
        return violations;
    }

    private static List<File> listWorkflowFiles(File workflowDirectory) {
        File[] entries = workflowDirectory.listFiles();
        if (entries == null) {
            throw new IllegalStateException("Cannot list " + workflowDirectory.getAbsolutePath());
        }
        List<File> workflowFiles = new ArrayList<>();
        for (File entry : entries) {
            if (entry.isFile() && (entry.getName().endsWith(".yml") || entry.getName().endsWith(".yaml"))) {
                workflowFiles.add(entry);
            }
        }
        workflowFiles.sort((first, second) -> first.getName().compareTo(second.getName()));
        return workflowFiles;
    }

    private static Map<String, Map<String, List<String>>> readTriggers(List<String> lines) {
        Map<String, Map<String, List<String>>> triggers = new LinkedHashMap<>();
        boolean insideTriggerBlock = false;
        String currentEvent = null;
        String currentFilter = null;
        for (String line : lines) {
            if (isIgnorable(line)) {
                continue;
            }
            if (isTopLevelKey(line)) {
                insideTriggerBlock = line.startsWith("on:");
                currentEvent = null;
                currentFilter = null;
                continue;
            }
            if (!insideTriggerBlock) {
                continue;
            }
            int indent = indentOf(line);
            if (indent == 2 && keyOf(line) != null) {
                currentEvent = keyOf(line);
                currentFilter = null;
                triggers.put(currentEvent, new LinkedHashMap<>());
            } else if (indent == 4 && currentEvent != null && keyOf(line) != null) {
                currentFilter = keyOf(line);
                triggers.get(currentEvent).put(currentFilter, inlineListOf(valueOf(line)));
            } else if (indent == 6 && currentFilter != null && line.trim().startsWith("- ")) {
                triggers.get(currentEvent).get(currentFilter).add(unquote(line.trim().substring(2)));
            }
        }
        return triggers;
    }

    private static String readConcurrencyGroup(List<String> lines) {
        boolean insideConcurrencyBlock = false;
        for (String line : lines) {
            if (isIgnorable(line)) {
                continue;
            }
            if (isTopLevelKey(line)) {
                insideConcurrencyBlock = line.startsWith("concurrency:");
                continue;
            }
            if (insideConcurrencyBlock && indentOf(line) == 2 && "group".equals(keyOf(line))) {
                return valueOf(line);
            }
        }
        return null;
    }

    private static List<String> inlineListOf(String value) {
        List<String> values = new ArrayList<>();
        if (value.startsWith("[") && value.endsWith("]")) {
            for (String element : value.substring(1, value.length() - 1).split(",")) {
                if (!element.trim().isEmpty()) {
                    values.add(unquote(element));
                }
            }
        }
        return values;
    }

    private static boolean containsWildcard(List<String> branchPatterns) {
        for (String branchPattern : branchPatterns) {
            if (branchPattern.contains("*") || branchPattern.contains("?") || branchPattern.contains("[")) {
                return true;
            }
        }
        return false;
    }

    private static boolean isIgnorable(String line) {
        return line.trim().isEmpty() || line.trim().startsWith("#");
    }

    private static boolean isTopLevelKey(String line) {
        return !line.startsWith(" ") && !line.startsWith("-") && line.contains(":");
    }

    private static int indentOf(String line) {
        int indent = 0;
        while (indent < line.length() && line.charAt(indent) == ' ') {
            indent++;
        }
        return indent;
    }

    private static String keyOf(String line) {
        String trimmed = line.trim();
        int separator = trimmed.indexOf(':');
        if (separator <= 0) {
            return null;
        }
        String key = trimmed.substring(0, separator);
        return key.matches("[A-Za-z_][A-Za-z0-9_-]*") ? key : null;
    }

    private static String valueOf(String line) {
        String trimmed = line.trim();
        return trimmed.substring(trimmed.indexOf(':') + 1).trim();
    }

    private static String unquote(String value) {
        String trimmed = value.trim();
        for (String quote : Arrays.asList("'", "\"")) {
            if (trimmed.length() >= 2 && trimmed.startsWith(quote) && trimmed.endsWith(quote)) {
                return trimmed.substring(1, trimmed.length() - 1);
            }
        }
        return trimmed;
    }
}
