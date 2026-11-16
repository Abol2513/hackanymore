package io.hexforge.core.hexscan

import io.hexforge.core.hexcore.RegionKind
import java.nio.ByteBuffer
import java.nio.ByteOrder

enum class ScanValueType(val byteWidth: Int) {
    INT_8(1),
    INT_16(2),
    INT_32(4),
    INT_64(8),
    FLOAT_32(4),
    FLOAT_64(8),
}

enum class FuzzyCompareMode {
    CHANGED,
    UNCHANGED,
    INCREASED,
    DECREASED,
}

data class ScanCandidate(
    val address: Long,
    val regionKind: RegionKind,
    val valueType: ScanValueType,
    val rawBytes: ByteArray,
    val confidenceScore: Float,
) {
    fun asInt32(): Int {
        if (rawBytes.size < 4) return 0
        return ByteBuffer.wrap(rawBytes).order(ByteOrder.LITTLE_ENDIAN).int
    }

    fun asFloat32(): Float {
        if (rawBytes.size < 4) return 0f
        return ByteBuffer.wrap(rawBytes).order(ByteOrder.LITTLE_ENDIAN).float
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ScanCandidate) return false
        return address == other.address && valueType == other.valueType && rawBytes.contentEquals(other.rawBytes)
    }

    override fun hashCode(): Int {
        var result = address.hashCode()
        result = 31 * result + valueType.hashCode()
        result = 31 * result + rawBytes.contentHashCode()
        return result
    }
}

/**
 * Computes candidate plausibility scores (PD-8: filter false positives before showing raw addresses).
 */
object ConfidenceScorer {
    fun score(regionKind: RegionKind, address: Long, alignment: Int): Float {
        val regionWeight = when (regionKind) {
            RegionKind.CPP_HEAP -> 0.92f
            RegionKind.ANONYMOUS -> 0.88f
            RegionKind.LIBRARY -> 0.85f
            RegionKind.JAVA_HEAP -> 0.72f
            RegionKind.STACK -> 0.35f
            RegionKind.EXECUTABLE -> 0.25f
            RegionKind.OTHER -> 0.50f
        }
        val alignedBonus = if (alignment > 0 && address % alignment == 0L) 0.08f else -0.15f
        return (regionWeight + alignedBonus).coerceIn(0.05f, 1.0f)
    }
}

/**
 * Pure-Kotlin scan implementation mirroring native SIMD hexscan semantics for M1 verification.
 */
object InMemoryScanner {
    fun scanExactInt32(
        buffer: ByteArray,
        baseAddress: Long,
        targetValue: Int,
        regionKind: RegionKind = RegionKind.CPP_HEAP,
        step: Int = 4,
    ): List<ScanCandidate> {
        if (buffer.size < 4 || step <= 0) return emptyList()
        val results = mutableListOf<ScanCandidate>()
        val bb = ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN)
        var offset = 0
        while (offset + 4 <= buffer.size) {
            val value = bb.getInt(offset)
            if (value == targetValue) {
                val addr = baseAddress + offset
                results += ScanCandidate(
                    address = addr,
                    regionKind = regionKind,
                    valueType = ScanValueType.INT_32,
                    rawBytes = buffer.copyOfRange(offset, offset + 4),
                    confidenceScore = ConfidenceScorer.score(regionKind, addr, 4),
                )
            }
            offset += step
        }
        return results.sortedByDescending { it.confidenceScore }
    }

    /**
     * AOB pattern scan supporting ?? wildcards, e.g. "4A ?? 3B ?? ?? 5C".
     */
    fun scanAob(
        buffer: ByteArray,
        baseAddress: Long,
        pattern: String,
    ): List<Long> {
        val tokens = pattern.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (tokens.isEmpty() || buffer.size < tokens.size) return emptyList()
        val expected = IntArray(tokens.size) { idx ->
            val t = tokens[idx]
            if (t == "??" || t == "?") -1 else t.toInt(16) and 0xFF
        }
        val hits = mutableListOf<Long>()
        val limit = buffer.size - expected.size
        for (i in 0..limit) {
            var matched = true
            for (j in expected.indices) {
                val mask = expected[j]
                if (mask != -1 && (buffer[i + j].toInt() and 0xFF) != mask) {
                    matched = false
                    break
                }
            }
            if (matched) hits += (baseAddress + i)
        }
        return hits
    }
}
