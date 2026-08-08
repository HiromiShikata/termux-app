package com.termux.terminal;

import androidx.annotation.NonNull;

import java.io.ByteArrayOutputStream;

final class ShellStartupInputBuffer {

    static final int CAPACITY_BYTES = 8192;

    private final ByteArrayOutputStream mHeldInput = new ByteArrayOutputStream();

    synchronized boolean hold(@NonNull byte[] data, int offset, int count) {
        if (count <= 0) return false;
        if (mHeldInput.size() + count > CAPACITY_BYTES) return false;
        mHeldInput.write(data, offset, count);
        return true;
    }

    synchronized int heldByteCount() {
        return mHeldInput.size();
    }

    @NonNull
    synchronized byte[] drain() {
        byte[] heldInput = mHeldInput.toByteArray();
        mHeldInput.reset();
        return heldInput;
    }
}
