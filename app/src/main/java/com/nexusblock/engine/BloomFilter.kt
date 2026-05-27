package com.nexusblock.engine

import java.util.BitSet
import kotlin.math.abs

/**
 * A high-performance probabilistic data structure for set membership.
 * Used to quickly determine if a domain might be in the blocklist.
 * 
 * Performance: O(k) where k is the number of hash functions (usually small, e.g., 3-5).
 * Memory: Fixed size bitset.
 */
class BloomFilter(
    private val size: Int = 1024 * 1024 * 4, // 512KB bitset (4 million bits)
    private val hashCount: Int = 3
) {
    private val bitSet = BitSet(size)

    /**
     * Adds an element to the filter.
     */
    fun add(element: String) {
        val normalized = element.lowercase().trim()
        for (i in 0 until hashCount) {
            val hash = hash(normalized, i)
            bitSet.set(abs(hash % size))
        }
    }

    /**
     * Checks if an element might be in the set.
     * Returns false if the element is definitely NOT in the set.
     * Returns true if the element MIGHT be in the set (with small false positive rate).
     */
    fun mightContain(element: String): Boolean {
        val normalized = element.lowercase().trim()
        for (i in 0 until hashCount) {
            val hash = hash(normalized, i)
            if (!bitSet.get(abs(hash % size))) {
                return false
            }
        }
        return true
    }

    /**
     * Clears the filter.
     */
    fun clear() {
        bitSet.clear()
    }

    /**
     * Simple multi-hash implementation using string hashCode and iteration.
     */
    private fun hash(element: String, index: Int): Int {
        var h = element.hashCode()
        // Mix the hash based on the index
        h = h xor (h ushr 16)
        h *= 0x85ebca6b.toInt()
        h = h xor (h ushr 13)
        h *= 0xc2b2ae35.toInt()
        h = h xor (h ushr 16)
        return h + index * 31
    }
}
