package com.termux.app.ownercall;

import androidx.annotation.NonNull;

import com.termux.app.terminal.HostTmuxSessionName;

import java.io.IOException;
import java.util.List;

public final class OwnerCallFileReader {

    private final OwnerCallFileTransport mTransport;
    private final OwnerCallDocumentParser mParser = new OwnerCallDocumentParser();

    public OwnerCallFileReader(@NonNull OwnerCallFileTransport transport) {
        mTransport = transport;
    }

    @NonNull
    public List<OwnerCall> readCallsOfSession(@NonNull String fileUrl, @NonNull String sessionName)
        throws IOException {
        return OwnerCallInbox.callsOfSession(mParser.parse(mTransport.fetch(fileUrl)),
            HostTmuxSessionName.normalize(sessionName));
    }
}
