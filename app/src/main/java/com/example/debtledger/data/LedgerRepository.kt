package com.example.debtledger.data

import androidx.room.withTransaction
import com.example.debtledger.domain.*
import com.example.debtledger.data.local.*
import org.json.JSONObject
import java.util.UUID

/** All writes must pass through this repository. DAO writes are internal infrastructure. */
class LedgerRepository(private val db: AppDatabase, private val now: () -> Long,
    private val today: () -> Long) {
    private val dao = db.ledgerDao()
    private fun id() = UUID.randomUUID().toString()
    private fun positive(amount: Long) { LedgerRules.positive(amount) }
    private fun date(day: Long) { LedgerRules.date(day,today()) }
    private fun paid(payments: List<PaymentEntity>, excluding: String? = null): Long =
        payments.filter { it.voidedAt == null && it.id != excluding }
            .fold(0L) { total, p -> Math.addExact(total, p.amountMinor) }
    private fun snapshot(value: Any): String = when(value) {
        is PersonEntity -> JSONObject().put("id",value.id).put("name",value.name)
            .put("phone",value.phone ?: JSONObject.NULL).put("notes",value.notes ?: JSONObject.NULL)
            .put("archivedAt",value.archivedAt ?: JSONObject.NULL)
            .put("createdAt",value.createdAt).put("updatedAt",value.updatedAt).toString()
        is DebtEntity -> JSONObject().put("id",value.id).put("personId",value.personId)
            .put("direction",value.direction.name).put("currency",value.currency.name)
            .put("originalAmountMinor",value.originalAmountMinor).put("debtDate",value.debtDate)
            .put("description",value.description).put("notes",value.notes ?: JSONObject.NULL)
            .put("cancelledAt",value.cancelledAt ?: JSONObject.NULL)
            .put("createdAt",value.createdAt).put("updatedAt",value.updatedAt).toString()
        is PaymentEntity -> JSONObject().put("id",value.id).put("debtId",value.debtId)
            .put("operationId",value.operationId).put("amountMinor",value.amountMinor)
            .put("paymentDate",value.paymentDate).put("notes",value.notes ?: JSONObject.NULL)
            .put("voidedAt",value.voidedAt ?: JSONObject.NULL)
            .put("createdAt",value.createdAt).put("updatedAt",value.updatedAt).toString()
        else -> error("Unsupported audit entity")
    }
    private suspend fun audit(type: String, entityId: String, action: String,
        before: Any?, after: Any?, reason: String? = null) {
        dao.insertAudit(AuditEntity(id(),type,entityId,action,before?.let(::snapshot),
            after?.let(::snapshot),reason,now()))
    }
    suspend fun savePerson(personId: String?, name: String, phone: String?, notes: String?): String = db.withTransaction {
        require(name.isNotBlank())
        val old = personId?.let { requireNotNull(dao.person(it)) }
        val value = PersonEntity(old?.id ?: id(),name.trim(),phone?.trim()?.takeIf { it.isNotEmpty() },
            notes,old?.archivedAt,old?.createdAt ?: now(),now())
        if(old == null) dao.insertPerson(value) else dao.updatePerson(value)
        audit("PERSON",value.id,if(old == null) "CREATE" else "EDIT",old,value)
        value.id
    }
    suspend fun saveDebt(debtId: String?, personId: String, direction: DebtDirection,
        currency: Currency, amount: Long, day: Long, description: String, notes: String?): String = db.withTransaction {
        positive(amount); date(day); require(description.isNotBlank())
        val person = requireNotNull(dao.person(personId))
        val old = debtId?.let { requireNotNull(dao.debt(it)) }
        require(old?.cancelledAt == null)
        val history = old?.let { dao.payments(it.id) } ?: emptyList()
        if(old == null || old.personId != personId) require(person.archivedAt == null)
        if(history.isNotEmpty()) require(old != null && old.personId == personId &&
            old.direction == direction && old.currency == currency) { "Debt identity is locked" }
        LedgerRules.debtEdit(amount, paid(history), day,
            history.filter { it.voidedAt == null }.minOfOrNull { it.paymentDate }, today())
        val value = DebtEntity(old?.id ?: id(),personId,direction,currency,amount,day,
            description.trim(),notes,null,old?.createdAt ?: now(),now())
        if(old == null) dao.insertDebt(value) else dao.updateDebt(value)
        audit("DEBT",value.id,if(old == null) "CREATE" else "EDIT",old,value)
        value.id
    }
    suspend fun addPayment(operationId: String, debtId: String, amount: Long,
        day: Long, notes: String?): String = db.withTransaction {
        require(operationId.isNotBlank())
        val previous = dao.operation(operationId)
        if(previous != null) {
            require(previous.debtId == debtId && previous.amountMinor == amount &&
                previous.paymentDate == day && previous.notes == notes && previous.voidedAt == null)
                { "Operation ID reused with changed data" }
            previous.id
        } else {
            positive(amount); date(day)
            val debt = requireNotNull(dao.debt(debtId)); require(debt.cancelledAt == null)
            LedgerRules.payment(debt.originalAmountMinor, paid(dao.payments(debtId)),
                amount, day, debt.debtDate, today())
            val value = PaymentEntity(id(),debtId,operationId,amount,day,notes,null,now(),now())
            dao.insertPayment(value); audit("PAYMENT",value.id,"CREATE",null,value); value.id
        }
    }
    suspend fun editPayment(paymentId: String, amount: Long, day: Long, notes: String?, reason: String) = db.withTransaction {
        positive(amount); date(day); require(reason.isNotBlank())
        val old = requireNotNull(dao.payment(paymentId)); require(old.voidedAt == null)
        val debt = requireNotNull(dao.debt(old.debtId)); require(debt.cancelledAt == null)
        LedgerRules.payment(debt.originalAmountMinor, paid(dao.payments(debt.id),old.id),
            amount, day, debt.debtDate, today())
        val value = old.copy(amountMinor=amount,paymentDate=day,notes=notes,updatedAt=now())
        dao.updatePayment(value); audit("PAYMENT",old.id,"EDIT",old,value,reason.trim())
    }
    suspend fun voidPayment(paymentId: String, reason: String) = db.withTransaction {
        require(reason.isNotBlank())
        val old = requireNotNull(dao.payment(paymentId)); require(old.voidedAt == null)
        val debt = requireNotNull(dao.debt(old.debtId)); require(debt.cancelledAt == null)
        val value = old.copy(voidedAt=now(),updatedAt=now())
        dao.updatePayment(value); audit("PAYMENT",old.id,"VOID",old,value,reason.trim())
    }
    suspend fun cancelDebt(debtId: String, reason: String) = db.withTransaction {
        require(reason.isNotBlank())
        val old = requireNotNull(dao.debt(debtId)); require(old.cancelledAt == null)
        val timestamp = now()
        for(payment in dao.payments(debtId).filter { it.voidedAt == null }) {
            val value = payment.copy(voidedAt=timestamp,updatedAt=timestamp)
            dao.updatePayment(value); audit("PAYMENT",payment.id,"VOID_WITH_DEBT",payment,value,reason.trim())
        }
        val value = old.copy(cancelledAt=timestamp,updatedAt=timestamp)
        dao.updateDebt(value); audit("DEBT",old.id,"CANCEL",old,value,reason.trim())
    }
    suspend fun archivePerson(personId: String) = db.withTransaction {
        val old = requireNotNull(dao.person(personId)); require(old.archivedAt == null)
        val value = old.copy(archivedAt=now(),updatedAt=now())
        dao.updatePerson(value); audit("PERSON",old.id,"ARCHIVE",old,value)
    }
    suspend fun unarchivePerson(personId: String) = db.withTransaction {
        val old = requireNotNull(dao.person(personId)); require(old.archivedAt != null)
        val value = old.copy(archivedAt=null,updatedAt=now())
        dao.updatePerson(value); audit("PERSON",old.id,"UNARCHIVE",old,value)
    }
    suspend fun deleteEmptyPerson(personId: String) = db.withTransaction {
        val old = requireNotNull(dao.person(personId)); require(dao.debtCount(personId) == 0L)
        dao.deleteEmptyPerson(personId); audit("PERSON",old.id,"DELETE",old,null)
    }
}
