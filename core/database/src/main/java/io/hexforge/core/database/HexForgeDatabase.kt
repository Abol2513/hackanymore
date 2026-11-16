package io.hexforge.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ChronicleEventEntity::class,
        VaultEntryEntity::class,
        GatekeeperDecisionEntity::class,
        TechniqueCardEntity::class,
        TechniqueCardFtsEntity::class,
        ForgeJobEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class HexForgeDatabase : RoomDatabase() {
    abstract fun chronicleDao(): ChronicleDao
    abstract fun vaultDao(): VaultDao
    abstract fun gatekeeperDao(): GatekeeperDao
    abstract fun techniqueDao(): TechniqueDao
    abstract fun forgeJobDao(): ForgeJobDao

    companion object {
        @Volatile
        private var instance: HexForgeDatabase? = null

        fun getInstance(context: Context): HexForgeDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    HexForgeDatabase::class.java,
                    "hexforge_core.db",
                ).build().also { instance = it }
            }
        }
    }
}
