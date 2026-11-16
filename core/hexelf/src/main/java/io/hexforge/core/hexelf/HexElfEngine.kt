package io.hexforge.core.hexelf

import java.nio.ByteBuffer
import java.nio.ByteOrder

enum class DetectedBinaryFormat(val labelEn: String, val labelFa: String) {
    APK_OR_ZIP("APK / ZIP Archive", "بستهٔ APK / آرشیو ZIP"),
    ELF_BINARY("ELF Native Binary (.so)", "فایل اجرایی/کتابخانه بومی ELF"),
    DEX_BYTECODE("Dalvik Executable (DEX)", "بایت‌کد دالویک (DEX)"),
    IL2CPP_METADATA("Unity IL2CPP global-metadata.dat", "ابردادهٔ یونیتی IL2CPP"),
    SQLITE_DB("SQLite Database", "پایگاه‌داده SQLite"),
    AXML_OR_ARSC("Binary Android XML / ARSC", "XML باینری یا جدول منابع ARSC"),
    UNKNOWN_BINARY("Generic Binary", "فایل باینری عمومی"),
}

data class ElfHeaderSummary(
    val is64Bit: Boolean,
    val isLittleEndian: Boolean,
    val machineCode: Int,
    val elfType: Int,
)

/**
 * Magic-byte binary format sniffer and safe ELF header reader (Part 11 Stage 1).
 * Handles corrupt or truncated buffers gracefully without throwing (PD-12).
 */
object BinaryFormatSniffer {
    fun sniff(headerBytes: ByteArray): DetectedBinaryFormat {
        if (headerBytes.size < 4) return DetectedBinaryFormat.UNKNOWN_BINARY

        // ELF: 0x7F 'E' 'L' 'F'
        if (headerBytes[0] == 0x7F.toByte() &&
            headerBytes[1] == 'E'.code.toByte() &&
            headerBytes[2] == 'L'.code.toByte() &&
            headerBytes[3] == 'F'.code.toByte()
        ) {
            return DetectedBinaryFormat.ELF_BINARY
        }

        // ZIP / APK: 'P' 'K' 0x03 0x04
        if (headerBytes[0] == 'P'.code.toByte() &&
            headerBytes[1] == 'K'.code.toByte() &&
            headerBytes[2] == 0x03.toByte() &&
            headerBytes[3] == 0x04.toByte()
        ) {
            return DetectedBinaryFormat.APK_OR_ZIP
        }

        // DEX: 'd' 'e' 'x' '\n'
        if (headerBytes[0] == 'd'.code.toByte() &&
            headerBytes[1] == 'e'.code.toByte() &&
            headerBytes[2] == 'x'.code.toByte() &&
            headerBytes[3] == '\n'.code.toByte()
        ) {
            return DetectedBinaryFormat.DEX_BYTECODE
        }

        // IL2CPP global-metadata.dat little-endian sanity 0xFAB11BAF -> AF 1B B1 FA
        if (headerBytes[0] == 0xAF.toByte() &&
            headerBytes[1] == 0x1B.toByte() &&
            headerBytes[2] == 0xB1.toByte() &&
            headerBytes[3] == 0xFA.toByte()
        ) {
            return DetectedBinaryFormat.IL2CPP_METADATA
        }

        // SQLite format 3
        if (headerBytes.size >= 16) {
            val prefix = headerBytes.copyOfRange(0, 15).toString(Charsets.US_ASCII)
            if (prefix == "SQLite format 3") return DetectedBinaryFormat.SQLITE_DB
        }

        // Binary AXML (0x0003) or ARSC (0x0002) little-endian
        val typeLe = (headerBytes[0].toInt() and 0xFF) or ((headerBytes[1].toInt() and 0xFF) shl 8)
        if (typeLe == 0x0002 || typeLe == 0x0003) {
            return DetectedBinaryFormat.AXML_OR_ARSC
        }

        return DetectedBinaryFormat.UNKNOWN_BINARY
    }

    fun parseElfHeader(bytes: ByteArray): ElfHeaderSummary? {
        if (bytes.size < 20) return null
        if (sniff(bytes) != DetectedBinaryFormat.ELF_BINARY) return null
        val eiClass = bytes[4].toInt() and 0xFF
        val eiData = bytes[5].toInt() and 0xFF
        if (eiClass != 1 && eiClass != 2) return null
        if (eiData != 1 && eiData != 2) return null
        val order = if (eiData == 1) ByteOrder.LITTLE_ENDIAN else ByteOrder.BIG_ENDIAN
        val bb = ByteBuffer.wrap(bytes).order(order)
        val eType = bb.getShort(16).toInt() and 0xFFFF
        val eMachine = bb.getShort(18).toInt() and 0xFFFF
        return ElfHeaderSummary(
            is64Bit = eiClass == 2,
            isLittleEndian = eiData == 1,
            machineCode = eMachine,
            elfType = eType,
        )
    }
}
