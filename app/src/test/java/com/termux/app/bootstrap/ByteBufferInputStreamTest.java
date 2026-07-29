package com.termux.app.bootstrap;

import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class ByteBufferInputStreamTest {

    @Test
    public void readsEveryByteOfADirectBufferThroughASmallTransferArray() throws IOException {
        byte[] content = buildContent(1024 * 1024);
        ByteBuffer directContent = toDirectBuffer(content);
        Assert.assertFalse(
            "The test must exercise a buffer without a Java heap backing array, as the native bootstrap accessor "
                + "hands out.",
            directContent.hasArray());

        ByteArrayOutputStream readContent = new ByteArrayOutputStream();
        byte[] transferBuffer = new byte[8096];
        try (InputStream stream = new ByteBufferInputStream(directContent)) {
            int readLength;
            while ((readLength = stream.read(transferBuffer)) != -1) {
                readContent.write(transferBuffer, 0, readLength);
            }
        }

        Assert.assertArrayEquals(content, readContent.toByteArray());
    }

    @Test
    public void reportsEndOfStreamOnceTheBufferIsExhausted() throws IOException {
        ByteBuffer content = toDirectBuffer(new byte[]{1, 2});
        try (InputStream stream = new ByteBufferInputStream(content)) {
            Assert.assertEquals(1, stream.read());
            Assert.assertEquals(2, stream.read());
            Assert.assertEquals(-1, stream.read());
            Assert.assertEquals(-1, stream.read(new byte[4]));
        }
    }

    @Test
    public void readsSingleBytesAsUnsignedValues() throws IOException {
        ByteBuffer content = toDirectBuffer(new byte[]{(byte) 0x00, (byte) 0x7F, (byte) 0x80, (byte) 0xFF});
        try (InputStream stream = new ByteBufferInputStream(content)) {
            Assert.assertEquals(0x00, stream.read());
            Assert.assertEquals(0x7F, stream.read());
            Assert.assertEquals(0x80, stream.read());
            Assert.assertEquals(0xFF, stream.read());
        }
    }

    @Test
    public void reportsTheRemainingByteCountAsAvailable() throws IOException {
        ByteBuffer content = toDirectBuffer(new byte[]{1, 2, 3, 4, 5});
        try (InputStream stream = new ByteBufferInputStream(content)) {
            Assert.assertEquals(5, stream.available());
            Assert.assertEquals(3, stream.read(new byte[3]));
            Assert.assertEquals(2, stream.available());
        }
    }

    @Test
    public void leavesTheSourceBufferPositionUntouchedSoItCanBeReadAgain() throws IOException {
        byte[] content = buildContent(4096);
        ByteBuffer directContent = toDirectBuffer(content);

        readFully(new ByteBufferInputStream(directContent));

        Assert.assertEquals(0, directContent.position());
        Assert.assertArrayEquals(content, readFully(new ByteBufferInputStream(directContent)));
    }

    @Test
    public void readsFromAReadOnlyBuffer() throws IOException {
        byte[] content = buildContent(4096);
        ByteBuffer readOnlyContent = toDirectBuffer(content).asReadOnlyBuffer();

        Assert.assertArrayEquals(content, readFully(new ByteBufferInputStream(readOnlyContent)));
    }

    @Test
    public void rejectsAMissingContentBuffer() {
        try {
            new ByteBufferInputStream(null);
            Assert.fail("A missing content buffer must be rejected rather than accepted.");
        } catch (IllegalArgumentException expected) {
            Assert.assertTrue(expected.getMessage(), expected.getMessage().contains("content buffer"));
        }
    }

    @Test
    public void rejectsADestinationRangeOutsideTheDestinationArray() throws IOException {
        try (InputStream stream = new ByteBufferInputStream(toDirectBuffer(new byte[]{1, 2, 3}))) {
            try {
                stream.read(new byte[2], 1, 2);
                Assert.fail("A destination range outside the destination array must be rejected.");
            } catch (IndexOutOfBoundsException expected) {
                Assert.assertNotNull(expected);
            }
        }
    }

    private byte[] buildContent(int length) {
        byte[] content = new byte[length];
        for (int index = 0; index < length; index++) {
            content[index] = (byte) (index * 31 + 7);
        }
        return content;
    }

    private ByteBuffer toDirectBuffer(byte[] content) {
        ByteBuffer directBuffer = ByteBuffer.allocateDirect(content.length);
        directBuffer.put(content);
        directBuffer.rewind();
        return directBuffer;
    }

    private byte[] readFully(InputStream stream) throws IOException {
        ByteArrayOutputStream readContent = new ByteArrayOutputStream();
        byte[] transferBuffer = new byte[512];
        int readLength;
        while ((readLength = stream.read(transferBuffer)) != -1) {
            readContent.write(transferBuffer, 0, readLength);
        }
        stream.close();
        return readContent.toByteArray();
    }
}
