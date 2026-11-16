package com.example

import io.hexforge.core.chronicle.ChronicleBus
import io.hexforge.core.chronicle.ChronicleMessageCatalog
import io.hexforge.core.common.ClearanceToken
import io.hexforge.core.common.EngineType
import io.hexforge.core.common.ExecutionTier
import io.hexforge.core.common.GatekeeperVerdict
import io.hexforge.core.common.ModuleId
import io.hexforge.core.common.OperationScope
import io.hexforge.core.common.ReversibleKind
import io.hexforge.core.common.TargetDescriptor
import io.hexforge.core.gatekeeper.GatekeeperEngine
import io.hexforge.core.gatekeeper.GatekeeperResult
import io.hexforge.core.hexcore.ProcMapsParser
import io.hexforge.core.hexcore.RegionKind
import io.hexforge.core.hexelf.BinaryFormatSniffer
import io.hexforge.core.hexelf.DetectedBinaryFormat
import io.hexforge.core.hexscan.InMemoryScanner
import io.hexforge.core.vault.Lz4BlockCodec
import io.hexforge.core.vault.VaultEngine
import io.hexforge.core.vault.VaultRollbackResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.io.path.createTempDirectory
import kotlin.random.Random

class GatekeeperAndVaultTest {

    @Test
    fun chronicleCatalogHasAtLeast120BilingualEntriesAcrossAllModules() {
        val count = ChronicleMessageCatalog.size()
        assertTrue("Expected >= 120 catalog entries, got $count", count >= 120)
        for (tpl in ChronicleMessageCatalog.allTemplates) {
            assertTrue("Blank EN template for ${tpl.key}", tpl.templateEn.isNotBlank())
            assertTrue("Blank FA template for ${tpl.key}", tpl.templateFa.isNotBlank())
        }
        val coveredModules = ChronicleMessageCatalog.allTemplates.map { it.module }.toSet()
        assertEquals(ModuleId.entries.toSet(), coveredModules)
    }

    @Test
    fun gatekeeperRefusesOnlineAndServerAuthoritativeTargetsAndLogsChronicle() {
        val chronicle = ChronicleBus()
        val gatekeeper = GatekeeperEngine(chronicleBus = chronicle)

        val pubg = TargetDescriptor(
            packageName = "com.tencent.ig",
            displayNameEn = "PUBG Mobile",
            displayNameFa = "پابجی",
            versionName = "3.5",
        )
        val pubgResult = gatekeeper.requestClearance(pubg, OperationScope.MEMORY_SCAN_WRITE)
        assertTrue(pubgResult is GatekeeperResult.Denied)

        val supercell = TargetDescriptor(
            packageName = "com.supercell.clashofclans",
            displayNameEn = "Clash",
            displayNameFa = "کلش",
            versionName = "16.0",
        )
        val supercellResult = gatekeeper.requestClearance(supercell, OperationScope.STATIC_APK_PATCH)
        assertTrue(supercellResult is GatekeeperResult.Denied)

        val hybridTarget = TargetDescriptor(
            packageName = "com.indie.hybridgame",
            displayNameEn = "Hybrid Game",
            displayNameFa = "بازی هیبرید",
            versionName = "1.2",
            hasAlwaysOnSockets = true,
            hasAccountLoginFlow = true,
            isOfflineVerified = true,
        )
        val memBlocked = gatekeeper.requestClearance(hybridTarget, OperationScope.MEMORY_SCAN_WRITE)
        assertTrue(memBlocked is GatekeeperResult.Denied)
        val adsCleared = gatekeeper.requestClearance(hybridTarget, OperationScope.AD_AND_TRACKER_HYGIENE)
        assertTrue(adsCleared is GatekeeperResult.Cleared)

        val offlineGame = TargetDescriptor(
            packageName = "io.hexforge.sandbox.knightquest",
            displayNameEn = "KnightQuest",
            displayNameFa = "ماجراجویی شوالیه",
            versionName = "1.0",
            engineType = EngineType.UNITY_IL2CPP,
            isOfflineVerified = true,
        )
        val cleared = gatekeeper.requestClearance(offlineGame, OperationScope.MEMORY_SCAN_WRITE)
        assertTrue(cleared is GatekeeperResult.Cleared)
        val token = (cleared as GatekeeperResult.Cleared).token
        assertTrue(gatekeeper.verifyToken(token, offlineGame.packageName, OperationScope.MEMORY_SCAN_WRITE))
        assertFalse(gatekeeper.verifyToken(token, "com.other.pkg", OperationScope.MEMORY_SCAN_WRITE))

        val forged = ClearanceToken.mintInternal(
            tokenId = "forged",
            targetPackage = offlineGame.packageName,
            operationScope = OperationScope.MEMORY_SCAN_WRITE,
            verdict = GatekeeperVerdict.CLEARED_OFFLINE,
            issuedAtEpochMs = 1L,
            sealSecret = ClearanceToken.GATEKEEPER_INTERNAL_SEAL,
            signatureDigest = "0000",
        )
        assertFalse(gatekeeper.verifyToken(forged, offlineGame.packageName, OperationScope.MEMORY_SCAN_WRITE))
        assertTrue(chronicle.latestTail.value.size >= 6)
    }

