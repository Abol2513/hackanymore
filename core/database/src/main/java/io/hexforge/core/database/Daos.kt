package io.hexforge.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ChronicleDao {
    @Query("SELECT * FROM chronicle_events ORDER BY timestampMs DESC, id DESC LIMIT :limit")
    fun observeRecent(limit: Int = 300): Flow<List<ChronicleEventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: ChronicleEventEntity): Long

    @Query("DELETE FROM chronicle_events WHERE timestampMs < :cutoffMs")
    suspend fun deleteOlderThan(cutoffMs: Long): Int

    @Query("SELECT COUNT(*) FROM chronicle_events")
    suspend fun countAll(): Int
}

@Dao
interface VaultDao {
    @Query("SELECT * FROM vault_entries ORDER BY createdAtMs DESC")
    fun observeAll(): Flow<List<VaultEntryEntity>>

    @Query("SELECT * FROM vault_entries WHERE vaultId = :vaultId LIMIT 1")
    suspend fun getById(vaultId: String): VaultEntryEntity?

    @Query("SELECT * FROM vault_entries WHERE targetPackage = :pkg ORDER BY createdAtMs DESC")
    suspend fun getForTarget(pkg: String): List<VaultEntryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: VaultEntryEntity)

    @Query("UPDATE vault_entries SET isProtected = :isProtected WHERE vaultId = :vaultId")
    suspend fun setProtected(vaultId: String, isProtected: Boolean)

    @Query("UPDATE vault_entries SET isRolledBack = :rolledBack WHERE vaultId = :vaultId")
    suspend fun setRolledBack(vaultId: String, rolledBack: Boolean)

    @Query("DELETE FROM vault_entries WHERE vaultId = :vaultId")
    suspend fun deleteById(vaultId: String)

    @Query("SELECT COUNT(*) FROM vault_entries WHERE sha256Hex = :sha256 AND vaultId != :excludeVaultId")
    suspend fun countBlobReferences(sha256: String, excludeVaultId: String): Int

    @Query("SELECT * FROM vault_entries WHERE isProtected = 0 AND createdAtMs < :cutoffMs")
    suspend fun findUnprotectedOlderThan(cutoffMs: Long): List<VaultEntryEntity>
}

@Dao
interface GatekeeperDao {
    @Query("SELECT * FROM gatekeeper_decisions ORDER BY timestampMs DESC LIMIT :limit")
    fun observeDecisions(limit: Int = 100): Flow<List<GatekeeperDecisionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(decision: GatekeeperDecisionEntity): Long
}

@Dao
interface TechniqueDao {
    @Query("SELECT * FROM technique_cards ORDER BY techniqueId ASC")
    fun observeAll(): Flow<List<TechniqueCardEntity>>

    @Query(
        """
        SELECT technique_cards.* FROM technique_cards
        JOIN technique_cards_fts ON technique_cards.rowid = technique_cards_fts.rowid
        WHERE technique_cards_fts MATCH :query
        """
    )
    suspend fun searchFts(query: String): List<TechniqueCardEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(cards: List<TechniqueCardEntity>)
}

@Dao
interface ForgeJobDao {
    @Query("SELECT * FROM forge_jobs ORDER BY updatedAtMs DESC")
    fun observeJobs(): Flow<List<ForgeJobEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(job: ForgeJobEntity)
}
