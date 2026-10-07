package com.example.debtledger.data.local

import androidx.room.*

import com.example.debtledger.domain.Currency
import com.example.debtledger.domain.DebtDirection
class Converters {
    @TypeConverter fun currency(value: Currency): String = value.name
    @TypeConverter fun currency(value: String): Currency = Currency.valueOf(value)
    @TypeConverter fun direction(value: DebtDirection): String = value.name
    @TypeConverter fun direction(value: String): DebtDirection = DebtDirection.valueOf(value)
}
@Entity(tableName = "persons")
data class PersonEntity(
    @PrimaryKey val id: String, val name: String, val phone: String? = null,
    val notes: String? = null, val archivedAt: Long? = null,
    val createdAt: Long, val updatedAt: Long
)
@Entity(tableName = "debts", foreignKeys = [ForeignKey(
    entity = PersonEntity::class, parentColumns = ["id"], childColumns = ["personId"],
    onDelete = ForeignKey.RESTRICT, onUpdate = ForeignKey.RESTRICT
)], indices = [Index("personId"), Index(value = ["currency", "direction", "debtDate"])])
data class DebtEntity(
    @PrimaryKey val id: String, val personId: String, val direction: DebtDirection,
    val currency: Currency, val originalAmountMinor: Long, val debtDate: Long,
    val description: String, val notes: String? = null, val cancelledAt: Long? = null,
    val createdAt: Long, val updatedAt: Long
)
@Entity(tableName = "payments", foreignKeys = [ForeignKey(
    entity = DebtEntity::class, parentColumns = ["id"], childColumns = ["debtId"],
    onDelete = ForeignKey.RESTRICT, onUpdate = ForeignKey.RESTRICT
)], indices = [Index("debtId"), Index(value = ["operationId"], unique = true)])
data class PaymentEntity(
    @PrimaryKey val id: String, val debtId: String, val operationId: String,
    val amountMinor: Long, val paymentDate: Long, val notes: String? = null,
    val voidedAt: Long? = null, val createdAt: Long, val updatedAt: Long
)
@Entity(tableName = "audit_events", indices = [Index(value = ["entityType", "entityId"]), Index("createdAt")])
data class AuditEntity(
    @PrimaryKey val id: String, val entityType: String, val entityId: String,
    val action: String, val beforeJson: String?, val afterJson: String?,
    val reason: String?, val createdAt: Long
)
