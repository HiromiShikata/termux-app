package com.termux.app.terminal.tts;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SpeakTagScanner {

    private static final Pattern SPEAK_BLOCK_PATTERN = Pattern.compile("<speak>([\\s\\S]*?)</speak>");

    private static final Pattern MARKDOWN_DECORATION_PATTERN = Pattern.compile("[*_`#>~]");

    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");

    private String lastSpokenText;

    public static List<String> extractSpokenTexts(String output) {
        List<String> spokenTexts = new ArrayList<>();
        if (output == null) return spokenTexts;

        Matcher matcher = SPEAK_BLOCK_PATTERN.matcher(output);
        while (matcher.find()) {
            String spokenText = stripDecoration(matcher.group(1));
            if (!spokenText.isEmpty()) spokenTexts.add(spokenText);
        }
        return spokenTexts;
    }

    public static String stripDecoration(String innerText) {
        if (innerText == null) return "";
        String withoutMarkdown = MARKDOWN_DECORATION_PATTERN.matcher(innerText).replaceAll(" ");
        return WHITESPACE_PATTERN.matcher(withoutMarkdown).replaceAll(" ").trim();
    }

    public String newSpokenText(String output) {
        List<String> spokenTexts = extractSpokenTexts(output);
        if (spokenTexts.isEmpty()) return null;

        String latestSpokenText = spokenTexts.get(spokenTexts.size() - 1);
        if (latestSpokenText.equals(lastSpokenText)) return null;
        return latestSpokenText;
    }

    public void markSpoken(String spokenText) {
        lastSpokenText = spokenText;
    }
}
