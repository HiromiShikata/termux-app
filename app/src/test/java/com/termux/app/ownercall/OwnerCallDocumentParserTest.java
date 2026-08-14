package com.termux.app.ownercall;

import org.junit.Assert;
import org.junit.Test;
import org.yaml.snakeyaml.Yaml;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class OwnerCallDocumentParserTest {

    private static final String SESSION_NAME =
        "https_//github_com/HiromiShikata/termux-app/issues/1884";

    private static final String THREE_DOCUMENTS = ""
        + "---\n"
        + "sessionName: \"" + SESSION_NAME + "\"\n"
        + "calledAt: \"2026-08-14T04:22:28Z\"\n"
        + "body: |2\n"
        + "  The first line of the call body.\n"
        + "\n"
        + "  A later line of the call body.\n"
        + "---\n"
        + "sessionName: \"" + SESSION_NAME + "\"\n"
        + "calledAt: \"2026-08-14T05:10:00Z\"\n"
        + "body: |2\n"
        + "   The first line of this body begins with a space.\n"
        + "---\n"
        + "sessionName: \"" + SESSION_NAME + "\"\n"
        + "calledAt: \"2026-08-14T06:00:00Z\"\n"
        + "body: |2\n"
        + "  Decide whether the previous addresses may be deleted in bulk.\n";

    private final OwnerCallDocumentParser parser = new OwnerCallDocumentParser();

    @Test
    public void readsEveryDocumentOfTheFileOldestFirst() {
        List<OwnerCall> calls = parser.parse(THREE_DOCUMENTS);

        Assert.assertEquals(3, calls.size());
        Assert.assertEquals("2026-08-14T04:22:28Z", calls.get(0).getCalledAt());
        Assert.assertEquals("2026-08-14T05:10:00Z", calls.get(1).getCalledAt());
        Assert.assertEquals("2026-08-14T06:00:00Z", calls.get(2).getCalledAt());
        Assert.assertEquals("The first line of the call body.\n\nA later line of the call body.",
            calls.get(0).getBody());
        Assert.assertEquals(" The first line of this body begins with a space.",
            calls.get(1).getBody());
        Assert.assertEquals("Decide whether the previous addresses may be deleted in bulk.",
            calls.get(2).getBody());
    }

    @Test
    public void carriesTheSessionNameOfEveryDocumentSoAForeignFileCanBeRejected() {
        for (OwnerCall call : parser.parse(THREE_DOCUMENTS)) {
            Assert.assertEquals(SESSION_NAME, call.getSessionName());
        }
    }

    @Test
    public void readsTheSameValuesAsAFullYamlImplementationReadsFromTheSameFile() {
        List<OwnerCall> calls = parser.parse(THREE_DOCUMENTS);
        List<OwnerCall> callsReadByYaml = ownerCallsReadByYaml(THREE_DOCUMENTS);

        Assert.assertEquals(callsReadByYaml.size(), calls.size());
        for (int index = 0; index < calls.size(); index++) {
            Assert.assertEquals(callsReadByYaml.get(index).getSessionName(),
                calls.get(index).getSessionName());
            Assert.assertEquals(callsReadByYaml.get(index).getCalledAt(),
                calls.get(index).getCalledAt());
            Assert.assertEquals(callsReadByYaml.get(index).getBody(), calls.get(index).getBody());
        }
    }

    @Test
    public void readsNothingFromAnAbsentOrEmptyFile() {
        Assert.assertTrue(parser.parse(null).isEmpty());
        Assert.assertTrue(parser.parse("").isEmpty());
        Assert.assertTrue(parser.parse("---\n").isEmpty());
    }

    @Test
    public void keepsADocumentDelimiterThatBelongsToACallBody() {
        String documentWhoseBodyHoldsADelimiter = ""
            + "---\n"
            + "sessionName: \"" + SESSION_NAME + "\"\n"
            + "calledAt: \"2026-08-14T04:22:28Z\"\n"
            + "body: |2\n"
            + "  before\n"
            + "  ---\n"
            + "  after\n";

        List<OwnerCall> calls = parser.parse(documentWhoseBodyHoldsADelimiter);

        Assert.assertEquals(1, calls.size());
        Assert.assertEquals("before\n---\nafter", calls.get(0).getBody());
    }

    private static List<OwnerCall> ownerCallsReadByYaml(String document) {
        List<OwnerCall> calls = new ArrayList<>();
        for (Object loaded : new Yaml().loadAll(document)) {
            if (!(loaded instanceof Map)) {
                continue;
            }
            Map<?, ?> fields = (Map<?, ?>) loaded;
            calls.add(new OwnerCall(String.valueOf(fields.get("sessionName")),
                String.valueOf(fields.get("calledAt")),
                withoutTrailingNewlines(String.valueOf(fields.get("body")))));
        }
        return calls;
    }

    private static String withoutTrailingNewlines(String body) {
        int end = body.length();
        while (end > 0 && body.charAt(end - 1) == '\n') {
            end--;
        }
        return body.substring(0, end);
    }
}
