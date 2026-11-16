package io.hexforge.core.vault

import io.hexforge.core.chronicle.ChronicleBus
import io.hexforge.core.common.ExecutionTier
import io.hexforge.core.common.ReversibleKind
import io.hexforge.core.database.VaultDao
import io.hexforge.core.database.VaultEntryEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.io.File
import java.security.MessageDigest
import java.util.UUID

data class VaultBackupRecord(
    val vaultId: String,
    val targetPackage: String,
    val jobId: String,
    val kind: ReversibleKind,
    val originalPath: String,
    val sha256Hex: String,
    val blobRelativePath: String,
    val originalSizeBytes: Long,
    val compressedSizeBytes: Long,
    val createdAtMs: Long,
    val isProtected: Boolean,
    val isRolledBack: Boolean,
    val summaryEn: String,
    val summaryFa: String,
)

sealed interface VaultRollbackResult {
    data class Success(val vaultId: String, val restoredBytes: ByteArray, val sha256Hex: String) : VaultRollbackResult
    data class Failure(val vaultId: String, val reasonEn: String, val reasonFa: String) : VaultRollbackResult
}

/**
 * Content-addressed (SHA-256), LZ4-compressed backup and rollback engine (Part 19 M12, PD-7).
 */
class VaultEngine(
    private val vaultRootDir: File,
    private val vaultDao: VaultDao? = null,
    private val chronicleBus: ChronicleBus? = null,
) {
    private val blobsDir: File = File(vaultRootDir, "blobs").apply { mkdirs() }
    private val memoryIndex = MutableStateFlow<List<VaultBackupRecord>>(emptyList())

    val entries: Flow<List<VaultBackupRecord>> =
        vaultDao?.observeAll()?.map { list -> list.map { it.toDomain() } }
            ?: memoryIndex.asStateFlow()

    suspend fun backupFile(
        sourceFile: File,
        targetPackage: String,
        jobId: String,
        kind: ReversibleKind,
        tier: ExecutionTier = ExecutionTier.T0_HEXSPACE,
        summaryEn: String = "Pre-operation backup of ${sourceFile.name}",
        summaryFa: String = "نسخهٔ پشتیبان پیش از عملیات برای ${sourceFile.name}",
    ): VaultBackupRecord {
        require(sourceFile.exists() && sourceFile.isFile) {
            "Cannot back up missing file: ${sourceFile.absolutePath}"
        }
        return backupBytes(
            rawBytes = sourceFile.readBytes(),
            originalPath = sourceFile.absolutePath,
            targetPackage = targetPackage,
            jobId = jobId,
            kind = kind,
            tier = tier,
            summaryEn = summaryEn,
            summaryFa = summaryFa,
        )
    }

    suspend fun backupBytes(
        rawBytes: ByteArray,
        originalPath: String,
        targetPackage: String,
        jobId: String,
        kind: ReversibleKind,
        tier: ExecutionTier = ExecutionTier.T0_HEXSPACE,
        summaryEn: String = "Snapshot of $originalPath",
        summaryFa: String = "نسخهٔ پشتیبان از $originalPath",
    ): VaultBackupRecord {
        blobsDir.mkdirs()
        val sha256 = sha256Hex(rawBytes)
        val blobRelPath = "blobs/$sha256.hxlz"
        val blobFile = File(vaultRootDir, blobRelPath)
        val deduplicated = blobFile.exists()

        val compressedBytes = if (!deduplicated) {
            val compressed = Lz4BlockCodec.compress(rawBytes)
            blobFile.writeBytes(compressed)
            compressed.size.toLong()
        } else {
            blobFile.length()
        }

        val vaultId = "vlt_${UUID.randomUUID().toString().take(8)}"
        val record = VaultBackupRecord(
            vaultId = vaultId,
            targetPackage = targetPackage,
            jobId = jobId,
            kind = kind,
            originalPath = originalPath,
            sha256Hex = sha256,
            blobRelativePath = blobRelPath,
            originalSizeBytes = rawBytes.size.toLong(),
            compressedSizeBytes = compressedBytes,
            createdAtMs = System.currentTimeMillis(),
            isProtected = false,
            isRolledBack = false,
            summaryEn = summaryEn,
            summaryFa = summaryFa,
        )

        if (vaultDao != null) {
            vaultDao.upsert(record.toEntity())
        } else {
            memoryIndex.update { list -> listOf(record) + list }
        }

        if (deduplicated) {
            chronicleBus?.emit(
                messageKey = "M12_BACKUP_DEDUP",
                tier = tier,
                targetId = targetPackage,
                params = mapOf(
                    "sha256" to sha256.take(12),
                    "vaultId" to vaultId,
                ),
                reversibleRefId = vaultId,
            )
        } else {
            chronicleBus?.emit(
                messageKey = "M12_BACKUP_CREATED",
                tier = tier,
                targetId = targetPackage,
                params = mapOf(
                    "path" to originalPath,
                    "origBytes" to rawBytes.size.toString(),
                    "compBytes" to compressedBytes.toString(),
                    "sha256" to sha256.take(12),
                    "vaultId" to vaultId,
                ),
                reversibleRefId = vaultId,
            )
        }
        return record
    }

    suspend fun rollbackBytes(
        vaultId: String,
        tier: ExecutionTier = ExecutionTier.T0_HEXSPACE,
    ): VaultRollbackResult {
        val record = findRecord(vaultId)
            ?: return VaultRollbackResult.Failure(
                vaultId = vaultId,
                reasonEn = "Vault entry $vaultId not found.",
                reasonFa = "رکورد والت با شناسهٔ $vaultId یافت نشد.",
            )

        chronicleBus?.emit(
            messageKey = "M12_ROLLBACK_START",
            tier = tier,
            targetId = record.targetPackage,
            params = mapOf(
                "vaultId" to vaultId,
                "path" to record.originalPath,
            ),
            reversibleRefId = vaultId,
        )

        val blobFile = File(vaultRootDir, record.blobRelativePath)
        if (!blobFile.exists()) {
            chronicleBus?.emit(
                messageKey = "M12_ROLLBACK_INTEGRITY_FAIL",
                tier = tier,
                targetId = record.targetPackage,
                params = mapOf(
                    "vaultId" to vaultId,
                    "expected" to record.sha256Hex.take(12),
                ),
            )
            return VaultRollbackResult.Failure(
                vaultId = vaultId,
                reasonEn = "Backup blob missing on disk for $vaultId.",
                reasonFa = "فایل پشتیبان روی دیسک برای $vaultId یافت نشد.",
            )
        }

        val restored = try {
            Lz4BlockCodec.decompress(blobFile.readBytes())
        } catch (t: Throwable) {
            chronicleBus?.emit(
                messageKey = "M12_ROLLBACK_INTEGRITY_FAIL",
                tier = tier,
                targetId = record.targetPackage,
                params = mapOf(
                    "vaultId" to vaultId,
                    "expected" to record.sha256Hex.take(12),
                ),
            )
            return VaultRollbackResult.Failure(
                vaultId = vaultId,
                reasonEn = "Backup blob corrupted: ${t.message}",
                reasonFa = "فایل پشتیبان آسیب دیده است: ${t.message}",
            )
        }

        val actualSha = sha256Hex(restored)
        if (actualSha != record.sha256Hex) {
            chronicleBus?.emit(
                messageKey = "M12_ROLLBACK_INTEGRITY_FAIL",
                tier = tier,
                targetId = record.targetPackage,
                params = mapOf(
                    "vaultId" to vaultId,
                    "expected" to record.sha256Hex.take(12),
                ),
            )
            return VaultRollbackResult.Failure(
                vaultId = vaultId,
                reasonEn = "SHA-256 verification failed during rollback.",
                reasonFa = "راستی‌آزمایی SHA-256 هنگام بازگردانی ناموفق بود.",
            )
        }

        markRolledBack(vaultId)
        chronicleBus?.emit(
            messageKey = "M12_ROLLBACK_OK",
            tier = tier,
            targetId = record.targetPackage,
            params = mapOf(
                "path" to record.originalPath,
                "vaultId" to vaultId,
                "sha256" to actualSha.take(12),
            ),
            reversibleRefId = vaultId,
        )
        return VaultRollbackResult.Success(vaultId, restored, actualSha)
    }

    suspend fun rollbackToFile(
        vaultId: String,
        destinationFile: File,
        tier: ExecutionTier = ExecutionTier.T0_HEXSPACE,
    ): VaultRollbackResult {
        val result = rollbackBytes(vaultId, tier)
        if (result is VaultRollbackResult.Success) {
            destinationFile.parentFile?.mkdirs()
            destinationFile.writeBytes(result.restoredBytes)
        }
        return result
    }

    suspend fun toggleProtected(vaultId: String, tier: ExecutionTier = ExecutionTier.T0_HEXSPACE): Boolean {
        val record = findRecord(vaultId) ?: return false
        val next = !record.isProtected
        if (vaultDao != null) {
            vaultDao.setProtected(vaultId, next)
        } else {
            memoryIndex.update { list ->
                list.map { if (it.vaultId == vaultId) it.copy(isProtected = next) else it }
            }
        }
        chronicleBus?.emit(
            messageKey = "M12_PIN_TOGGLED",
            tier = tier,
            targetId = record.targetPackage,
            params = mapOf("vaultId" to vaultId, "protected" to next.toString()),
            reversibleRefId = vaultId,
        )
        return next
    }

    suspend fun purgeUnprotectedOlderThan(
        maxAgeMs: Long,
        nowMs: Long = System.currentTimeMillis(),
        tier: ExecutionTier = ExecutionTier.T0_HEXSPACE,
    ): Int {
        val cutoff = nowMs - maxAgeMs
        val candidates = if (vaultDao != null) {
            vaultDao.findUnprotectedOlderThan(cutoff)
        } else {
            memoryIndex.value.filter { !it.isProtected && it.createdAtMs < cutoff }.map { it.toEntity() }
        }
        var freedBytes = 0L
        var removedCount = 0
        for (entry in candidates) {
            val sharedRefs = if (vaultDao != null) {
                vaultDao.countBlobReferences(entry.sha256Hex, entry.vaultId)
            } else {
                memoryIndex.value.count { it.sha256Hex == entry.sha256Hex && it.vaultId != entry.vaultId }
            }
            if (sharedRefs == 0) {
                val blob = File(vaultRootDir, entry.blobRelativePath)
                if (blob.exists()) {
                    freedBytes += blob.length()
                    blob.delete()
                }
            }
            if (vaultDao != null) {
                vaultDao.deleteById(entry.vaultId)
            } else {
                memoryIndex.update { list -> list.filterNot { it.vaultId == entry.vaultId } }
            }
            removedCount++
        }
        if (removedCount > 0) {
            chronicleBus?.emit(
                messageKey = "M12_CLEANUP_PURGED",
                tier = tier,
                targetId = "vault",
                params = mapOf("count" to removedCount.toString(), "freedBytes" to freedBytes.toString()),
            )
        }
        return removedCount
    }

    private suspend fun findRecord(vaultId: String): VaultBackupRecord? {
        return vaultDao?.getById(vaultId)?.toDomain()
            ?: memoryIndex.value.firstOrNull { it.vaultId == vaultId }
    }

    private suspend fun markRolledBack(vaultId: String) {
        if (vaultDao != null) {
            vaultDao.setRolledBack(vaultId, true)
        } else {
            memoryIndex.update { list ->
                list.map { if (it.vaultId == vaultId) it.copy(isRolledBack = true) else it }
            }
        }
    }

    companion object {
        fun sha256Hex(bytes: ByteArray): String {
            val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
            return digest.joinToString("") { "%02x".format(it) }
        }
    }

    private fun VaultBackupRecord.toEntity() = VaultEntryEntity(
        vaultId = vaultId,
        targetPackage = targetPackage,
        jobId = jobId,
        kind = kind.name,
        originalPath = originalPath,
        sha256Hex = sha256Hex,
        blobRelativePath = blobRelativePath,
        originalSizeBytes = originalSizeBytes,
        compressedSizeBytes = compressedSizeBytes,
        createdAtMs = createdAtMs,
        isProtected = isProtected,
        isRolledBack = isRolledBack,
        summaryEn = summaryEn,
        summaryFa = summaryFa,
    )

    private fun VaultEntryEntity.toDomain() = VaultBackupRecord(
        vaultId = vaultId,
        targetPackage = targetPackage,
        jobId = jobId,
        kind = ReversibleKind.entries.firstOrNull { it.name == kind } ?: ReversibleKind.FILE_SNAPSHOT,
        originalPath = originalPath,
        sha256Hex = sha256Hex,
        blobRelativePath = blobRelativePath,
        originalSizeBytes = originalSizeBytes,
        compressedSizeBytes = compressedSizeBytes,
        createdAtMs = createdAtMs,
        isProtected = isProtected,
        isRolledBack = isRolledBack,
        summaryEn = summaryEn,
        summaryFa = summaryFa,
    )
}
