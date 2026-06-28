package com.termux.app.sessiondefinition;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SshKeepaliveCommandAugmenter {

    public static final int SERVER_ALIVE_INTERVAL_SECONDS = 60;

    public static final int SERVER_ALIVE_COUNT_MAX = 3;

    private static final String KEEPALIVE_OPTIONS =
        "-o ServerAliveInterval=" + SERVER_ALIVE_INTERVAL_SECONDS
            + " -o ServerAliveCountMax=" + SERVER_ALIVE_COUNT_MAX;

    private static final Pattern SSH_INVOCATION = Pattern.compile("(^|\\s)(ssh)(\\s)");

    private static final Pattern EXISTING_SERVER_ALIVE_INTERVAL =
        Pattern.compile("ServerAliveInterval");

    public String augment(String command) {
        if (command == null) {
            return null;
        }
        if (EXISTING_SERVER_ALIVE_INTERVAL.matcher(command).find()) {
            return command;
        }
        Matcher matcher = SSH_INVOCATION.matcher(command);
        if (!matcher.find()) {
            return command;
        }
        String prefix = matcher.group(1);
        String trailingWhitespace = matcher.group(3);
        String replacement = Matcher.quoteReplacement(
            prefix + "ssh " + KEEPALIVE_OPTIONS + trailingWhitespace);
        return command.substring(0, matcher.start())
            + replacement
            + command.substring(matcher.end());
    }
}
