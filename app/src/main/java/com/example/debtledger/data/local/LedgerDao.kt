package com.example.debtledger.data.local

import androidx.room.*
import com.example.debtledger.domain.Currency
import com.example.debtledger.domain.DebtDirection
import kotlinx.coroutines.flow.Flow

@DatabaseView(viewName = "debt_balances", value = """
SELECT d.*, COALESCE(p.paidMinor, 0) AS paidMinor,
       d.originalAmountMinor - COALESCE(p.paidMinor, 0) AS remainingMinor
FROM debts d LEFT JOIN (
    SELECT debtId, SUM(amountMinor) AS paidMinor
    FROM payments WHERE voidedAt IS NULL GROUP BY debtId
) p ON p.debtId = d.id
""")
data class DebtBalance(
    val id: String, val personId: String, val direction: DebtDirection,
    val currency: Currency, val originalAmountMinor: Long, val debtDate: Long,
    val description: String, val notes: String?, val cancelledAt: Long?,
    val createdAt: Long, val updatedAt: Long, val paidMinor: Long, val remainingMinor: Long
) {
    val status: String get() = when {
        cancelledAt != null -> "CANCELLED"
        paidMinor == 0L -> "UNPAID"
        remainingMinor == 0L -> "PAID"
        else -> "PARTIAL"
    }
}
data class CurrencyTotals(val currency: Currency, val receivableMinor: Long,
    val payableMinor: Long, val collectedMinor: Long, val repaidMinor: Long,
    val unpaidCount: Long, val partialCount: Long, val paidCount: Long)

@Dao
interface LedgerDao {
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertPerson(value: PersonEntity)
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertDebt(value: DebtEntity)
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertPayment(value: PaymentEntity)
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertAudit(value: AuditEntity)
    @Update suspend fun updatePerson(value: PersonEntity)
    @Update suspend fun updateDebt(value: DebtEntity)
    @Update suspend fun updatePayment(value: PaymentEntity)
    @Query("SELECT * FROM persons WHERE id=:id") suspend fun person(id: String): PersonEntity?
    @Query("SELECT * FROM debts WHERE id=:id") suspend fun debt(id: String): DebtEntity?
    @Query("SELECT * FROM payments WHERE id=:id") suspend fun payment(id: String): PaymentEntity?
    @Query("SELECT * FROM payments WHERE operationId=:id") suspend fun operation(id: String): PaymentEntity?
    @Query("SELECT * FROM payments WHERE debtId=:id ORDER BY paymentDate, createdAt")
    suspend fun payments(id: String): List<PaymentEntity>
    @Query("SELECT * FROM debt_balances ORDER BY debtDate DESC, createdAt DESC")
    fun observeAllDebts(): Flow<List<DebtBalance>>
    @Query("SELECT * FROM persons ORDER BY name") fun observePersons(): Flow<List<PersonEntity>>
    @Query("SELECT * FROM debt_balances WHERE id=:id") fun observeDebt(id: String): Flow<DebtBalance?>
    @Query("""SELECT * FROM debt_balances WHERE
        (:personId IS NULL OR personId=:personId) AND
        (:currency IS NULL OR currency=:currency) AND
        (:direction IS NULL OR direction=:direction) AND
        (:fromDay IS NULL OR debtDate>=:fromDay) AND
        (:toDay IS NULL OR debtDate<=:toDay) AND
        ((:state='CANCELLED' AND cancelledAt IS NOT NULL) OR
         (cancelledAt IS NULL AND (:state IS NULL OR
         (:state='UNPAID' AND paidMinor=0) OR
         (:state='PARTIAL' AND paidMinor>0 AND remainingMinor>0) OR
         (:state='PAID' AND remainingMinor=0) OR
         (:state='OPEN' AND remainingMinor>0)))) ORDER BY debtDate DESC, createdAt DESC""")
    fun observeDebts(personId: String?, currency: Currency?, direction: DebtDirection?,
        state: String?, fromDay: Long?, toDay: Long?): Flow<List<DebtBalance>>
    @Query("""SELECT currency,
        SUM(CASE WHEN direction='RECEIVABLE' THEN remainingMinor ELSE 0 END) AS receivableMinor,
        SUM(CASE WHEN direction='PAYABLE' THEN remainingMinor ELSE 0 END) AS payableMinor,
        SUM(CASE WHEN direction='RECEIVABLE' THEN paidMinor ELSE 0 END) AS collectedMinor,
        SUM(CASE WHEN direction='PAYABLE' THEN paidMinor ELSE 0 END) AS repaidMinor,
        SUM(CASE WHEN paidMinor=0 THEN 1 ELSE 0 END) AS unpaidCount,
        SUM(CASE WHEN paidMinor>0 AND remainingMinor>0 THEN 1 ELSE 0 END) AS partialCount,
        SUM(CASE WHEN remainingMinor=0 THEN 1 ELSE 0 END) AS paidCount
        FROM debt_balances WHERE cancelledAt IS NULL AND
        (:personId IS NULL OR personId=:personId) GROUP BY currency""")
    fun observeTotals(personId: String?): Flow<List<CurrencyTotals>>
    @Query("SELECT * FROM payments WHERE debtId=:id ORDER BY paymentDate DESC, createdAt DESC")
    fun observePayments(id: String): Flow<List<PaymentEntity>>
    @Query("SELECT * FROM audit_events ORDER BY createdAt DESC, id DESC LIMIT :limit")
    fun observeActivity(limit: Int = 100): Flow<List<AuditEntity>>
    @Query("SELECT COUNT(*) FROM debts WHERE personId=:id") suspend fun debtCount(id: String): Long
    @Query("DELETE FROM persons WHERE id=:id") suspend fun deleteEmptyPerson(id: String)
}
