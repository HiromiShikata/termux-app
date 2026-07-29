package com.termux.ci;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class WorkflowTriggerConcurrencyTest {

    private final File workflowDirectory =
        WorkflowTriggerConcurrencyScanner.findWorkflowDirectory(new File("."));

    @Test
    public void aPushRunAndAPullRequestRunOfTheSameBranchNeverShareOneConcurrencyGroup() throws IOException {
        List<String> violations = WorkflowTriggerConcurrencyScanner.findViolations(workflowDirectory);
        Assert.assertEquals("", String.join("\n", violations));
    }

    @Test
    public void theBuildWorkflowIsScannedSoTheInvariantCannotSilentlyCoverNothing() {
        Assert.assertTrue(new File(workflowDirectory, "debug_build.yml").isFile());
    }
}
