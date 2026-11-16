package io.hexforge.core.gatekeeper

import io.hexforge.core.chronicle.ChronicleBus
import io.hexforge.core.common.ClearanceToken
import io.hexforge.core.common.ExecutionTier
import io.hexforge.core.common.GatekeeperVerdict
import io.hexforge.core.common.OperationScope
import io.hexforge.core.common.TargetDescriptor
import io.hexforge.core.database.GatekeeperDao
import io.hexforge.core.database.GatekeeperDecisionEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.util.UUID

data class GatekeeperAuditRecord(
    val targetPackage: String,
    val scope: OperationScope,
    val verdict: GatekeeperVerdict,
    val ruleCode: String,
    val reasonEn: String,
    val reasonFa: String,
    val tokenId: String?,
    val timestampMs: Long,
)

sealed interface GatekeeperResult {
    data class Cleared(
        val token: ClearanceToken,
        val ruleCode: String,
        val reasonEn: String,
        val reasonFa: String,
    ) : GatekeeperResult

    data class Denied(
        val verdict: GatekeeperVerdict,
        val ruleCode: String,
        val reasonEn: String,
        val reasonFa: String,
    ) : GatekeeperResult
}

/**
 * Structural ethics and offline-only policy gate (Part 13 M6, PD-1).
 * Engines CANNOT execute mutating or runtime operations without a verified ClearanceToken.
 */
