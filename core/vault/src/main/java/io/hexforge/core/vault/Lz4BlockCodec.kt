package io.hexforge.core.vault

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Pure-Kotlin framed LZ4 block compressor and decompressor for Vault snapshots (Part 19 M12).
 * Frame layout: [H][X][L][Z] (4B magic) + [uncompressedLen] (4B int32 LE) + LZ4 block payload.
 */
object Lz4BlockCodec {
    private val MAGIC = byteArrayOf('H'.code.toByte(), 'X'.code.toByte(), 'L'.code.toByte(), 'Z'.code.toByte())
    private const val HASH_LOG = 14
    private const val HASH_SIZE = 1 shl HASH_LOG
    private const val MIN_MATCH = 4
    private const val LAST_LITERALS = 5
    private const val MF_LIMIT = 12
    private const val MAX_DISTANCE = 65535
    private const val MAX_DECOMPRESSED_BYTES = 256 * 1024 * 1024

    fun compress(input: ByteArray): ByteArray {
        val out = ByteArrayOutputStream(input.size + 16)
        out.write(MAGIC)
        val lenBytes = ByteArray(4)
        ByteBuffer.wrap(lenBytes).order(ByteOrder.LITTLE_ENDIAN).putInt(input.size)
        out.write(lenBytes)

        if (input.isEmpty()) return out.toByteArray()

        if (input.size < MF_LIMIT) {
            writeLiteralsOnly(out, input, 0, input.size)
            return out.toByteArray()
        }

        val hashTable = IntArray(HASH_SIZE) { -1 }
        var anchor = 0
        var pos = 0
        val limit = input.size - MF_LIMIT

        while (pos <= limit) {
            val h = hash4(input, pos)
            val candidate = hashTable[h]
            hashTable[h] = pos

            if (candidate >= 0 &&
                pos - candidate in 1..MAX_DISTANCE &&
                equals4(input, candidate, pos)
            ) {
                var matchLen = MIN_MATCH
                val maxMatch = input.size - LAST_LITERALS - pos
                while (matchLen < maxMatch && input[candidate + matchLen] == input[pos + matchLen]) {
                    matchLen++
                }

                val litLen = pos - anchor
                val offset = pos - candidate
                val mlCode = matchLen - MIN_MATCH

                val tokenLit = litLen.coerceAtMost(15)
                val tokenMatch = mlCode.coerceAtMost(15)
                out.write((tokenLit shl 4) or tokenMatch)

                if (litLen >= 15) writeExtraLength(out, litLen - 15)
                if (litLen > 0) out.write(input, anchor, litLen)

                out.write(offset and 0xFF)
                out.write((offset ushr 8) and 0xFF)

                if (mlCode >= 15) writeExtraLength(out, mlCode - 15)

                pos += matchLen
                anchor = pos
            } else {
                pos++
            }
        }

        val remaining = input.size - anchor
        if (remaining > 0) {
            writeLiteralsOnly(out, input, anchor, remaining)
        }
        return out.toByteArray()
    }

    fun decompress(framed: ByteArray): ByteArray {
        require(framed.size >= 8) { "Truncated Vault LZ4 frame" }
        for (i in 0..3) {
            require(framed[i] == MAGIC[i]) { "Invalid Vault LZ4 magic header" }
        }
        val origLen = ByteBuffer.wrap(framed, 4, 4).order(ByteOrder.LITTLE_ENDIAN).int
        require(origLen in 0..MAX_DECOMPRESSED_BYTES) { "Invalid decompressed size: $origLen" }
        if (origLen == 0) return ByteArray(0)

        val out = ByteArray(origLen)
        var inPos = 8
        var outPos = 0

        while (inPos < framed.size) {
            val token = framed[inPos++].toInt() and 0xFF
            var litLen = token ushr 4
            if (litLen == 15) {
                val (extra, nextIn) = readExtraLength(framed, inPos)
                litLen += extra
                inPos = nextIn
            }
            require(litLen >= 0 && inPos + litLen <= framed.size) { "Corrupt LZ4 literal bounds" }
            require(outPos + litLen <= origLen) { "Corrupt LZ4 output literal overflow" }
            System.arraycopy(framed, inPos, out, outPos, litLen)
            inPos += litLen
            outPos += litLen

            if (inPos >= framed.size) break

            require(inPos + 2 <= framed.size) { "Truncated LZ4 match offset" }
            val offset = (framed[inPos].toInt() and 0xFF) or ((framed[inPos + 1].toInt() and 0xFF) shl 8)
            inPos += 2
            require(offset in 1..outPos) { "Invalid LZ4 match offset $offset at outPos $outPos" }

            var matchLen = (token and 0x0F) + MIN_MATCH
            if ((token and 0x0F) == 15) {
                val (extra, nextIn) = readExtraLength(framed, inPos)
                matchLen += extra
                inPos = nextIn
            }
            require(outPos + matchLen <= origLen) { "Corrupt LZ4 match overflow" }
            for (k in 0 until matchLen) {
                out[outPos + k] = out[outPos - offset + k]
            }
            outPos += matchLen
        }

        require(outPos == origLen) { "Decompressed length mismatch: expected $origLen, got $outPos" }
        return out
    }

    private fun writeLiteralsOnly(out: ByteArrayOutputStream, input: ByteArray, start: Int, len: Int) {
        val tokenLit = len.coerceAtMost(15)
        out.write(tokenLit shl 4)
        if (len >= 15) writeExtraLength(out, len - 15)
        out.write(input, start, len)
    }

    private fun writeExtraLength(out: ByteArrayOutputStream, value: Int) {
        var rem = value
        while (rem >= 255) {
            out.write(255)
            rem -= 255
        }
        out.write(rem)
    }

    private fun readExtraLength(data: ByteArray, startPos: Int): Pair<Int, Int> {
        var pos = startPos
        var sum = 0
        while (true) {
            require(pos < data.size) { "Truncated LZ4 extended length" }
            val b = data[pos++].toInt() and 0xFF
            sum += b
            if (b != 255) break
        }
        return sum to pos
    }

    private fun hash4(data: ByteArray, pos: Int): Int {
        val v = (data[pos].toInt() and 0xFF) or
            ((data[pos + 1].toInt() and 0xFF) shl 8) or
            ((data[pos + 2].toInt() and 0xFF) shl 16) or
            ((data[pos + 3].toInt() and 0xFF) shl 24)
        return ((v * -1640531535) ushr (32 - HASH_LOG)) and (HASH_SIZE - 1)
    }

    private fun equals4(data: ByteArray, a: Int, b: Int): Boolean {
        return data[a] == data[b] &&
            data[a + 1] == data[b + 1] &&
            data[a + 2] == data[b + 2] &&
            data[a + 3] == data[b + 3]
    }
}
