package com.termux.app.terminal;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class ActiveSessionViewBindingResolution {

    private final boolean mRequiresTerminalRebind;

    private final boolean mRequiresBrowserRebind;

    private final String mDiagnosticLine;

    private ActiveSessionViewBindingResolution(boolean requiresTerminalRebind,
                                               boolean requiresBrowserRebind,
                                               @NonNull String diagnosticLine) {
        this.mRequiresTerminalRebind = requiresTerminalRebind;
        this.mRequiresBrowserRebind = requiresBrowserRebind;
        this.mDiagnosticLine = diagnosticLine;
    }

    public boolean requiresTerminalRebind() {
        return mRequiresTerminalRebind;
    }

    public boolean requiresBrowserRebind() {
        return mRequiresBrowserRebind;
    }

    public boolean isConsistent() {
        return !mRequiresTerminalRebind && !mRequiresBrowserRebind;
    }

    @NonNull
    public String diagnosticLine() {
        return mDiagnosticLine;
    }

    @NonNull
    public static ActiveSessionViewBindingResolution resolve(@Nullable String activeSessionHandle,
                                                             @Nullable String displayedTerminalSessionHandle,
                                                             boolean browserVisible,
                                                             @Nullable String displayedBrowserSessionHandle,
                                                             boolean activeSessionHasBrowserTab) {
        boolean requiresTerminalRebind = activeSessionHandle != null
            && !activeSessionHandle.equals(displayedTerminalSessionHandle);

        boolean activeMatchesBrowser = activeSessionHandle != null
            && activeSessionHandle.equals(displayedBrowserSessionHandle);
        boolean browserShowsForeignPage = displayedBrowserSessionHandle != null
            && !displayedBrowserSessionHandle.equals(activeSessionHandle);
        boolean activeTabNotYetShown = activeSessionHasBrowserTab && !activeMatchesBrowser;
        boolean requiresBrowserRebind = browserVisible
            && (browserShowsForeignPage || activeTabNotYetShown);

        String diagnosticLine = "session view binding"
            + " active=" + activeSessionHandle
            + " terminal=" + displayedTerminalSessionHandle
            + " browser=" + displayedBrowserSessionHandle
            + " browserVisible=" + browserVisible
            + " activeHasBrowserTab=" + activeSessionHasBrowserTab
            + " terminalRebind=" + requiresTerminalRebind
            + " browserRebind=" + requiresBrowserRebind;

        return new ActiveSessionViewBindingResolution(
            requiresTerminalRebind, requiresBrowserRebind, diagnosticLine);
    }
}
