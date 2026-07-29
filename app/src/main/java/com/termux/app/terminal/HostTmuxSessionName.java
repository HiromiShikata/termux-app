package com.termux.app.terminal;

public final class HostTmuxSessionName {

    private HostTmuxSessionName() {
    }

    public static String normalize(String sessionName) {
        if (sessionName == null) {
            return null;
        }
        return sessionName.replace('.', '_').replace(':', '_');
    }
}
