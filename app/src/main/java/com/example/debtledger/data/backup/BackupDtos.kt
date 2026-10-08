package com.example.debtledger.data.backup

import com.example.debtledger.data.local.AuditEntity
import com.example.debtledger.data.local.DebtEntity
import com.example.debtledger.data.local.PaymentEntity
import com.example.debtledger.data.local.PersonEntity
import com.example.debtledger.domain.Currency
import com.example.debtledger.domain.DebtDirection
import kotlinx.serialization.Serializable

@Serializable
data class BackupHeaderV1(
    val formatIdentifier: String,
    val formatVersion: Int,
    val databaseVersion: Int,
    val cipherAlgorithm: String,
    val kdf: String,
    val kdfIterations: Int,
    val salt: String,
    val nonce: String,
    val exportTimestamp: Long
)

@Serializable
data class BackupEnvelopeV1(
    val header: BackupHeaderV1,
    val ciphertext: String
)

@Serializable
data class PersonDto(
    val id: String,
    val name: String,
    val phone: String? = null,
    val notes: String? = null,
    val archivedAt: Long? = null,
    val createdAt: Long,
    val updatedAt: Long
)

@Serializable
data class DebtDto(
    val id: String,
    val personId: String,
    val direction: String,
    val currency: String,
    val originalAmountMinor: Long,
    val debtDate: Long,
    val description: String,
    val notes: String? = null,
    val cancelledAt: Long? = null,
    val createdAt: Long,
    val updatedAt: Long
)

@Serializable
data class PaymentDto(
    val id: String,
    val debtId: String,
    val operationId: String,
    val amountMinor: Long,
    val paymentDate: Long,
    val notes: String? = null,
    val voidedAt: Long? = null,
    val createdAt: Long,
    val updatedAt: Long
)

@Serializable
data class AuditDto(
    val id: String,
    val entityType: String,
    val entityId: String,
    val action: String,
    val beforeJson: String? = null,
    val afterJson: String? = null,
    val reason: String? = null,
    val createdAt: Long
)

@Serializable
data class BackupMetadataDto(
    val formatIdentifier: String,
    val formatVersion: Int,
    val databaseVersion: Int,
    val appVersion: String,
    val exportTimestamp: Long,
    val counts: BackupCountsDto
)

@Serializable
data class BackupCountsDto(
    val persons: Int,
    val debts: Int,
    val payments: Int,
    val auditEvents: Int
)

@Serializable
data class BackupDataDto(
    val persons: List<PersonDto>,
    val debts: List<DebtDto>,
    val payments: List<PaymentDto>,
    val auditEvents: List<AuditDto>
)

@Serializable
data class PlainBackupPayloadV1(
    val metadata: BackupMetadataDto,
    val data: BackupDataDto
)

fun PersonEntity.toDto() = PersonDto(id, name, phone, notes, archivedAt, createdAt, updatedAt)
fun PersonDto.toEntity() = PersonEntity(id, name, phone, notes, archivedAt, createdAt, updatedAt)

fun DebtEntity.toDto() = DebtDto(id, personId, direction.name, currency.name, originalAmountMinor, debtDate, description, notes, cancelledAt, createdAt, updatedAt)
fun DebtDto.toEntity() = DebtEntity(id, personId, DebtDirection.valueOf(direction), Currency.valueOf(currency), originalAmountMinor, debtDate, description, notes, cancelledAt, createdAt, updatedAt)

fun PaymentEntity.toDto() = PaymentDto(id, debtId, operationId, amountMinor, paymentDate, notes, voidedAt, createdAt, updatedAt)
fun PaymentDto.toEntity() = PaymentEntity(id, debtId, operationId, amountMinor, paymentDate, notes, voidedAt, createdAt, updatedAt)

fun AuditEntity.toDto() = AuditDto(id, entityType, entityId, action, beforeJson, afterJson, reason, createdAt)
fun AuditDto.toEntity() = AuditEntity(id, entityType, entityId, action, beforeJson, afterJson, reason, createdAt)
