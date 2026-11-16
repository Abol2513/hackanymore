package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import io.hexforge.core.chronicle.ChronicleBus
import io.hexforge.core.common.ExecutionTier
import io.hexforge.core.common.OperationScope
import io.hexforge.core.common.ReversibleKind
import io.hexforge.core.common.TargetDescriptor
import io.hexforge.core.database.HexForgeDatabase
import io.hexforge.core.database.TechniqueCardEntity
import io.hexforge.core.gatekeeper.GatekeeperEngine
import io.hexforge.core.gatekeeper.GatekeeperResult
import io.hexforge.core.vault.VaultEngine
import io.hexforge.core.vault.VaultRollbackResult
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import kotlin.io.path.createTempDirectory

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class HexForgeDatabaseRobolectricTest {

    private lateinit var db: HexForgeDatabase
    private lateinit var vaultTempDir: File

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, HexForgeDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        vaultTempDir = createTempDirectory("hexforge_room_vault").toFile()
    }

    @After
    fun tearDown() {
        db.close()
        vaultTempDir.deleteRecursively()
    }

    @Test
    fun roomPersistenceRoundTripsForChronicleVaultGatekeeperAndCodexFts4() = runTest {
        val chronicle = ChronicleBus(chronicleDao = db.chronicleDao())
        val vault = VaultEngine(
            vaultRootDir = vaultTempDir,
            vaultDao = db.vaultDao(),
            chronicleBus = chronicle,
        )
        val gatekeeper = GatekeeperEngine(
            gatekeeperDao = db.gatekeeperDao(),
            chronicleBus = chronicle,
        )

        val offlineTarget = TargetDescriptor(
            packageName = "io.hexforge.demo.offline",
            displayNameEn = "Demo Offline",
            displayNameFa = "دموی آفلاین",
            versionName = "1.0",
        )
        val clearance = gatekeeper.requestClearance(offlineTarget, OperationScope.STATIC_APK_PATCH)
        assertTrue(clearance is GatekeeperResult.Cleared)

        val payload = "PK\u0003\u0004MOCK_APK_BYTES".repeat(50).toByteArray()
        val backup = vault.backupBytes(
            rawBytes = payload,
            originalPath = "/data/app/demo/base.apk",
            targetPackage = offlineTarget.packageName,
            jobId = "room_test_job",
            kind = ReversibleKind.APK_ORIGINAL,
        )

        val storedVault = vault.entries.first()
        assertEquals(1, storedVault.size)
        assertEquals(backup.vaultId, storedVault.first().vaultId)

        val rollback = vault.rollbackBytes(backup.vaultId, ExecutionTier.T0_HEXSPACE)
        assertTrue(rollback is VaultRollbackResult.Success)
        assertEquals(payload.size, (rollback as VaultRollbackResult.Success).restoredBytes.size)

        db.techniqueDao().insertAll(
            listOf(
                TechniqueCardEntity(
                    techniqueId = "TC-MEM-08",
                    category = "TC-MEM",
                    titleEn = "Encrypted XOR-value scan",
                    titleFa = "اسکن مقدار رمزنگاری‌شده با XOR",
                    engineScope = "ALL",
                    difficulty = 3,
                    stepsEn = "Brute common XOR keys across adjacent memory fields.",
                    stepsFa = "آزمون کلیدهای رایج XOR روی فیلدهای مجاور حافظه.",
                    lastVerifiedDate = "2026-11-16",
                )
            )
        )
        val ftsHits = db.techniqueDao().searchFts("XOR")
        assertEquals(1, ftsHits.size)
        assertEquals("TC-MEM-08", ftsHits.first().techniqueId)
    }
}
