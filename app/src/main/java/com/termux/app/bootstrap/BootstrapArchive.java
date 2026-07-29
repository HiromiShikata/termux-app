package com.termux.app.bootstrap;

import java.io.InputStream;
import java.nio.ByteBuffer;

public final class BootstrapArchive {

    private final ByteBuffer content;

    public BootstrapArchive(ByteBuffer content) {
        if (content == null) {
            throw new IllegalArgumentException("BootstrapArchive requires a content buffer");
        }
        this.content = content.asReadOnlyBuffer();
    }

    public int sizeInBytes() {
        return content.remaining();
    }

    public InputStream openStream() {
        return new ByteBufferInputStream(content);
    }
}
