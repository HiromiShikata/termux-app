package com.termux.terminal;

public final class RemoteShellClientCommand {

    private static final String[] REMOTE_SHELL_CLIENT_PROGRAM_NAMES = {"autossh", "ssh", "mosh"};

    private RemoteShellClientCommand() {
    }

    public static boolean isRunBy(String shellPath, String[] args) {
        if (namesARemoteShellClient(shellPath)) return true;
        if (args == null) return false;
        for (String arg : args) {
            if (namesARemoteShellClient(arg)) return true;
        }
        return false;
    }

    private static boolean namesARemoteShellClient(String value) {
        if (value == null) return false;
        for (String programName : REMOTE_SHELL_CLIENT_PROGRAM_NAMES) {
            if (containsAsWholeWord(value, programName)) return true;
        }
        return false;
    }

    private static boolean containsAsWholeWord(String value, String word) {
        int searchFrom = 0;
        while (true) {
            int found = value.indexOf(word, searchFrom);
            if (found < 0) return false;
            int after = found + word.length();
            if (!isWordCharacter(value, found - 1) && !isWordCharacter(value, after)) return true;
            searchFrom = found + 1;
        }
    }

    private static boolean isWordCharacter(String value, int index) {
        if (index < 0 || index >= value.length()) return false;
        char character = value.charAt(index);
        return Character.isLetterOrDigit(character) || character == '_' || character == '-';
    }
}