class GatekeeperEngine(
    private val gatekeeperDao: GatekeeperDao? = null,
    private val chronicleBus: ChronicleBus? = null,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) {
    private val inMemoryAudit = MutableStateFlow<List<GatekeeperAuditRecord>>(emptyList())

    val auditLog: Flow<List<GatekeeperAuditRecord>> =
        gatekeeperDao?.observeDecisions(100)?.map { list ->
            list.map {
                GatekeeperAuditRecord(
                    targetPackage = it.targetPackage,
                    scope = OperationScope.entries.firstOrNull { s -> s.name == it.operationScope }
                        ?: OperationScope.DEEP_PROBE_INSPECT,
                    verdict = GatekeeperVerdict.entries.firstOrNull { v -> v.name == it.verdict }
                        ?: GatekeeperVerdict.DENIED_ONLINE_COMPETITIVE,
                    ruleCode = it.policyRuleCode,
                    reasonEn = it.reasonEn,
                    reasonFa = it.reasonFa,
                    tokenId = it.tokenId,
                    timestampMs = it.timestampMs,
                )
            }
        } ?: inMemoryAudit.asStateFlow()

    fun requestClearance(
        target: TargetDescriptor,
        operationScope: OperationScope,
        tier: ExecutionTier = ExecutionTier.T0_HEXSPACE,
    ): GatekeeperResult {
        val pkgRule = BlocklistRegistry.matchPackage(target.packageName)
        if (pkgRule != null) {
            val reasonEn = "Refused: ${target.packageName} matches online/competitive rule ${pkgRule.ruleCode} (${pkgRule.categoryEn}). HEXFORGE targets offline single-player games only."
            val reasonFa = "رد شد: بستهٔ ${target.packageName} مشمول قانون مسدودسازی ${pkgRule.ruleCode} (${pkgRule.categoryFa}) است. HEXFORGE فقط برای بازی‌های آفلاین تک‌نفره طراحی شده است."
            recordAudit(target.packageName, operationScope, GatekeeperVerdict.DENIED_ONLINE_COMPETITIVE, pkgRule.ruleCode, reasonEn, reasonFa, null)
            chronicleBus?.emit(
                messageKey = "M6_DENIED_BLOCKLIST",
                tier = tier,
                targetId = target.packageName,
                params = mapOf("target" to target.packageName, "rule" to pkgRule.ruleCode),
            )
            return GatekeeperResult.Denied(
                verdict = GatekeeperVerdict.DENIED_ONLINE_COMPETITIVE,
                ruleCode = pkgRule.ruleCode,
                reasonEn = reasonEn,
                reasonFa = reasonFa,
            )
        }

        val behavesServerAuthoritative =
            target.hasAntiCheatSdk || (target.hasAlwaysOnSockets && target.hasAccountLoginFlow) || !target.isOfflineVerified

        if (behavesServerAuthoritative) {
            val allowedLocalOnly = operationScope == OperationScope.AD_AND_TRACKER_HYGIENE ||
                operationScope == OperationScope.DEEP_PROBE_INSPECT

            if (!allowedLocalOnly) {
                val reasonEn = "Refused: ${target.packageName} exhibits server-authoritative economy or anti-cheat signals. Memory, billing, and save mods are locked; only local ad/tracker hygiene or read-only inspection is permitted."
                val reasonFa = "رد شد: هدف ${target.packageName} دارای اقتصاد سرورمحور، نشست آنلاین دائمی یا ضدتقلب است. ویرایش حافظه و پرداخت قفل شده و فقط تحلیل فقط-خواندنی یا حذف تبلیغات محلی مجاز است."
                recordAudit(target.packageName, operationScope, GatekeeperVerdict.DENIED_ONLINE_COMPETITIVE, "GK-HEUR-SERVER-AUTH", reasonEn, reasonFa, null)
                chronicleBus?.emit(
                    messageKey = "M6_DENIED_SERVER_AUTH",
                    tier = tier,
                    targetId = target.packageName,
                    params = mapOf("target" to target.packageName, "scope" to operationScope.labelEn),
                )
                return GatekeeperResult.Denied(
                    verdict = GatekeeperVerdict.DENIED_ONLINE_COMPETITIVE,
                    ruleCode = "GK-HEUR-SERVER-AUTH",
                    reasonEn = reasonEn,
                    reasonFa = reasonFa,
                )
            }

            val token = mintSignedToken(
                targetPackage = target.packageName,
                operationScope = operationScope,
                verdict = GatekeeperVerdict.LOCAL_HYGIENE_ONLY,
            )
            val reasonEn = "Restricted clearance: local hygiene/read-only scope permitted for hybrid target."
            val reasonFa = "مجوز محدود: صرفاً عملیات محلی حذف تبلیغات یا بازبینی فقط-خواندنی مجاز است."
            recordAudit(target.packageName, operationScope, GatekeeperVerdict.LOCAL_HYGIENE_ONLY, "GK-HYBRID-LOCAL-ONLY", reasonEn, reasonFa, token.tokenId)
            chronicleBus?.emit(
                messageKey = "M6_LIMITED_HYGIENE",
                tier = tier,
                targetId = target.packageName,
                params = mapOf("target" to target.packageName),
            )
            return GatekeeperResult.Cleared(token, "GK-HYBRID-LOCAL-ONLY", reasonEn, reasonFa)
        }

        val token = mintSignedToken(
            targetPackage = target.packageName,
            operationScope = operationScope,
            verdict = GatekeeperVerdict.CLEARED_OFFLINE,
        )
        val reasonEn = "Cleared: offline single-player target verified under PD-1."
        val reasonFa = "تأیید شد: هدف آفلاین تک‌نفره مطابق اصل PD-1 مجاز شناخته شد."
        recordAudit(target.packageName, operationScope, GatekeeperVerdict.CLEARED_OFFLINE, "GK-OFFLINE-OK", reasonEn, reasonFa, token.tokenId)
        chronicleBus?.emit(
            messageKey = "M6_CLEARANCE_GRANTED",
            tier = tier,
            targetId = target.packageName,
            params = mapOf(
                "target" to target.packageName,
                "scope" to operationScope.labelEn,
                "rule" to "GK-OFFLINE-OK",
                "tokenId" to token.tokenId,
            ),
        )
        return GatekeeperResult.Cleared(token, "GK-OFFLINE-OK", reasonEn, reasonFa)
    }

    fun verifyToken(
        token: ClearanceToken,
        expectedPackage: String,
        requiredScope: OperationScope,
    ): Boolean {
        if (token.targetPackage != expectedPackage || token.operationScope != requiredScope) {
            chronicleBus?.emit(
                messageKey = "M6_TOKEN_REJECTED",
                tier = ExecutionTier.T0_HEXSPACE,
                targetId = expectedPackage,
                params = mapOf("target" to expectedPackage),
            )
            return false
        }
        val expectedSig = computeSignatureDigest(
            token.tokenId,
            token.targetPackage,
            token.operationScope,
            token.verdict,
            token.issuedAtEpochMs,
        )
        val valid = token.signatureDigest == expectedSig
        if (!valid) {
            chronicleBus?.emit(
                messageKey = "M6_TOKEN_REJECTED",
                tier = ExecutionTier.T0_HEXSPACE,
                targetId = expectedPackage,
                params = mapOf("target" to expectedPackage),
            )
        }
        return valid
    }

    private fun mintSignedToken(
        targetPackage: String,
        operationScope: OperationScope,
        verdict: GatekeeperVerdict,
    ): ClearanceToken {
        val tokenId = "gk_${UUID.randomUUID().toString().take(8)}"
        val now = System.currentTimeMillis()
        val sig = computeSignatureDigest(tokenId, targetPackage, operationScope, verdict, now)
        return ClearanceToken.mintInternal(
            tokenId = tokenId,
            targetPackage = targetPackage,
            operationScope = operationScope,
            verdict = verdict,
            issuedAtEpochMs = now,
            sealSecret = ClearanceToken.GATEKEEPER_INTERNAL_SEAL,
            signatureDigest = sig,
        )
    }

    private fun computeSignatureDigest(
        tokenId: String,
        targetPackage: String,
        operationScope: OperationScope,
        verdict: GatekeeperVerdict,
        issuedAtEpochMs: Long,
    ): String {
        val raw = "$tokenId|$targetPackage|${operationScope.name}|${verdict.name}|$issuedAtEpochMs|$SIGNING_SALT"
        return MessageDigest.getInstance("SHA-256")
            .digest(raw.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }

    private fun recordAudit(
        targetPackage: String,
        operationScope: OperationScope,
        verdict: GatekeeperVerdict,
        ruleCode: String,
        reasonEn: String,
        reasonFa: String,
        tokenId: String?,
    ) {
        val now = System.currentTimeMillis()
        val record = GatekeeperAuditRecord(
            targetPackage = targetPackage,
            scope = operationScope,
            verdict = verdict,
            ruleCode = ruleCode,
            reasonEn = reasonEn,
            reasonFa = reasonFa,
            tokenId = tokenId,
            timestampMs = now,
        )
        inMemoryAudit.update { list -> (listOf(record) + list).take(100) }
        gatekeeperDao?.let { dao ->
            scope.launch {
                dao.insert(
                    GatekeeperDecisionEntity(
                        targetPackage = targetPackage,
                        operationScope = operationScope.name,
                        verdict = verdict.name,
                        policyRuleCode = ruleCode,
                        reasonEn = reasonEn,
                        reasonFa = reasonFa,
                        tokenId = tokenId,
                        timestampMs = now,
                    )
                )
            }
        }
    }

    companion object {
        private const val SIGNING_SALT = "HEXFORGE_M6_RUNTIME_VERIFY_KEY_V1"
    }
}
