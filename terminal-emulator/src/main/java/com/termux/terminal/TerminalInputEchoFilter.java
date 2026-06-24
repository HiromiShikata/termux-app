package com.termux.terminal;

import java.util.ArrayDeque;

public final class TerminalInputEchoFilter {

    private static final int MAX_PENDING_ECHO_BYTES = 4096;

    private final ArrayDeque<Byte> mPendingEchoBytes = new ArrayDeque<>();

    public synchronized void recordUserInput(byte[] data, int offset, int count) {
        for (int i = 0; i < count; i++) {
            if (mPendingEchoBytes.size() >= MAX_PENDING_ECHO_BYTES) {
                mPendingEchoBytes.pollFirst();
            }
            mPendingEchoBytes.addLast(data[offset + i]);
        }
    }

    public synchronized int consumeEchoPrefixReturningGenuineOffset(byte[] data, int offset, int length) {
        int index = offset;
        int end = offset + length;
        while (index < end && !mPendingEchoBytes.isEmpty() && mPendingEchoBytes.peekFirst() == data[index]) {
            mPendingEchoBytes.pollFirst();
            index++;
        }
        if (index < end) {
            mPendingEchoBytes.clear();
        }
        return index;
    }

    public synchronized int countGenuineOutput(byte[] data, int offset, int length) {
        return offset + length - consumeEchoPrefixReturningGenuineOffset(data, offset, length);
    }

    public synchronized boolean hasPendingEcho() {
        return !mPendingEchoBytes.isEmpty();
    }
}
