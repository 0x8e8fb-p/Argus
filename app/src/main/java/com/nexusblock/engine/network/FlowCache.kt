package com.nexusblock.engine.network

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stateful flow verdict cache (LRU eviction).
 *
 * Caches the final allow/block decision for a 4-tuple flow
 * (srcIp, dstIp, srcPort, dstPort) so repeated packets on the same
 * connection bypass deep inspection.
 */
@Singleton
class FlowCache @Inject constructor() {

    companion object {
        private const val MAX_SIZE = 4096
        private const val HALF_SIZE = MAX_SIZE / 2
    }

    enum class Verdict { ALLOW, DROP }

    /**
     * LRU LinkedHashMap. Access order = true promotes entries on get/put,
     * so eldest = least-recently-used.
     */
    private val cache = object : LinkedHashMap<Long, Verdict>(MAX_SIZE, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Long, Verdict>?): Boolean {
            return size > MAX_SIZE
        }
    }

    /**
     * Generates a 64-bit flow key from the 4-tuple.
     * Layout: [srcIp:32][dstIp:32] xor [srcPort:16][dstPort:16]
     */
    fun key(srcIp: Int, dstIp: Int, srcPort: Int, dstPort: Int): Long {
        val ipPair = (srcIp.toLong() shl 32) or (dstIp.toLong() and 0xFFFFFFFFL)
        val portPair = (srcPort.toLong() shl 16) or (dstPort.toLong() and 0xFFFFL)
        return ipPair xor (portPair shl 1)
    }

    @Synchronized
    fun get(key: Long): Verdict? = cache[key]

    @Synchronized
    fun put(key: Long, verdict: Verdict) {
        cache[key] = verdict
    }

    /** Flush half the cache when Android signals memory pressure. */
    @Synchronized
    fun trimMemory() {
        if (cache.size > HALF_SIZE) {
            val iter = cache.entries.iterator()
            val toRemove = cache.size - HALF_SIZE
            repeat(toRemove) {
                if (iter.hasNext()) {
                    iter.next()
                    iter.remove()
                }
            }
        }
    }

    @Synchronized
    fun clear() {
        cache.clear()
    }

    @Synchronized
    fun size(): Int = cache.size
}
