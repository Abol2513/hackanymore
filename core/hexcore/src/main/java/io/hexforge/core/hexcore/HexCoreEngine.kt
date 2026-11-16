package io.hexforge.core.hexcore

import io.hexforge.core.common.ClearanceToken

enum class RegionKind(val chipCode: String, val labelEn: String, val labelFa: String) {
    JAVA_HEAP("JH", "Java Heap", "هیپ جاوا"),
    CPP_HEAP("CH", "C++ Heap", "هیپ C++"),
    ANONYMOUS("A", "Anonymous", "ناشناس"),
    STACK("S", "Stack", "پشته"),
    LIBRARY("LIB", "Shared Lib (.so)", "کتابخانهٔ بومی"),
    EXECUTABLE("X", "Executable", "اجرایی"),
    OTHER("O", "Other", "سایر"),
}

data class MemoryRegion(
    val startAddress: Long,
    val endAddress: Long,
    val permissions: String,
    val offset: Long,
    val path: String,
    val kind: RegionKind,
) {
    val sizeBytes: Long get() = (endAddress - startAddress).coerceAtLeast(0L)
    val isReadable: Boolean get() = permissions.getOrNull(0) == 'r'
    val isWritable: Boolean get() = permissions.getOrNull(1) == 'w'
    val isExecutable: Boolean get() = permissions.getOrNull(2) == 'x'
}

enum class MemoryAccessMethod(val labelEn: String, val labelFa: String) {
    PROCESS_VM_READV("process_vm_readv/writev", "process_vm_readv/writev"),
    PTRACE_PEEK_POKE("ptrace PEEK/POKE", "ptrace PEEK/POKE"),
    PROC_MEM("/proc/<pid>/mem", "/proc/<pid>/mem"),
    IN_MEMORY_SANDBOX("In-Memory Test Buffer", "بافر حافظهٔ آزمایشی"),
}

/**
 * Parses Linux /proc/<pid>/maps lines into classified HEXFORGE memory regions (Part 8 M1.1).
 */
object ProcMapsParser {
    private val mapLineRegex = Regex(
        """^([0-9a-fA-F]+)-([0-9a-fA-F]+)\s+([rwxps-]{4})\s+([0-9a-fA-F]+)\s+\S+\s+\d+\s*(.*)$"""
    )

    fun parse(mapsContent: String): List<MemoryRegion> {
        return mapsContent.lineSequence()
            .mapNotNull { line -> parseLine(line.trim()) }
            .toList()
    }

    fun parseLine(line: String): MemoryRegion? {
        if (line.isBlank()) return null
        val match = mapLineRegex.matchEntire(line) ?: return null
        val start = match.groupValues[1].toULongOrNull(16)?.toLong() ?: return null
        val end = match.groupValues[2].toULongOrNull(16)?.toLong() ?: return null
        val perms = match.groupValues[3]
        val offset = match.groupValues[4].toULongOrNull(16)?.toLong() ?: 0L
        val path = match.groupValues[5].trim()
        return MemoryRegion(
            startAddress = start,
            endAddress = end,
            permissions = perms,
            offset = offset,
            path = path,
            kind = classifyRegion(perms, path),
        )
    }

    private fun classifyRegion(perms: String, path: String): RegionKind {
        return when {
            path.contains("[stack", ignoreCase = true) -> RegionKind.STACK
            path.contains("dalvik", ignoreCase = true) -> RegionKind.JAVA_HEAP
            path.contains("[heap]", ignoreCase = true) || path.contains("scudo", ignoreCase = true) ||
                path.contains("jemalloc", ignoreCase = true) -> RegionKind.CPP_HEAP
            path.endsWith(".so") || path.contains(".so (") -> {
                if (perms.getOrNull(2) == 'x') RegionKind.EXECUTABLE else RegionKind.LIBRARY
            }
            perms.getOrNull(2) == 'x' -> RegionKind.EXECUTABLE
            path.isEmpty() || path.startsWith("[anon:") -> RegionKind.ANONYMOUS
            else -> RegionKind.OTHER
        }
    }
}

interface ProcessMemoryBridge {
    val activeMethod: MemoryAccessMethod
    fun listRegions(pid: Int, clearance: ClearanceToken): List<MemoryRegion>
    fun readBytes(pid: Int, address: Long, length: Int, clearance: ClearanceToken): ByteArray
    fun writeBytes(pid: Int, address: Long, data: ByteArray, clearance: ClearanceToken): Boolean
}

/**
 * Deterministic in-memory bridge used during M1 foundations and JVM/Robolectric tests
 * before M3 binds JNI libhexcore.so against HexSpace child processes.
 */
class InMemoryProcessBridge(
    initialBacking: ByteArray = ByteArray(4096),
    private val baseAddress: Long = 0x7000_0000L,
) : ProcessMemoryBridge {
    override val activeMethod: MemoryAccessMethod = MemoryAccessMethod.IN_MEMORY_SANDBOX
    private val memory: ByteArray = initialBacking.copyOf()

    override fun listRegions(pid: Int, clearance: ClearanceToken): List<MemoryRegion> {
        return listOf(
            MemoryRegion(
                startAddress = baseAddress,
                endAddress = baseAddress + memory.size,
                permissions = "rw-p",
                offset = 0L,
                path = "[anon:hexspace_sandbox]",
                kind = RegionKind.ANONYMOUS,
            )
        )
    }

    override fun readBytes(pid: Int, address: Long, length: Int, clearance: ClearanceToken): ByteArray {
        require(length >= 0) { "Length must be non-negative" }
        val offset = (address - baseAddress).toInt()
        if (offset < 0 || offset + length > memory.size) {
            throw IndexOutOfBoundsException("Address 0x${address.toString(16)} out of mapped bounds")
        }
        return memory.copyOfRange(offset, offset + length)
    }

    override fun writeBytes(pid: Int, address: Long, data: ByteArray, clearance: ClearanceToken): Boolean {
        val offset = (address - baseAddress).toInt()
        if (offset < 0 || offset + data.size > memory.size) return false
        System.arraycopy(data, 0, memory, offset, data.size)
        return true
    }
}
