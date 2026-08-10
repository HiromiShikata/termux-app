package com.termux.app.process;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.List;

public class ProcFileSystemProcessTableTest {

    private File mProcDirectory;

    @Before
    public void createProcDirectory() throws IOException {
        File placeholder = File.createTempFile("process-table", "");
        Assert.assertTrue(placeholder.delete());
        Assert.assertTrue(placeholder.mkdir());
        mProcDirectory = placeholder;
    }

    @After
    public void removeProcDirectory() {
        deleteRecursively(mProcDirectory);
    }

    private void deleteRecursively(File file) {
        File[] children = file.listFiles();
        if (children != null) {
            for (File child : children) {
                deleteRecursively(child);
            }
        }
        file.delete();
    }

    private void givenProcessDirectory(String processIdentifier) {
        Assert.assertTrue(new File(mProcDirectory, processIdentifier).mkdir());
    }

    private void givenProcessWithCommandLine(String processIdentifier, String commandLine)
        throws IOException {
        givenProcessDirectory(processIdentifier);
        File commandLineFile = new File(new File(mProcDirectory, processIdentifier), "cmdline");
        try (OutputStream output = new FileOutputStream(commandLineFile)) {
            output.write(commandLine.getBytes(Charset.forName("UTF-8")));
        }
    }

    private ProcFileSystemProcessTable processTable() {
        return new ProcFileSystemProcessTable(mProcDirectory);
    }

    @Test
    public void onlyTheNumericEntriesAreReadAsProcessesBecauseTheOthersAreKernelStateFiles()
        throws IOException {
        givenProcessDirectory("101");
        givenProcessDirectory("2024");
        givenProcessDirectory("self");
        Assert.assertTrue(new File(mProcDirectory, "meminfo").createNewFile());

        List<String> processIdentifiers = processTable().processIdentifiers();

        Assert.assertEquals("counting a kernel state entry as a process would overstate how close the"
                + " app is to the ceiling Android enforces on its processes, and counting the self"
                + " alias would count one process twice",
            2, processIdentifiers.size());
        Assert.assertTrue("a numeric entry is a process and has to be counted. Actual: "
            + processIdentifiers, processIdentifiers.contains("101"));
        Assert.assertTrue("a numeric entry is a process and has to be counted. Actual: "
            + processIdentifiers, processIdentifiers.contains("2024"));
    }

    @Test
    public void theCommandNameIsTheProgramBaseNameSoEverySessionRunningOneProgramGroupsTogether()
        throws IOException {
        givenProcessWithCommandLine("101", "/data/data/example/files/usr/bin/ssh\0example.invalid\0");

        Assert.assertEquals("the breakdown groups by command name, so two sessions running the same"
                + " program from the same path have to land in one group rather than in two",
            "ssh", processTable().commandNameOf("101"));
    }

    @Test
    public void aCommandLineHoldingOnlyTheProgramIsReadWithoutRequiringATrailingSeparator()
        throws IOException {
        givenProcessWithCommandLine("101", "/data/data/example/files/usr/bin/sh");

        Assert.assertEquals("a process started with no arguments still occupies a slot under the"
            + " ceiling, so its name has to be readable", "sh", processTable().commandNameOf("101"));
    }

    @Test
    public void aLoginShellIsReportedUnderItsProgramNameRatherThanAsASeparateCommand()
        throws IOException {
        givenProcessWithCommandLine("101", "-/data/data/example/files/usr/bin/bash\0");

        Assert.assertEquals("a login shell writes its program name with a leading marker, and leaving"
                + " the marker in would split one program across two groups and hide how many shells"
                + " are alive", "bash", processTable().commandNameOf("101"));
    }

    @Test
    public void aProcessWithNoReadableCommandLineReportsNoNameRatherThanAnEmptyName() {
        givenProcessDirectory("101");

        Assert.assertNull("a process whose command line cannot be read still has to be counted under"
                + " the unreadable group, which the caller can only do when the name is reported as"
                + " absent rather than as an empty string",
            processTable().commandNameOf("101"));
    }

    @Test
    public void aProcessTableThatCannotBeListedFailsRatherThanReportingNoProcesses() {
        File missingDirectory = new File(mProcDirectory, "absent");

        try {
            new ProcFileSystemProcessTable(missingDirectory).processIdentifiers();
            Assert.fail("a process table that cannot be listed reported as an empty list would say the"
                + " app is running no processes at the moment it is being killed for running too many");
        } catch (IllegalStateException expected) {
            Assert.assertTrue("the failure has to name the path that could not be listed so the report"
                    + " states why the number is missing. Actual: " + expected.getMessage(),
                expected.getMessage().contains(missingDirectory.getPath()));
        }
    }
}
