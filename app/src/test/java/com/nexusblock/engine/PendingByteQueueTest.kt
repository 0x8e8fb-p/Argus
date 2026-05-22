package com.nexusblock.engine

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PendingByteQueueTest {

    @Test
    fun offerRejectsDataBeyondCapWithoutDroppingExistingBytes() {
        val queue = PendingByteQueue(maxBytes = 4)

        assertTrue(queue.offer(byteArrayOf(1, 2)))
        assertFalse(queue.offer(byteArrayOf(3, 4, 5)))

        assertEquals(2, queue.byteCount)
        assertArrayEquals(byteArrayOf(1, 2), queue.drain())
    }

    @Test
    fun drainPreservesSegmentOrderAndClearsQueue() {
        val queue = PendingByteQueue(maxBytes = 8)

        assertTrue(queue.offer(byteArrayOf(1, 2)))
        assertTrue(queue.offer(byteArrayOf(3)))
        assertTrue(queue.offer(byteArrayOf(4, 5)))

        assertArrayEquals(byteArrayOf(1, 2, 3, 4, 5), queue.drain())
        assertEquals(0, queue.byteCount)
        assertNull(queue.drain())
    }

    @Test
    fun offerCopiesInputBytes() {
        val queue = PendingByteQueue(maxBytes = 4)
        val payload = byteArrayOf(1, 2)

        assertTrue(queue.offer(payload))
        payload[0] = 9

        assertArrayEquals(byteArrayOf(1, 2), queue.drain())
    }
}