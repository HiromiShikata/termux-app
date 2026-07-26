package com.termux.app.sessiondefinition;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SshKeepaliveCommandAugmenter {

    public static final int SERVER_ALIVE_INTERVAL_SECONDS = 30;

    public static final int SERVER_ALIVE_COUNT_MAX = 3;

    public static final String AUTOSSH_GATETIME_ENV = "AUTOSSH_GATETIME=0";

    public static final int CONNECT_TIMEOUT_SECONDS = 10;

    private static final String KEEPALIVE_OPTIONS =
        "-o ServerAliveInterval=" + SERVER_ALIVE_INTERVAL_SECONDS
            + " -o ServerAliveCountMax=" + SERVER_ALIVE_COUNT_MAX
            + " -o TCPKeepAlive=yes"
            + " -o ConnectTimeout=" + CONNECT_TIMEOUT_SECONDS;

    private static final Pattern SSH_INVOCATION = Pattern.compile("(^|\\s)(ssh)(\\s)");

    private static final Pattern AUTOSSH_INVOCATION = Pattern.compile("(^|\\s)autossh(\\s)");

    private static final Pattern EXISTING_SERVER_ALIVE_INTERVAL =
        Pattern.compile("ServerAliveInterval");

    private static final Pattern EXISTING_AUTOSSH_GATETIME =
        Pattern.compile("AUTOSSH_GATETIME");

    public String augment(String command) {
        if (command == null) {
            return null;
        }
        String withKeepaliveOptions = augmentSshOptions(command);
        return augmentAutosshGatetime(withKeepaliveOptions);
    }

    private String augmentSshOptions(String command) {
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

    private String augmentAutosshGatetime(String command) {
        if (EXISTING_AUTOSSH_GATETIME.matcher(command).find()) {
            return command;
        }
        Matcher matcher = AUTOSSH_INVOCATION.matcher(command);
        if (!matcher.find()) {
            return command;
        }
        String prefix = matcher.group(1);
        String replacement = Matcher.quoteReplacement(
            prefix + AUTOSSH_GATETIME_ENV + " autossh" + matcher.group(2));
        return command.substring(0, matcher.start())
            + replacement
            + command.substring(matcher.end());
    }
}
