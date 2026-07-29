package com.termux.app.bootstrap;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BootstrapArchiveNativeAccessorGuardTest {

    private static final String NATIVE_ACCESSOR_SOURCE = "app/src/main/cpp/termux-bootstrap.c";

    private static final Pattern JNI_INTERFACE_CALL = Pattern.compile("\\(\\s*\\*\\s*env\\s*\\)\\s*->\\s*([A-Za-z]+)");

    @Test
    public void nativeAccessorHandsOutTheBootstrapArchiveAsADirectBufferWithoutAJavaHeapCopy() throws IOException {
        String nativeSource = readNativeAccessorSource();
        Assert.assertTrue(
            "The native bootstrap accessor must hand the archive to Java as a direct buffer over the already mapped "
                + "blob, so the archive is never copied onto the Java heap. Expected a NewDirectByteBuffer call in "
                + NATIVE_ACCESSOR_SOURCE + " but found:\n" + nativeSource,
            nativeSource.contains("NewDirectByteBuffer"));
        Assert.assertFalse(
            "The native bootstrap accessor must not allocate a Java byte array for the whole archive, because that "
                + "allocation fails on devices whose heap growth limit is smaller than the archive. Found NewByteArray "
                + "in " + NATIVE_ACCESSOR_SOURCE + ":\n" + nativeSource,
            nativeSource.contains("NewByteArray"));
        Assert.assertFalse(
            "The native bootstrap accessor must not copy the archive into a Java byte array. Found SetByteArrayRegion "
                + "in " + NATIVE_ACCESSOR_SOURCE + ":\n" + nativeSource,
            nativeSource.contains("SetByteArrayRegion"));
    }

    @Test
    public void nativeAccessorMakesNoFurtherJniCallAfterItsAllocatingCall() throws IOException {
        String nativeSource = readNativeAccessorSource();
        StringBuilder foundCalls = new StringBuilder();
        int callCount = 0;
        Matcher matcher = JNI_INTERFACE_CALL.matcher(nativeSource);
        while (matcher.find()) {
            callCount++;
            foundCalls.append("\n    ").append(matcher.group(1));
        }
        Assert.assertEquals(
            "The native bootstrap accessor must perform exactly one JNI call. A JNI call issued after a failed "
                + "allocation runs with the OutOfMemoryError still pending, which the runtime treats as a fatal error "
                + "and aborts the whole process instead of surfacing an exception. JNI calls found in "
                + NATIVE_ACCESSOR_SOURCE + ":" + foundCalls,
            1,
            callCount);
    }

    private String readNativeAccessorSource() throws IOException {
        Path nativeAccessorSource = locateRepositoryRoot().resolve(NATIVE_ACCESSOR_SOURCE);
        Assert.assertTrue(
            "The native bootstrap accessor source does not exist: " + nativeAccessorSource,
            Files.isRegularFile(nativeAccessorSource));
        return new String(Files.readAllBytes(nativeAccessorSource), StandardCharsets.UTF_8);
    }

    private Path locateRepositoryRoot() {
        Path current = Paths.get("").toAbsolutePath();
        while (current != null) {
            if (Files.exists(current.resolve("settings.gradle"))) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException(
            "Could not locate the repository root (a directory containing settings.gradle) starting from "
                + Paths.get("").toAbsolutePath());
    }
}
