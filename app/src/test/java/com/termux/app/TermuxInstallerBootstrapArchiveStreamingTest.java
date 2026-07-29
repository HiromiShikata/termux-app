package com.termux.app;

import com.termux.app.bootstrap.BootstrapArchive;

import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class TermuxInstallerBootstrapArchiveStreamingTest {

    @Test
    public void installerNeverExposesTheBootstrapArchiveAsASingleByteArray() {
        List<String> byteArrayReturningMethods = new ArrayList<>();
        for (Method declaredMethod : TermuxInstaller.class.getDeclaredMethods()) {
            if (byte[].class.equals(declaredMethod.getReturnType())) {
                byteArrayReturningMethods.add(declaredMethod.toString());
            }
        }
        Assert.assertEquals(
            "The installer must consume the bootstrap archive as a stream. A method returning the whole archive as a "
                + "single byte array makes the archive resident on the Java heap, which fails on devices whose heap "
                + "growth limit is smaller than the archive. Offending methods: " + byteArrayReturningMethods,
            new ArrayList<String>(),
            byteArrayReturningMethods);
    }

    @Test
    public void nativeBootstrapArchiveAccessorReturnsADirectBufferRatherThanAByteArray() throws NoSuchMethodException {
        Method nativeAccessor = TermuxInstaller.class.getDeclaredMethod("getZipBuffer");
        Assert.assertTrue(
            "The bootstrap archive accessor must be the native method backed by the bootstrap shared library.",
            Modifier.isNative(nativeAccessor.getModifiers()));
        Assert.assertEquals(
            "The native bootstrap archive accessor must return a buffer over the already mapped archive so that no "
                + "Java heap allocation of the archive size is required.",
            ByteBuffer.class,
            nativeAccessor.getReturnType());
    }

    @Test
    public void installerLoadsTheBootstrapArchiveAsAStreamableArchive() throws NoSuchMethodException {
        Method archiveLoader = TermuxInstaller.class.getDeclaredMethod("loadBootstrapArchive");
        Assert.assertEquals(
            "The installer must load the bootstrap archive as a streamable archive rather than as archive bytes.",
            BootstrapArchive.class,
            archiveLoader.getReturnType());
    }
}
