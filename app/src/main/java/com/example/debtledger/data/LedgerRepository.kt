package com.example.debtledger.data

import androidx.room.withTransaction
import com.example.debtledger.data.backup.*
import com.example.debtledger.data.local.*
import com.example.debtledger.domain.*
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.security.MessageDigest
import java.util.UUID
import kotlin.coroutines.coroutineContext

enum class RestoreTestFailurePoint { AFTER_DELETE, MID_INSERT }

/** All writes must pass through this repository. DAO writes are internal infrastructure. */
class LedgerRepository(
    private val db: AppDatabase,
    private val now: () -> Long,
    private val today: () -> Long
) {
    private val dao = db.ledgerDao()
    private val writeMutex = Mutex()
    var currentGeneration: Long = 0L
        private set

    var testRestoreFailurePoint: RestoreTestFailurePoint? = null
    var testOnBeforeAcquireWriteLock: (suspend () -> Unit)? = null
    var testOnBeforeGenerationIncrementInNonCancellable: (suspend () -> Unit)? = null

    private fun id() = UUID.randomUUID().toString()
    private fun positive(amount: Long) { LedgerRules.positive(amount) }
    private fun date(day: Long) { LedgerRules.date(day, today()) }

    private fun paid(payments: List<PaymentEntity>, excluding: String? = null): Long =
        payments.filter { it.voidedAt == null && it.id != excluding }
            .fold(0L) { total, p -> Math.addExact(total, p.amountMinor) }

    private fun snapshot(value: Any): String = when(value) {
        is PersonEntity -> JSONObject().put("id", value.id).put("name", value.name)
            .put("phone", value.phone ?: JSONObject.NULL).put("notes", value.notes ?: JSONObject.NULL)
            .put("archivedAt", value.archivedAt ?: JSONObject.NULL)
            .put("createdAt", value.createdAt).put("updatedAt", value.updatedAt).toString()
        is DebtEntity -> JSONObject().put("id", value.id).put("personId", value.personId)
            .put("direction", value.direction.name).put("currency", value.currency.name)
            .put("originalAmountMinor", value.originalAmountMinor).put("debtDate", value.debtDate)
            .put("description", value.description).put("notes", value.notes ?: JSONObject.NULL)
            .put("cancelledAt", value.cancelledAt ?: JSONObject.NULL)
            .put("createdAt", value.createdAt).put("updatedAt", value.updatedAt).toString()
        is PaymentEntity -> JSONObject().put("id", value.id).put("debtId", value.debtId)
            .put("operationId", value.operationId).put("amountMinor", value.amountMinor)
            .put("paymentDate", value.paymentDate).put("notes", value.notes ?: JSONObject.NULL)
            .put("voidedAt", value.voidedAt ?: JSONObject.NULL)
            .put("createdAt", value.createdAt).put("updatedAt", value.updatedAt).toString()
        else -> error("Unsupported audit entity")
    }

    private suspend fun audit(
        type: String, entityId: String, action: String,
        before: Any?, after: Any?, reason: String? = null
    ) {
        dao.insertAudit(
            AuditEntity(
                id(), type, entityId, action,
                before?.let(::snapshot), after?.let(::snapshot),
                reason, now()
            )
        )
    }

    suspend fun getDatabaseFingerprint(): String = db.withTransaction {
        val persons = dao.getAllPersons().sortedBy { it.id }
        val debts = dao.getAllDebts().sortedBy { it.id }
        val payments = dao.getAllPayments().sortedBy { it.id }
        val audits = dao.getAllAuditEvents().sortedBy { it.id }

        val sb = StringBuilder()
        persons.forEach { sb.append("P|${it.id}|${it.name}|${it.phone}|${it.notes}|${it.archivedAt}|${it.createdAt}|${it.updatedAt}\n") }
        debts.forEach { sb.append("D|${it.id}|${it.personId}|${it.direction.name}|${it.currency.name}|${it.originalAmountMinor}|${it.debtDate}|${it.description}|${it.notes}|${it.cancelledAt}|${it.createdAt}|${it.updatedAt}\n") }
        payments.forEach { sb.append("PAY|${it.id}|${it.debtId}|${it.operationId}|${it.amountMinor}|${it.paymentDate}|${it.notes}|${it.voidedAt}|${it.createdAt}|${it.updatedAt}\n") }
        audits.forEach { sb.append("A|${it.id}|${it.entityType}|${it.entityId}|${it.action}|${it.beforeJson}|${it.afterJson}|${it.reason}|${it.createdAt}\n") }

        val bytes = MessageDigest.getInstance("SHA-256").digest(sb.toString().toByteArray(Charsets.UTF_8))
        bytes.joinToString("") { "%02x".format(it) }
    }

    suspend fun exportBackupPayload(): PlainBackupPayloadV1 = db.withTransaction {
        val persons = dao.getAllPersons().map { it.toDto() }
        val debts = dao.getAllDebts().map { it.toDto() }
        val payments = dao.getAllPayments().map { it.toDto() }
        val audits = dao.getAllAuditEvents().map { it.toDto() }

        val timestamp = now()
        val metadata = BackupMetadataDto(
            formatIdentifier = "DebtLedgerBackup",
            formatVersion = 1,
            databaseVersion = 1,
            appVersion = "0.1.0",
            exportTimestamp = timestamp,
            counts = BackupCountsDto(
                persons = persons.size,
                debts = debts.size,
                payments = payments.size,
                auditEvents = audits.size
            )
        )
        val data = BackupDataDto(
            persons = persons,
            debts = debts,
            payments = payments,
            auditEvents = audits
        )
        PlainBackupPayloadV1(metadata = metadata, data = data)
    }

    suspend fun restoreBackupPayload(payload: PlainBackupPayloadV1, expectedFingerprint: String) {
        val startGen = currentGeneration
        writeMutex.withLock {
            try {
                coroutineContext.ensureActive()

                if (startGen != currentGeneration) {
                    throw RuleViolation(RuleError.STALE_WRITE)
                }

                val currentFingerprint = getDatabaseFingerprint()
                if (currentFingerprint != expectedFingerprint) {
                    throw RuleViolation(RuleError.STALE_WRITE)
                }

                // Verify validation rules before touching database
                BackupValidation.validatePayload(payload)

                val personEntities = payload.data.persons.map { it.toEntity() }
                val debtEntities = payload.data.debts.map { it.toEntity() }
                val paymentEntities = payload.data.payments.map { it.toEntity() }
                val auditEntities = payload.data.auditEvents.map { it.toEntity() }

                // Atomic replacement and generation increment in a single NonCancellable block
                withContext(NonCancellable) {
                    db.withTransaction {
                        dao.deleteAllAuditEvents()
                        dao.deleteAllPayments()
                        dao.deleteAllDebts()
                        dao.deleteAllPersons()

                        if (testRestoreFailurePoint == RestoreTestFailurePoint.AFTER_DELETE) {
                            throw IllegalStateException("TEST_INJECTED_FAILURE_AFTER_DELETE")
                        }

                        dao.insertPersons(personEntities)

                        if (testRestoreFailurePoint == RestoreTestFailurePoint.MID_INSERT) {
                            throw IllegalStateException("TEST_INJECTED_FAILURE_MID_INSERT")
                        }

                        dao.insertDebts(debtEntities)
                        dao.insertPayments(paymentEntities)
                        dao.insertAuditEvents(auditEntities)
                    }

                    testOnBeforeGenerationIncrementInNonCancellable?.invoke()

                    // Increment generation immediately after successful transaction commit
                    currentGeneration++
                }
            } finally {
                // Mutex is always released in finally
            }
        }
    }

    suspend fun savePerson(
        personId: String?, name: String, phone: String?, notes: String?,
        formGeneration: Long? = null
    ): String {
        val startGen = currentGeneration
        if (formGeneration != null && formGeneration != currentGeneration) {
            throw RuleViolation(RuleError.STALE_WRITE)
        }
        testOnBeforeAcquireWriteLock?.invoke()
        return writeMutex.withLock {
            if (startGen != currentGeneration || (formGeneration != null && formGeneration != currentGeneration)) {
                throw RuleViolation(RuleError.STALE_WRITE)
            }
            db.withTransaction {
                require(name.isNotBlank())
                val old = personId?.let { requireNotNull(dao.person(it)) }
                val value = PersonEntity(
                    old?.id ?: id(), name.trim(), phone?.trim()?.takeIf { it.isNotEmpty() },
                    notes, old?.archivedAt, old?.createdAt ?: now(), now()
                )
                if (old == null) dao.insertPerson(value) else dao.updatePerson(value)
                audit("PERSON", value.id, if (old == null) "CREATE" else "EDIT", old, value)
                value.id
            }
        }
    }

    suspend fun saveDebt(
        debtId: String?, personId: String, direction: DebtDirection,
        currency: Currency, amount: Long, day: Long, description: String, notes: String?,
        formGeneration: Long? = null
    ): String {
        val startGen = currentGeneration
        if (formGeneration != null && formGeneration != currentGeneration) {
            throw RuleViolation(RuleError.STALE_WRITE)
        }
        testOnBeforeAcquireWriteLock?.invoke()
        return writeMutex.withLock {
            if (startGen != currentGeneration || (formGeneration != null && formGeneration != currentGeneration)) {
                throw RuleViolation(RuleError.STALE_WRITE)
            }
            db.withTransaction {
                positive(amount); date(day); require(description.isNotBlank())
                val person = requireNotNull(dao.person(personId))
                val old = debtId?.let { requireNotNull(dao.debt(it)) }
                require(old?.cancelledAt == null)
                val history = old?.let { dao.payments(it.id) } ?: emptyList()
                if (old == null || old.personId != personId) require(person.archivedAt == null)
                if (history.isNotEmpty()) require(
                    old != null && old.personId == personId &&
                            old.direction == direction && old.currency == currency
                ) { "Debt identity is locked" }
                LedgerRules.debtEdit(
                    amount, paid(history), day,
                    history.filter { it.voidedAt == null }.minOfOrNull { it.paymentDate }, today()
                )
                val value = DebtEntity(
                    old?.id ?: id(), personId, direction, currency, amount, day,
                    description.trim(), notes, null, old?.createdAt ?: now(), now()
                )
                if (old == null) dao.insertDebt(value) else dao.updateDebt(value)
                audit("DEBT", value.id, if (old == null) "CREATE" else "EDIT", old, value)
                value.id
            }
        }
    }

    suspend fun addPayment(
        operationId: String, debtId: String, amount: Long,
        day: Long, notes: String?, formGeneration: Long? = null
    ): String {
        val startGen = currentGeneration
        if (formGeneration != null && formGeneration != currentGeneration) {
            throw RuleViolation(RuleError.STALE_WRITE)
        }
        testOnBeforeAcquireWriteLock?.invoke()
        return writeMutex.withLock {
            if (startGen != currentGeneration || (formGeneration != null && formGeneration != currentGeneration)) {
                throw RuleViolation(RuleError.STALE_WRITE)
            }
            db.withTransaction {
                require(operationId.isNotBlank())
                val previous = dao.operation(operationId)
                if (previous != null) {
                    require(
                        previous.debtId == debtId && previous.amountMinor == amount &&
                                previous.paymentDate == day && previous.notes == notes && previous.voidedAt == null
                    ) { "Operation ID reused with changed data" }
                    previous.id
                } else {
                    positive(amount); date(day)
                    val debt = requireNotNull(dao.debt(debtId)); require(debt.cancelledAt == null)
                    LedgerRules.payment(
                        debt.originalAmountMinor, paid(dao.payments(debtId)),
                        amount, day, debt.debtDate, today()
                    )
                    val value = PaymentEntity(id(), debtId, operationId, amount, day, notes, null, now(), now())
                    dao.insertPayment(value); audit("PAYMENT", value.id, "CREATE", null, value); value.id
                }
            }
        }
    }

    suspend fun editPayment(
        paymentId: String, amount: Long, day: Long, notes: String?, reason: String,
        formGeneration: Long? = null
    ) {
        val startGen = currentGeneration
        if (formGeneration != null && formGeneration != currentGeneration) {
            throw RuleViolation(RuleError.STALE_WRITE)
        }
        testOnBeforeAcquireWriteLock?.invoke()
        writeMutex.withLock {
            if (startGen != currentGeneration || (formGeneration != null && formGeneration != currentGeneration)) {
                throw RuleViolation(RuleError.STALE_WRITE)
            }
            db.withTransaction {
                positive(amount); date(day); require(reason.isNotBlank())
                val old = requireNotNull(dao.payment(paymentId)); require(old.voidedAt == null)
                val debt = requireNotNull(dao.debt(old.debtId)); require(debt.cancelledAt == null)
                LedgerRules.payment(
                    debt.originalAmountMinor, paid(dao.payments(debt.id), old.id),
                    amount, day, debt.debtDate, today()
                )
                val value = old.copy(amountMinor = amount, paymentDate = day, notes = notes, updatedAt = now())
                dao.updatePayment(value); audit("PAYMENT", old.id, "EDIT", old, value, reason.trim())
            }
        }
    }

    suspend fun voidPayment(paymentId: String, reason: String, formGeneration: Long? = null) {
        val startGen = currentGeneration
        if (formGeneration != null && formGeneration != currentGeneration) {
            throw RuleViolation(RuleError.STALE_WRITE)
        }
        testOnBeforeAcquireWriteLock?.invoke()
        writeMutex.withLock {
            if (startGen != currentGeneration || (formGeneration != null && formGeneration != currentGeneration)) {
                throw RuleViolation(RuleError.STALE_WRITE)
            }
            db.withTransaction {
                require(reason.isNotBlank())
                val old = requireNotNull(dao.payment(paymentId)); require(old.voidedAt == null)
                val debt = requireNotNull(dao.debt(old.debtId)); require(debt.cancelledAt == null)
                val value = old.copy(voidedAt = now(), updatedAt = now())
                dao.updatePayment(value); audit("PAYMENT", old.id, "VOID", old, value, reason.trim())
            }
        }
    }

    suspend fun cancelDebt(debtId: String, reason: String, formGeneration: Long? = null) {
        val startGen = currentGeneration
        if (formGeneration != null && formGeneration != currentGeneration) {
            throw RuleViolation(RuleError.STALE_WRITE)
        }
        testOnBeforeAcquireWriteLock?.invoke()
        writeMutex.withLock {
            if (startGen != currentGeneration || (formGeneration != null && formGeneration != currentGeneration)) {
                throw RuleViolation(RuleError.STALE_WRITE)
            }
            db.withTransaction {
                require(reason.isNotBlank())
                val old = requireNotNull(dao.debt(debtId)); require(old.cancelledAt == null)
                val timestamp = now()
                for (payment in dao.payments(debtId).filter { it.voidedAt == null }) {
                    val value = payment.copy(voidedAt = timestamp, updatedAt = timestamp)
                    dao.updatePayment(value); audit("PAYMENT", payment.id, "VOID_WITH_DEBT", payment, value, reason.trim())
                }
                val value = old.copy(cancelledAt = timestamp, updatedAt = timestamp)
                dao.updateDebt(value); audit("DEBT", old.id, "CANCEL", old, value, reason.trim())
            }
        }
    }

    suspend fun archivePerson(personId: String, formGeneration: Long? = null) {
        val startGen = currentGeneration
        if (formGeneration != null && formGeneration != currentGeneration) {
            throw RuleViolation(RuleError.STALE_WRITE)
        }
        testOnBeforeAcquireWriteLock?.invoke()
        writeMutex.withLock {
            if (startGen != currentGeneration || (formGeneration != null && formGeneration != currentGeneration)) {
                throw RuleViolation(RuleError.STALE_WRITE)
            }
            db.withTransaction {
                val old = requireNotNull(dao.person(personId)); require(old.archivedAt == null)
                val value = old.copy(archivedAt = now(), updatedAt = now())
                dao.updatePerson(value); audit("PERSON", old.id, "ARCHIVE", old, value)
            }
        }
    }

    suspend fun unarchivePerson(personId: String, formGeneration: Long? = null) {
        val startGen = currentGeneration
        if (formGeneration != null && formGeneration != currentGeneration) {
            throw RuleViolation(RuleError.STALE_WRITE)
        }
        testOnBeforeAcquireWriteLock?.invoke()
        writeMutex.withLock {
            if (startGen != currentGeneration || (formGeneration != null && formGeneration != currentGeneration)) {
                throw RuleViolation(RuleError.STALE_WRITE)
            }
            db.withTransaction {
                val old = requireNotNull(dao.person(personId)); require(old.archivedAt != null)
                val value = old.copy(archivedAt = null, updatedAt = now())
                dao.updatePerson(value); audit("PERSON", old.id, "UNARCHIVE", old, value)
            }
        }
    }

    suspend fun deleteEmptyPerson(personId: String, formGeneration: Long? = null) {
        val startGen = currentGeneration
        if (formGeneration != null && formGeneration != currentGeneration) {
            throw RuleViolation(RuleError.STALE_WRITE)
        }
        testOnBeforeAcquireWriteLock?.invoke()
        writeMutex.withLock {
            if (startGen != currentGeneration || (formGeneration != null && formGeneration != currentGeneration)) {
                throw RuleViolation(RuleError.STALE_WRITE)
            }
            db.withTransaction {
                val old = requireNotNull(dao.person(personId)); require(dao.debtCount(personId) == 0L)
                dao.deleteEmptyPerson(personId); audit("PERSON", old.id, "DELETE", old, null)
            }
        }
    }
}
