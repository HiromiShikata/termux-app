package com.termux.ci;

import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class WorkflowTriggerConcurrencyScanner {

    private static final String EVENT_NAME_EXPRESSION = "github.event_name";
    private static final String REFERENCE_EXPRESSION = "github.ref";
    private static final String HEAD_REFERENCE_EXPRESSION = "github.head_ref";
    private static final String PUSH_EVENT = "push";
    private static final String PULL_REQUEST_EVENT = "pull_request";
    private static final String BRANCHES_FILTER = "branches";
    private static final String TRIGGER_KEY = "on";
    private static final String TRIGGER_KEY_PARSED_AS_YAML_BOOLEAN = "true";
    private static final String CONCURRENCY_KEY = "concurrency";
    private static final String GROUP_KEY = "group";
    private static final List<String> WILDCARD_CHARACTERS = Arrays.asList("*", "?", "[");

    private WorkflowTriggerConcurrencyScanner() {
    }

    public static final class Scan {

        private final Map<String, List<String>> triggerEventsByWorkflow;
        private final List<String> violations;

        private Scan(Map<String, List<String>> triggerEventsByWorkflow, List<String> violations) {
            this.triggerEventsByWorkflow = triggerEventsByWorkflow;
            this.violations = violations;
        }

        public List<String> getScannedWorkflows() {
            return new ArrayList<>(triggerEventsByWorkflow.keySet());
        }

        public List<String> getTriggerEvents(String workflowFileName) {
            List<String> triggerEvents = triggerEventsByWorkflow.get(workflowFileName);
            return triggerEvents == null ? Collections.<String>emptyList() : triggerEvents;
        }

        public List<String> getViolations() {
            return violations;
        }

        public String describeViolations() {
            return String.join("\n", violations);
        }
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

    public static Scan scan(File workflowDirectory) throws IOException {
        Map<String, List<String>> triggerEventsByWorkflow = new LinkedHashMap<>();
        List<String> violations = new ArrayList<>();
        for (File workflowFile : listWorkflowFiles(workflowDirectory)) {
            Map<String, Object> document = readDocument(workflowFile);
            Map<String, Object> triggers = readTriggers(document);
            triggerEventsByWorkflow.put(workflowFile.getName(), new ArrayList<>(triggers.keySet()));
            String concurrencyGroup = readConcurrencyGroup(document);
            if (triggers.size() > 1 && concurrencyGroup != null && !distinguishesEvents(concurrencyGroup)) {
                violations.add(workflowFile.getName() + " triggers on " + triggers.keySet()
                    + " and its concurrency group '" + concurrencyGroup
                    + "' resolves to the same value for those events, so a run raised by one event"
                    + " cancels the run raised by another for the same branch; key the group on "
                    + EVENT_NAME_EXPRESSION + ", or on " + REFERENCE_EXPRESSION + " without "
                    + HEAD_REFERENCE_EXPRESSION);
            }
            if (triggers.containsKey(PUSH_EVENT) && triggers.containsKey(PULL_REQUEST_EVENT)) {
                List<String> pushBranches = readBranchFilter(triggers.get(PUSH_EVENT));
                if (pushBranches.isEmpty() || containsWildcard(pushBranches)) {
                    violations.add(workflowFile.getName()
                        + " triggers on both push and pull_request while its push branch filter is "
                        + (pushBranches.isEmpty() ? "unrestricted" : pushBranches)
                        + ", so a pull request head commit is built by two runs that report check runs"
                        + " of the same name");
                }
            }
            if (triggers.containsKey(PUSH_EVENT) && triggers.containsKey(PULL_REQUEST_EVENT)
                && concurrencyGroup == null) {
                violations.add(workflowFile.getName()
                    + " triggers on both push and pull_request without a concurrency group,"
                    + " so a superseded push run is never cancelled");
            }
        }
        return new Scan(triggerEventsByWorkflow, violations);
    }

    public static List<String> findViolations(File workflowDirectory) throws IOException {
        return scan(workflowDirectory).getViolations();
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

    private static Map<String, Object> readDocument(File workflowFile) throws IOException {
        try (InputStream workflowStream = new FileInputStream(workflowFile)) {
            Object document = new Yaml().load(workflowStream);
            if (!(document instanceof Map)) {
                throw new IllegalStateException(workflowFile.getName() + " does not parse as a YAML mapping");
            }
            return keyedByString((Map<?, ?>) document);
        }
    }

    private static Map<String, Object> keyedByString(Map<?, ?> mapping) {
        Map<String, Object> keyed = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : mapping.entrySet()) {
            keyed.put(String.valueOf(entry.getKey()), entry.getValue());
        }
        return keyed;
    }

    private static Map<String, Object> readTriggers(Map<String, Object> document) {
        Object triggerNode = document.containsKey(TRIGGER_KEY)
            ? document.get(TRIGGER_KEY)
            : document.get(TRIGGER_KEY_PARSED_AS_YAML_BOOLEAN);
        Map<String, Object> triggers = new LinkedHashMap<>();
        if (triggerNode instanceof Map) {
            triggers.putAll(keyedByString((Map<?, ?>) triggerNode));
        } else if (triggerNode instanceof List) {
            for (Object triggerEvent : (List<?>) triggerNode) {
                triggers.put(String.valueOf(triggerEvent), null);
            }
        } else if (triggerNode != null) {
            triggers.put(String.valueOf(triggerNode), null);
        }
        return triggers;
    }

    private static String readConcurrencyGroup(Map<String, Object> document) {
        Object concurrencyNode = document.get(CONCURRENCY_KEY);
        if (concurrencyNode instanceof Map) {
            Object concurrencyGroup = keyedByString((Map<?, ?>) concurrencyNode).get(GROUP_KEY);
            return concurrencyGroup == null ? null : String.valueOf(concurrencyGroup);
        }
        return concurrencyNode == null ? null : String.valueOf(concurrencyNode);
    }

    private static List<String> readBranchFilter(Object triggerEventNode) {
        List<String> branchPatterns = new ArrayList<>();
        if (!(triggerEventNode instanceof Map)) {
            return branchPatterns;
        }
        Object branchNode = keyedByString((Map<?, ?>) triggerEventNode).get(BRANCHES_FILTER);
        if (branchNode instanceof List) {
            for (Object branchPattern : (List<?>) branchNode) {
                branchPatterns.add(String.valueOf(branchPattern));
            }
        } else if (branchNode != null) {
            branchPatterns.add(String.valueOf(branchNode));
        }
        return branchPatterns;
    }

    private static boolean distinguishesEvents(String concurrencyGroup) {
        if (concurrencyGroup.contains(EVENT_NAME_EXPRESSION)) {
            return true;
        }
        return concurrencyGroup.contains(REFERENCE_EXPRESSION)
            && !concurrencyGroup.contains(HEAD_REFERENCE_EXPRESSION);
    }

    private static boolean containsWildcard(List<String> branchPatterns) {
        for (String branchPattern : branchPatterns) {
            for (String wildcardCharacter : WILDCARD_CHARACTERS) {
                if (branchPattern.contains(wildcardCharacter)) {
                    return true;
                }
            }
        }
        return false;
    }
}
