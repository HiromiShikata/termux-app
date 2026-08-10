package com.termux.app.process;

import com.termux.app.diagnostics.DiagnosticsAppProcessPopulation;
import com.termux.app.diagnostics.DiagnosticsProcessCommandCount;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AppProcessPopulationReaderTest {

    private static final class StubProcessTable implements ProcessTable {

        private final List<String> mProcessIdentifiers;

        private final Map<String, String> mCommandNameByProcessIdentifier;

        private final RuntimeException mListingFailure;

        StubProcessTable(List<String> processIdentifiers,
                         Map<String, String> commandNameByProcessIdentifier) {
            mProcessIdentifiers = processIdentifiers;
            mCommandNameByProcessIdentifier = commandNameByProcessIdentifier;
            mListingFailure = null;
        }

        StubProcessTable(RuntimeException listingFailure) {
            mProcessIdentifiers = new ArrayList<>();
            mCommandNameByProcessIdentifier = new LinkedHashMap<>();
            mListingFailure = listingFailure;
        }

        @Override
        public List<String> processIdentifiers() {
            if (mListingFailure != null) {
                throw mListingFailure;
            }
            return mProcessIdentifiers;
        }

        @Override
        public String commandNameOf(String processIdentifier) {
            return mCommandNameByProcessIdentifier.get(processIdentifier);
        }
    }

    @Test
    public void theTotalCountsEveryProcessTheAppIsRunning() {
        Map<String, String> commandNames = new LinkedHashMap<>();
        commandNames.put("101", "sh");
        commandNames.put("102", "ssh");
        commandNames.put("103", "ssh");

        DiagnosticsAppProcessPopulation population = new AppProcessPopulationReader(
            new StubProcessTable(Arrays.asList("101", "102", "103"), commandNames)).read();

        Assert.assertTrue("the report has to state that a measurement happened at all, because an"
            + " unmeasured population is indistinguishable from a population of zero", population.getWasMeasured());
        Assert.assertEquals("the phantom process ceiling is enforced against the total number of processes"
                + " the app is running, so a total that misses any process cannot be compared against it",
            3, population.getTotalProcessCount());
    }

    @Test
    public void theCommandNamesAreReportedMostNumerousFirstSoTheDominantProcessIsVisible() {
        Map<String, String> commandNames = new LinkedHashMap<>();
        commandNames.put("101", "sh");
        commandNames.put("102", "ssh");
        commandNames.put("103", "ssh");
        commandNames.put("104", "ssh");
        commandNames.put("105", "autossh");
        commandNames.put("106", "autossh");

        List<DiagnosticsProcessCommandCount> counts = new AppProcessPopulationReader(
            new StubProcessTable(Arrays.asList("101", "102", "103", "104", "105", "106"), commandNames))
            .read().getCountsByCommandName();

        Assert.assertEquals("grouping by command name is what tells one process per session apart from"
            + " three per session, so every distinct name has to survive the grouping", 3, counts.size());
        Assert.assertEquals("the most numerous command is the one that has to be removed to get under the"
            + " ceiling, so it has to be first", "ssh", counts.get(0).getCommandName());
        Assert.assertEquals("the count carried beside the name is what sizes the reduction needed",
            3, counts.get(0).getProcessCount());
        Assert.assertEquals("autossh outnumbers sh, so it has to rank above it",
            "autossh", counts.get(1).getCommandName());
        Assert.assertEquals("the least numerous command still has to appear, because a single leftover"
            + " process can be the one that crosses the ceiling", "sh", counts.get(2).getCommandName());
    }

    @Test
    public void aProcessWhoseCommandCannotBeReadIsStillCountedRatherThanDropped() {
        Map<String, String> commandNames = new LinkedHashMap<>();
        commandNames.put("101", "ssh");

        DiagnosticsAppProcessPopulation population = new AppProcessPopulationReader(
            new StubProcessTable(Arrays.asList("101", "102"), commandNames)).read();

        Assert.assertEquals("a process whose command line cannot be read still occupies a slot under the"
            + " ceiling, so dropping it would understate the population", 2, population.getTotalProcessCount());
        List<DiagnosticsProcessCommandCount> counts = population.getCountsByCommandName();
        boolean unreadableReported = false;
        for (DiagnosticsProcessCommandCount count : counts) {
            if (DiagnosticsProcessCommandCount.UNREADABLE_COMMAND_NAME.equals(count.getCommandName())) {
                unreadableReported = true;
                Assert.assertEquals("the unreadable processes have to carry their own count so the"
                    + " breakdown adds up to the total", 1, count.getProcessCount());
            }
        }
        Assert.assertTrue("an unreadable command name has to be named as unreadable rather than silently"
            + " omitted, because a breakdown that does not add up to the total misleads the reader",
            unreadableReported);
    }

    @Test
    public void aFailureToListTheProcessTableIsReportedRatherThanReadAsZeroProcesses() {
        DiagnosticsAppProcessPopulation population = new AppProcessPopulationReader(
            new StubProcessTable(new IllegalStateException("the process table could not be listed"))).read();

        Assert.assertFalse("a failed read reported as a successful measurement of zero processes would"
            + " say the app is far below the ceiling at the exact moment it is being killed for exceeding it",
            population.getWasMeasured());
        Assert.assertEquals("the reason the read failed is what tells the reader whether the number is"
                + " missing or genuinely zero", "the process table could not be listed",
            population.getReadFailureMessage());
    }
}
