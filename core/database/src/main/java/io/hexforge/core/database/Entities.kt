package io.hexforge.core.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.PrimaryKey

@Entity(tableName = "chronicle_events")
data class ChronicleEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val eventUuid: String,
    val timestampMs: Long,
    val moduleCode: String,
    val tierCode: String,
    val severity: String,
    val phase: String,
    val targetId: String,
    val messageKey: String,
    val messageEn: String,
    val messageFa: String,
    val payloadJson: String,
    val reversibleRefId: String?,
)

@Entity(tableName = "vault_entries")
data class VaultEntryEntity(
    @PrimaryKey val vaultId: String,
    val targetPackage: String,
    val jobId: String,
    val kind: String,
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

@Entity(tableName = "gatekeeper_decisions")
data class GatekeeperDecisionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val targetPackage: String,
    val operationScope: String,
    val verdict: String,
    val policyRuleCode: String,
    val reasonEn: String,
    val reasonFa: String,
    val tokenId: String?,
    val timestampMs: Long,
)

@Entity(tableName = "technique_cards")
data class TechniqueCardEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "rowid")
    val rowId: Int = 0,
    val techniqueId: String,
    val category: String,
    val titleEn: String,
    val titleFa: String,
    val engineScope: String,
    val difficulty: Int,
    val stepsEn: String,
    val stepsFa: String,
    val lastVerifiedDate: String,
)

@Fts4(contentEntity = TechniqueCardEntity::class)
@Entity(tableName = "technique_cards_fts")
data class TechniqueCardFtsEntity(
    val techniqueId: String,
    val category: String,
    val titleEn: String,
    val titleFa: String,
    val engineScope: String,
    val stepsEn: String,
    val stepsFa: String,
)

@Entity(tableName = "forge_jobs")
data class ForgeJobEntity(
    @PrimaryKey val jobId: String,
    val targetPackage: String,
    val jobTitleEn: String,
    val jobTitleFa: String,
    val stagesJson: String,
    val currentStageIndex: Int,
    val status: String,
    val createdAtMs: Long,
    val updatedAtMs: Long,
    val killCount: Int,
)
