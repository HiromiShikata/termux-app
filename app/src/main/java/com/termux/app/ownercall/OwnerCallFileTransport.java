package com.termux.app.ownercall;

import androidx.annotation.NonNull;

import java.io.IOException;

public interface OwnerCallFileTransport {

    @NonNull
    String fetch(@NonNull String url) throws IOException;

    void delete(@NonNull String url) throws IOException;
}