    @Test
    fun lz4CodecRoundTripsAndRejectsCorruptData() {
        val payloads = listOf(
            ByteArray(0),
            "short".toByteArray(),
            ByteArray(4096) { 'A'.code.toByte() },
            ("HEXFORGE_REPEAT_BLOCK_0123456789_".repeat(120)).toByteArray(),
            Random(42).nextBytes(16_384),
        )
        for (raw in payloads) {
            val compressed = Lz4BlockCodec.compress(raw)
            val restored = Lz4BlockCodec.decompress(compressed)
            assertArrayEquals(raw, restored)
        }

        val failed = runCatching { Lz4BlockCodec.decompress(byteArrayOf(1, 2, 3, 4)) }
        assertTrue(failed.isFailure)
    }

    @Test
    fun vaultDeduplicatesAndRestoresFileWithSha256Verification() = runTest {
        val tempRoot = createTempDirectory("hexforge_vault_test").toFile()
        try {
            val chronicle = ChronicleBus()
            val vault = VaultEngine(vaultRootDir = tempRoot, chronicleBus = chronicle)
            val workFile = File(tempRoot, "save.json")
            val originalContent = "{\"coins\":1250,\"level\":5,\"inventory\":[\"sword\",\"shield\"]}".repeat(20)
            workFile.writeText(originalContent)

            val rec1 = vault.backupFile(workFile, "io.hexforge.sandbox", "job1", ReversibleKind.FILE_SNAPSHOT)
            val rec2 = vault.backupFile(workFile, "io.hexforge.sandbox", "job2", ReversibleKind.FILE_SNAPSHOT)
            assertEquals(rec1.sha256Hex, rec2.sha256Hex)
            assertTrue(chronicle.latestTail.value.any { it.messageKey == "M12_BACKUP_DEDUP" })

            // Mutate file and verify Vault rolls back exact original bytes
            workFile.writeText("CORRUPTED_BY_EXPERIMENT")
            val rollback = vault.rollbackToFile(rec1.vaultId, workFile, ExecutionTier.T0_HEXSPACE)
            assertTrue(rollback is VaultRollbackResult.Success)
            assertEquals(originalContent, workFile.readText())

            // Toggle protection and verify protected item survives purge
            assertTrue(vault.toggleProtected(rec1.vaultId))
            val purged = vault.purgeUnprotectedOlderThan(maxAgeMs = -1L)
            assertEquals(1, purged)
            val protectedStillRestorable = vault.rollbackBytes(rec1.vaultId)
            assertTrue(protectedStillRestorable is VaultRollbackResult.Success)
        } finally {
            tempRoot.deleteRecursively()
        }
    }

    @Test
    fun coreParsersAndScannersOperateDeterministically() {
        val maps = """
            70000000-70010000 rw-p 00000000 00:00 0 [anon:dalvik-main]
            71000000-71020000 rw-p 00000000 00:00 0 [heap]
            74000000-74080000 r-xp 00000000 fd:01 42 /data/app/libil2cpp.so
        """.trimIndent()
        val regions = ProcMapsParser.parse(maps)
        assertEquals(3, regions.size)
        assertEquals(RegionKind.JAVA_HEAP, regions[0].kind)
        assertEquals(RegionKind.CPP_HEAP, regions[1].kind)
        assertEquals(RegionKind.EXECUTABLE, regions[2].kind)

        val buf = ByteBuffer.allocate(24).order(ByteOrder.LITTLE_ENDIAN)
            .putInt(10).putInt(777).putInt(20).putInt(777).putInt(0x5C3B4A99).putInt(0)
            .array()
        val hits = InMemoryScanner.scanExactInt32(buf, 0x1000L, 777)
        assertEquals(2, hits.size)
        assertEquals(0x1004L, hits[0].address)

        val aobBuf = byteArrayOf(0x00, 0x4A, 0x11, 0x3B, 0x22, 0x33, 0x5C, 0xFF.toByte())
        val aobHits = InMemoryScanner.scanAob(aobBuf, 0x2000L, "4A ?? 3B ?? ?? 5C")
        assertEquals(listOf(0x2001L), aobHits)

        val elf64 = ByteArray(24).also {
            it[0] = 0x7F; it[1] = 'E'.code.toByte(); it[2] = 'L'.code.toByte(); it[3] = 'F'.code.toByte()
            it[4] = 2; it[5] = 1
            it[16] = 3; it[18] = 183.toByte() // ET_DYN, EM_AARCH64
        }
        assertEquals(DetectedBinaryFormat.ELF_BINARY, BinaryFormatSniffer.sniff(elf64))
        val elfSummary = BinaryFormatSniffer.parseElfHeader(elf64)
        assertNotNull(elfSummary)
        assertTrue(elfSummary!!.is64Bit)
        assertEquals(183, elfSummary.machineCode)

        val il2cppMeta = byteArrayOf(0xAF.toByte(), 0x1B.toByte(), 0xB1.toByte(), 0xFA.toByte())
        assertEquals(DetectedBinaryFormat.IL2CPP_METADATA, BinaryFormatSniffer.sniff(il2cppMeta))
    }
}
