package com.termux.ci;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

public class WorkflowTriggerConcurrencyTest {

    @Rule
    public final TemporaryFolder fixtureDirectory = new TemporaryFolder();

    private final File repositoryWorkflowDirectory =
        WorkflowTriggerConcurrencyScanner.findWorkflowDirectory(new File("."));

    @Test
    public void aPushRunAndAPullRequestRunOfTheSameBranchNeverShareOneConcurrencyGroup() throws IOException {
        WorkflowTriggerConcurrencyScanner.Scan scan =
            WorkflowTriggerConcurrencyScanner.scan(repositoryWorkflowDirectory);
        Assert.assertEquals("", scan.describeViolations());
    }

    @Test
    public void theScanReadsTheTriggersOfEveryWorkflowFileInTheRepository() throws IOException {
        WorkflowTriggerConcurrencyScanner.Scan scan =
            WorkflowTriggerConcurrencyScanner.scan(repositoryWorkflowDirectory);
        Assert.assertEquals(Arrays.asList("push", "pull_request"), scan.getTriggerEvents("debug_build.yml"));
        Assert.assertEquals(Arrays.asList("pull_request", "workflow_dispatch"),
            scan.getTriggerEvents("android_instrumentation_tests.yml"));
        for (String workflowFileName : scan.getScannedWorkflows()) {
            Assert.assertFalse(workflowFileName + " parsed with no trigger event",
                scan.getTriggerEvents(workflowFileName).isEmpty());
        }
    }

    @Test
    public void theDefectIsReportedWhenItIsWrittenInBlockStyle() throws IOException {
        writeWorkflow("block_style.yml",
            "name: Build",
            "on:",
            "  push:",
            "    branches:",
            "      - '**'",
            "  pull_request:",
            "    branches:",
            "      - '**'",
            "concurrency:",
            "  group: build-${{ github.head_ref || github.ref_name }}",
            "  cancel-in-progress: true",
            "jobs:",
            "  build:",
            "    runs-on: ubuntu-latest");

        assertReportsBothDefects("block_style.yml");
    }

    @Test
    public void theDefectIsReportedWhenTheTriggersAreWrittenInFlowStyle() throws IOException {
        writeWorkflow("flow_style.yml",
            "name: Build",
            "on: [push, pull_request]",
            "concurrency:",
            "  group: build-${{ github.head_ref || github.ref_name }}",
            "  cancel-in-progress: true",
            "jobs:",
            "  build:",
            "    runs-on: ubuntu-latest");

        assertReportsBothDefects("flow_style.yml");
    }

    @Test
    public void theDefectIsReportedWhenTheFileIsIndentedWithFourSpaces() throws IOException {
        writeWorkflow("four_space_indentation.yml",
            "name: Build",
            "on:",
            "    push:",
            "        branches:",
            "            - '**'",
            "    pull_request:",
            "        branches:",
            "            - '**'",
            "concurrency:",
            "    group: build-${{ github.head_ref || github.ref_name }}",
            "    cancel-in-progress: true",
            "jobs:",
            "    build:",
            "        runs-on: ubuntu-latest");

        assertReportsBothDefects("four_space_indentation.yml");
    }

    @Test
    public void theDefectIsReportedWhenTheTriggerKeyIsQuoted() throws IOException {
        writeWorkflow("quoted_trigger_key.yml",
            "name: Build",
            "\"on\":",
            "  push:",
            "    branches:",
            "      - '**'",
            "  pull_request:",
            "    branches:",
            "      - '**'",
            "concurrency:",
            "  group: build-${{ github.head_ref || github.ref_name }}",
            "  cancel-in-progress: true",
            "jobs:",
            "  build:",
            "    runs-on: ubuntu-latest");

        assertReportsBothDefects("quoted_trigger_key.yml");
    }

    @Test
    public void aGroupKeyedOnTheWorkflowAndTheReferenceIsAccepted() throws IOException {
        writeWorkflow("workflow_and_reference_group.yml",
            "name: Build",
            "on:",
            "  push:",
            "    branches:",
            "      - main",
            "  pull_request:",
            "    branches:",
            "      - '**'",
            "concurrency:",
            "  group: ${{ github.workflow }}-${{ github.ref }}",
            "  cancel-in-progress: true",
            "jobs:",
            "  build:",
            "    runs-on: ubuntu-latest");

        Assert.assertEquals("", scanFixtures());
    }

    @Test
    public void aGroupKeyedOnTheEventNameAndTheBranchIsAccepted() throws IOException {
        writeWorkflow("event_name_group.yml",
            "name: Build",
            "on:",
            "  push:",
            "    branches:",
            "      - main",
            "  pull_request:",
            "    branches:",
            "      - '**'",
            "concurrency:",
            "  group: build-${{ github.event_name }}-${{ github.head_ref || github.ref_name }}",
            "  cancel-in-progress: true",
            "jobs:",
            "  build:",
            "    runs-on: ubuntu-latest");

        Assert.assertEquals("", scanFixtures());
    }

    @Test
    public void theViolationIsReportedWhenPushAndPrWorkflowHasNoConcurrencyGroup() throws IOException {
        writeWorkflow("no_concurrency_group.yml",
            "name: Build",
            "on:",
            "  push:",
            "    branches:",
            "      - main",
            "  pull_request:",
            "    branches:",
            "      - main",
            "jobs:",
            "  build:",
            "    runs-on: ubuntu-latest");

        List<String> violations = WorkflowTriggerConcurrencyScanner.findViolations(fixtureDirectory.getRoot());
        Assert.assertEquals(1, violations.size());
        Assert.assertTrue(violations.get(0), violations.get(0).contains("without a concurrency group"));
    }

    @Test
    public void aWildcardPushFilterBesideAPullRequestTriggerIsReportedEvenWhenTheGroupIsSafe() throws IOException {
        writeWorkflow("wildcard_push_filter.yml",
            "name: Build",
            "on:",
            "  push:",
            "    branches:",
            "      - '**'",
            "  pull_request:",
            "    branches:",
            "      - '**'",
            "concurrency:",
            "  group: build-${{ github.event_name }}-${{ github.head_ref || github.ref_name }}",
            "  cancel-in-progress: true",
            "jobs:",
            "  build:",
            "    runs-on: ubuntu-latest");

        List<String> violations = WorkflowTriggerConcurrencyScanner.findViolations(fixtureDirectory.getRoot());
        Assert.assertEquals(1, violations.size());
        Assert.assertTrue(violations.get(0), violations.get(0).contains("built by two runs"));
    }

    private void assertReportsBothDefects(String workflowFileName) throws IOException {
        List<String> violations = WorkflowTriggerConcurrencyScanner.findViolations(fixtureDirectory.getRoot());
        Assert.assertEquals(String.join("\n", violations), 2, violations.size());
        Assert.assertTrue(violations.get(0), violations.get(0).startsWith(workflowFileName));
        Assert.assertTrue(violations.get(0),
            violations.get(0).contains("resolves to the same value for those events"));
        Assert.assertTrue(violations.get(1), violations.get(1).contains("built by two runs"));
    }

    private String scanFixtures() throws IOException {
        return WorkflowTriggerConcurrencyScanner.scan(fixtureDirectory.getRoot()).describeViolations();
    }

    private void writeWorkflow(String workflowFileName, String... lines) throws IOException {
        File workflowFile = fixtureDirectory.newFile(workflowFileName);
        Files.write(workflowFile.toPath(), String.join("\n", lines).getBytes(StandardCharsets.UTF_8));
    }
}
