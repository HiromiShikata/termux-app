package com.termux.app.bootstrap;

import java.io.InputStream;
import java.nio.ByteBuffer;

public final class ByteBufferInputStream extends InputStream {

    private final ByteBuffer remainingContent;

    public ByteBufferInputStream(ByteBuffer content) {
        if (content == null) {
            throw new IllegalArgumentException("ByteBufferInputStream requires a content buffer");
        }
        this.remainingContent = content.duplicate();
    }

    @Override
    public int read() {
        if (!remainingContent.hasRemaining()) {
            return -1;
        }
        return remainingContent.get() & 0xFF;
    }

    @Override
    public int read(byte[] destination, int destinationOffset, int requestedLength) {
        if (destination == null) {
            throw new NullPointerException("ByteBufferInputStream requires a destination array");
        }
        if (destinationOffset < 0 || requestedLength < 0 || requestedLength > destination.length - destinationOffset) {
            throw new IndexOutOfBoundsException("Offset " + destinationOffset + " and length " + requestedLength
                + " do not fit a destination array of length " + destination.length);
        }
        if (requestedLength == 0) {
            return 0;
        }
        if (!remainingContent.hasRemaining()) {
            return -1;
        }
        int readableLength = Math.min(requestedLength, remainingContent.remaining());
        remainingContent.get(destination, destinationOffset, readableLength);
        return readableLength;
    }

    @Override
    public int available() {
        return remainingContent.remaining();
    }
}
