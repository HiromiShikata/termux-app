package com.termux.app.diagnostics;

public final class DiagnosticsMemoryUsage {

    private final long mJavaHeapUsedMegabytes;
    private final long mJavaHeapTotalMegabytes;
    private final long mJavaHeapMaxMegabytes;
    private final long mNativeHeapAllocatedMegabytes;

    public DiagnosticsMemoryUsage(long javaHeapUsedMegabytes, long javaHeapTotalMegabytes,
                                  long javaHeapMaxMegabytes, long nativeHeapAllocatedMegabytes) {
        mJavaHeapUsedMegabytes = javaHeapUsedMegabytes;
        mJavaHeapTotalMegabytes = javaHeapTotalMegabytes;
        mJavaHeapMaxMegabytes = javaHeapMaxMegabytes;
        mNativeHeapAllocatedMegabytes = nativeHeapAllocatedMegabytes;
    }

    public long getJavaHeapUsedMegabytes() {
        return mJavaHeapUsedMegabytes;
    }

    public long getJavaHeapTotalMegabytes() {
        return mJavaHeapTotalMegabytes;
    }

    public long getJavaHeapMaxMegabytes() {
        return mJavaHeapMaxMegabytes;
    }

    public long getNativeHeapAllocatedMegabytes() {
        return mNativeHeapAllocatedMegabytes;
    }
}
