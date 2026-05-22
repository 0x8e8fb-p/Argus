package com.nexusblock.engine

internal class PendingByteQueue(
    private val maxBytes: Int
) {
    private val segments = ArrayDeque<ByteArray>()
    var byteCount: Int = 0
        private set

    init {
        require(maxBytes >= 0) { "Pending byte cap must be non-negative" }
    }

    fun offer(data: ByteArray): Boolean {
        if (data.isEmpty()) return true
        if (byteCount + data.size > maxBytes) return false

        segments.addLast(data.copyOf())
        byteCount += data.size
        return true
    }

    fun drain(): ByteArray? {
        if (byteCount == 0) return null

        val drained = ByteArray(byteCount)
        var destinationOffset = 0
        for (segment in segments) {
            segment.copyInto(drained, destinationOffset)
            destinationOffset += segment.size
        }
        segments.clear()
        byteCount = 0
        return drained
    }

    fun clear() {
        segments.clear()
        byteCount = 0
    }
}