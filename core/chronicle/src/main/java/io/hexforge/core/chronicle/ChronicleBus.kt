package io.hexforge.core.chronicle

import io.hexforge.core.common.ExecutionTier
import io.hexforge.core.common.ModuleId
import io.hexforge.core.database.ChronicleDao
import io.hexforge.core.database.ChronicleEventEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Universal bilingual action reporter (Part 16 M9, PD-6).
 * Writes to Room when ChronicleDao is attached and mirrors into a hot StateFlow for live HUD/sheet telemetry.
 */
class ChronicleBus(
    private val chronicleDao: ChronicleDao? = null,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) {
    private val inMemoryEvents = MutableStateFlow<List<ChronicleEvent>>(emptyList())

    val liveEvents: Flow<List<ChronicleEvent>> =
        chronicleDao?.observeRecent(400)?.map { entities ->
            entities.map { it.toDomain() }
        } ?: inMemoryEvents.asStateFlow()

    val latestTail: StateFlow<List<ChronicleEvent>> = inMemoryEvents.asStateFlow()

    fun emit(
        messageKey: String,
        tier: ExecutionTier,
        targetId: String,
        params: Map<String, String> = emptyMap(),
        moduleOverride: ModuleId? = null,
        phaseOverride: ChroniclePhase? = null,
        severityOverride: ChronicleSeverity? = null,
        payloadJson: String = "{}",
        reversibleRefId: String? = null,
    ): ChronicleEvent {
        val template = ChronicleMessageCatalog.getOrNull(messageKey)
        val module = moduleOverride ?: template?.module ?: ModuleId.M9_CHRONICLE
        val phase = phaseOverride ?: template?.defaultPhase ?: ChroniclePhase.EXECUTE
        val severity = severityOverride ?: template?.defaultSeverity ?: ChronicleSeverity.INFO
        val (en, fa) = ChronicleMessageCatalog.format(messageKey, params)

        val event = ChronicleEvent(
            uuid = UUID.randomUUID().toString(),
            timestampMs = System.currentTimeMillis(),
            module = module,
            tier = tier,
            severity = severity,
            phase = phase,
            targetId = targetId,
            messageKey = messageKey,
            messageEn = en,
            messageFa = fa,
            payloadJson = payloadJson,
            reversibleRefId = reversibleRefId,
        )

        inMemoryEvents.update { current ->
            (listOf(event) + current).take(250)
        }

        chronicleDao?.let { dao ->
            scope.launch {
                dao.insert(event.toEntity())
            }
        }
        return event
    }

    suspend fun emitBlocking(
        messageKey: String,
        tier: ExecutionTier,
        targetId: String,
        params: Map<String, String> = emptyMap(),
        payloadJson: String = "{}",
        reversibleRefId: String? = null,
    ): ChronicleEvent {
        val event = emit(
            messageKey = messageKey,
            tier = tier,
            targetId = targetId,
            params = params,
            payloadJson = payloadJson,
            reversibleRefId = reversibleRefId,
        )
        return event
    }

    private fun ChronicleEvent.toEntity(): ChronicleEventEntity = ChronicleEventEntity(
        eventUuid = uuid,
        timestampMs = timestampMs,
        moduleCode = module.code,
        tierCode = tier.code,
        severity = severity.name,
        phase = phase.name,
        targetId = targetId,
        messageKey = messageKey,
        messageEn = messageEn,
        messageFa = messageFa,
        payloadJson = payloadJson,
        reversibleRefId = reversibleRefId,
    )

    private fun ChronicleEventEntity.toDomain(): ChronicleEvent {
        val mod = ModuleId.entries.firstOrNull { it.code == moduleCode } ?: ModuleId.M9_CHRONICLE
        val tier = ExecutionTier.entries.firstOrNull { it.code == tierCode } ?: ExecutionTier.T0_HEXSPACE
        val sev = ChronicleSeverity.entries.firstOrNull { it.name == severity } ?: ChronicleSeverity.INFO
        val ph = ChroniclePhase.entries.firstOrNull { it.name == phase } ?: ChroniclePhase.EXECUTE
        return ChronicleEvent(
            uuid = eventUuid,
            timestampMs = timestampMs,
            module = mod,
            tier = tier,
            severity = sev,
            phase = ph,
            targetId = targetId,
            messageKey = messageKey,
            messageEn = messageEn,
            messageFa = messageFa,
            payloadJson = payloadJson,
            reversibleRefId = reversibleRefId,
        )
    }
}
