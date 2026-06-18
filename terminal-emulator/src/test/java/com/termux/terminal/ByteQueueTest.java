package com.termux.terminal;

import junit.framework.TestCase;

public class ByteQueueTest extends TestCase {

	private static void assertArrayEquals(byte[] expected, byte[] actual) {
		if (expected.length != actual.length) {
			fail("Difference array length");
		}
		for (int i = 0; i < expected.length; i++) {
			if (expected[i] != actual[i]) {
				fail("Inequals at index=" + i + ", expected=" + (int) expected[i] + ", actual=" + (int) actual[i]);
			}
		}
	}

	public void testCompleteWrites() throws Exception {
		ByteQueue q = new ByteQueue(10);
		assertTrue(q.write(new byte[]{1, 2, 3}, 0, 3));

		byte[] arr = new byte[10];
		assertEquals(3, q.read(arr, true));
		assertArrayEquals(new byte[]{1, 2, 3}, new byte[]{arr[0], arr[1], arr[2]});

		assertTrue(q.write(new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10}, 0, 10));
		assertEquals(10, q.read(arr, true));
		assertArrayEquals(new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10}, arr);
	}

	public void testQueueWraparound() throws Exception {
		ByteQueue q = new ByteQueue(10);

		byte[] origArray = new byte[]{1, 2, 3, 4, 5, 6};
		byte[] readArray = new byte[origArray.length];
		for (int i = 0; i < 20; i++) {
			q.write(origArray, 0, origArray.length);
			assertEquals(origArray.length, q.read(readArray, true));
			assertArrayEquals(origArray, readArray);
		}
	}

	public void testWriteNotesClosing() throws Exception {
		ByteQueue q = new ByteQueue(10);
		q.close();
		assertFalse(q.write(new byte[]{1, 2, 3}, 0, 3));
	}

	public void testReadNonBlocking() throws Exception {
		ByteQueue q = new ByteQueue(10);
		assertEquals(0, q.read(new byte[128], false));
	}

	public void testPartialReadWhenDestinationSmallerThanStored() throws Exception {
		ByteQueue q = new ByteQueue(10);
		assertTrue(q.write(new byte[]{1, 2, 3, 4, 5, 6}, 0, 6));

		byte[] first = new byte[4];
		assertEquals(4, q.read(first, true));
		assertArrayEquals(new byte[]{1, 2, 3, 4}, first);

		byte[] second = new byte[4];
		assertEquals(2, q.read(second, true));
		assertArrayEquals(new byte[]{5, 6}, new byte[]{second[0], second[1]});
	}

	public void testWriteSplitsAcrossRingBoundary() throws Exception {
		ByteQueue q = new ByteQueue(10);
		assertTrue(q.write(new byte[]{1, 2, 3, 4, 5, 6}, 0, 6));
		assertEquals(6, q.read(new byte[6], true));

		byte[] crossing = new byte[]{10, 11, 12, 13, 14, 15, 16, 17};
		assertTrue(q.write(crossing, 0, crossing.length));

		byte[] readBack = new byte[crossing.length];
		assertEquals(crossing.length, q.read(readBack, true));
		assertArrayEquals(crossing, readBack);
	}

	public void testWriteRejectsOffsetPlusLengthBeyondBuffer() throws Exception {
		ByteQueue q = new ByteQueue(10);
		try {
			q.write(new byte[]{1, 2, 3}, 2, 3);
			fail("Expected IllegalArgumentException for offset + length > buffer.length");
		} catch (IllegalArgumentException expected) {
			// Expected.
		}
	}

	public void testWriteRejectsNonPositiveLength() throws Exception {
		ByteQueue q = new ByteQueue(10);
		try {
			q.write(new byte[]{1, 2, 3}, 0, 0);
			fail("Expected IllegalArgumentException for length <= 0");
		} catch (IllegalArgumentException expected) {
			// Expected.
		}
	}

	public void testReadAfterCloseReturnsMinusOne() throws Exception {
		ByteQueue q = new ByteQueue(10);
		q.close();
		assertEquals(-1, q.read(new byte[10], true));
	}

	public void testNonBlockingReadAfterCloseReturnsMinusOne() throws Exception {
		ByteQueue q = new ByteQueue(10);
		q.close();
		assertEquals(-1, q.read(new byte[10], false));
	}

	public void testBlockingReadUnblockedByConcurrentWrite() throws Exception {
		final ByteQueue q = new ByteQueue(10);
		final byte[] readBuffer = new byte[10];
		final int[] readResult = new int[]{Integer.MIN_VALUE};

		Thread reader = new Thread(() -> readResult[0] = q.read(readBuffer, true));
		reader.start();

		Thread.sleep(50);
		assertTrue(q.write(new byte[]{7, 8, 9}, 0, 3));

		reader.join(2000);
		assertFalse("Blocking read did not return after a concurrent write", reader.isAlive());
		assertEquals(3, readResult[0]);
		assertArrayEquals(new byte[]{7, 8, 9}, new byte[]{readBuffer[0], readBuffer[1], readBuffer[2]});
	}

	public void testBlockingWriteUnblockedByConcurrentReadAtFullBoundary() throws Exception {
		final ByteQueue q = new ByteQueue(4);
		assertTrue(q.write(new byte[]{1, 2, 3, 4}, 0, 4));

		final boolean[] writeResult = new boolean[]{false};
		final boolean[] writeReturned = new boolean[]{false};
		Thread writer = new Thread(() -> {
			writeResult[0] = q.write(new byte[]{5, 6}, 0, 2);
			writeReturned[0] = true;
		});
		writer.start();

		Thread.sleep(50);
		assertFalse("Write should still be blocked while the queue is full", writeReturned[0]);

		byte[] drain = new byte[4];
		assertEquals(4, q.read(drain, true));
		assertArrayEquals(new byte[]{1, 2, 3, 4}, drain);

		writer.join(2000);
		assertFalse("Blocking write did not return after a concurrent read", writer.isAlive());
		assertTrue(writeResult[0]);

		byte[] remaining = new byte[2];
		assertEquals(2, q.read(remaining, true));
		assertArrayEquals(new byte[]{5, 6}, remaining);
	}

}
