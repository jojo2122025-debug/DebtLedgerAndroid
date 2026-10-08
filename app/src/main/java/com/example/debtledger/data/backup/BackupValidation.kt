package com.example.debtledger.data.backup

import com.example.debtledger.domain.Currency
import com.example.debtledger.domain.DebtDirection
import java.time.LocalDate

object BackupValidation {

    fun validateHeader(header: BackupHeaderV1) {
        if (header.formatIdentifier != "DebtLedgerBackup") {
            throw IllegalArgumentException("الملف غير صادر من تطبيق دفتر الديون")
        }
        if (header.formatVersion != 1 || header.databaseVersion != 1) {
            throw IllegalArgumentException("إصدار التنسيق أو المخطط غير مدعوم (formatVersion=${header.formatVersion}, dbVersion=${header.databaseVersion})")
        }
        if (header.cipherAlgorithm != "AES-256-GCM") {
            throw IllegalArgumentException("خوارزمية التشفير غير مدعومة (${header.cipherAlgorithm})")
        }
        if (header.kdf != "PBKDF2WithHmacSHA256") {
            throw IllegalArgumentException("دالة اشتقاق المفتاح غير مدعومة (${header.kdf})")
        }
        if (header.kdfIterations != 100000) {
            throw IllegalArgumentException("عدد دورات اشتقاق المفتاح غير مطابق للعلامة المعتمدة (100000)")
        }
    }

    fun validateMetadata(metadata: BackupMetadataDto, header: BackupHeaderV1? = null) {
        if (metadata.formatIdentifier != "DebtLedgerBackup") {
            throw IllegalArgumentException("هوية الميتاداتا غير مطابقة لتطبيق دفتر الديون")
        }
        if (metadata.formatVersion != 1 || metadata.databaseVersion != 1) {
            throw IllegalArgumentException("إصدار الميتاداتا غير مدعوم (formatVersion=${metadata.formatVersion}, dbVersion=${metadata.databaseVersion})")
        }
        if (header != null) {
            if (metadata.formatIdentifier != header.formatIdentifier ||
                metadata.formatVersion != header.formatVersion ||
                metadata.databaseVersion != header.databaseVersion ||
                metadata.exportTimestamp != header.exportTimestamp
            ) {
                throw IllegalArgumentException("بيانات الميتاداتا الداخلية لا تتطابق مع الترويسة الخارجية")
            }
        }
    }

    fun validatePayload(payload: PlainBackupPayloadV1, header: BackupHeaderV1? = null) {
        validateMetadata(payload.metadata, header)

        val counts = payload.metadata.counts
        val data = payload.data

        if (counts.persons != data.persons.size ||
            counts.debts != data.debts.size ||
            counts.payments != data.payments.size ||
            counts.auditEvents != data.auditEvents.size
        ) {
            throw IllegalArgumentException("أعداد السجلات المذكورة في الميتاداتا لا تطابق الأعداد الفعلية في البيانات")
        }

        validateData(data)
    }

    fun validateData(data: BackupDataDto) {
        val totalCount = data.persons.size + data.debts.size + data.payments.size + data.auditEvents.size
        if (totalCount > BackupCrypto.MAX_RECORDS_LIMIT) {
            throw IllegalArgumentException("عدد السجلات الكلي ($totalCount) يتجاوز الحد الأقصى المسموح (${BackupCrypto.MAX_RECORDS_LIMIT})")
        }

        // 1. Validate Person Entities
        val personIds = HashSet<String>()
        for (p in data.persons) {
            if (p.id.isBlank()) throw IllegalArgumentException("توجد هوية شخص فارغة")
            if (!personIds.add(p.id)) throw IllegalArgumentException("توجد هوية شخص مكررة: ${p.id}")
            if (p.name.isBlank()) throw IllegalArgumentException("اسم الشخص لا يمكن أن يكون فارغاً (id=${p.id})")
            if (p.name.length > 200) throw IllegalArgumentException("طول اسم الشخص يتجاوز 200 حرف (id=${p.id})")
            if (p.phone != null && p.phone.length > 30) throw IllegalArgumentException("طول رقم الهاتف يتجاوز 30 حرفاً (id=${p.id})")
            if (p.notes != null && p.notes.length > 2000) throw IllegalArgumentException("طول ملاحظات الشخص يتجاوز 2000 حرف (id=${p.id})")
        }

        // 2. Validate Debt Entities
        val debtIds = HashSet<String>()
        val currencyReceivableSum = HashMap<String, Long>()
        val currencyPayableSum = HashMap<String, Long>()

        for (d in data.debts) {
            if (d.id.isBlank()) throw IllegalArgumentException("توجد هوية دين فارغة")
            if (!debtIds.add(d.id)) throw IllegalArgumentException("توجد هوية دين مكررة: ${d.id}")
            if (!personIds.contains(d.personId)) throw IllegalArgumentException("الدين ${d.id} يشير إلى شخص غير موجود (personId=${d.personId})")

            val currency = runCatching { Currency.valueOf(d.currency) }.getOrNull()
                ?: throw IllegalArgumentException("عملة غير مدعومة (${d.currency}) في الدين ${d.id}")

            val direction = runCatching { DebtDirection.valueOf(d.direction) }.getOrNull()
                ?: throw IllegalArgumentException("اتجاه غير مدعوم (${d.direction}) في الدين ${d.id}")

            if (d.originalAmountMinor <= 0L) throw IllegalArgumentException("مبلغ الدين الأصلي يجب أن يكون أكبر من صفر (id=${d.id})")
            if (d.description.isBlank()) throw IllegalArgumentException("وصف الدين لا يمكن أن يكون فارغاً (id=${d.id})")
            if (d.description.length > 500) throw IllegalArgumentException("وصف الدين يتجاوز 500 حرف (id=${d.id})")
            if (d.notes != null && d.notes.length > 2000) throw IllegalArgumentException("ملاحظات الدين تتجاوز 2000 حرف (id=${d.id})")

            // Validate debt date
            runCatching { LocalDate.ofEpochDay(d.debtDate) }.getOrElse {
                throw IllegalArgumentException("تاريخ الدين غير صالح (debtDate=${d.debtDate}) في الدين ${d.id}")
            }

            // Overflow check for totals
            val key = currency.name
            val currentSum = if (direction == DebtDirection.RECEIVABLE) currencyReceivableSum.getOrDefault(key, 0L) else currencyPayableSum.getOrDefault(key, 0L)
            val newSum = try {
                Math.addExact(currentSum, d.originalAmountMinor)
            } catch (e: ArithmeticException) {
                throw IllegalArgumentException("إجمالي المبالغ للعملة $key يتجاوز الحد الأقصى المسموح (Long overflow)")
            }
            if (direction == DebtDirection.RECEIVABLE) currencyReceivableSum[key] = newSum else currencyPayableSum[key] = newSum
        }

        // 3. Validate Payments & Overpayment check
        val paymentIds = HashSet<String>()
        val operationIds = HashSet<String>()
        val debtActivePaidSum = HashMap<String, Long>()

        for (p in data.payments) {
            if (p.id.isBlank()) throw IllegalArgumentException("توجد هوية دفعة فارغة")
            if (!paymentIds.add(p.id)) throw IllegalArgumentException("توجد هوية دفعة مكررة: ${p.id}")
            if (p.operationId.isBlank()) throw IllegalArgumentException("توجد عملية دفعة بدون operationId")
            if (!operationIds.add(p.operationId)) throw IllegalArgumentException("معرف العملية operationId مكرر: ${p.operationId}")

            val debt = data.debts.find { it.id == p.debtId }
                ?: throw IllegalArgumentException("الدفعة ${p.id} تشير إلى دين غير موجود (debtId=${p.debtId})")

            if (p.amountMinor <= 0L) throw IllegalArgumentException("مبلغ الدفعة يجب أن يكون أكبر من صفر (id=${p.id})")
            if (p.notes != null && p.notes.length > 2000) throw IllegalArgumentException("ملاحظات الدفعة تتجاوز 2000 حرف (id=${p.id})")

            runCatching { LocalDate.ofEpochDay(p.paymentDate) }.getOrElse {
                throw IllegalArgumentException("تاريخ الدفعة غير صالح (paymentDate=${p.paymentDate}) في الدفعة ${p.id}")
            }

            if (p.voidedAt == null) {
                val currentPaid = debtActivePaidSum.getOrDefault(p.debtId, 0L)
                val newPaid = try {
                    Math.addExact(currentPaid, p.amountMinor)
                } catch (e: ArithmeticException) {
                    throw IllegalArgumentException("مجموع الدفعات للدين ${p.debtId} يتجاوز الحد الأقصى المسموح")
                }
                if (newPaid > debt.originalAmountMinor) {
                    throw IllegalArgumentException("مجموع الدفعات الفعالة (${newPaid}) يتجاوز مبلغ الدين الأصلي (${debt.originalAmountMinor}) للدين ${p.debtId}")
                }
                debtActivePaidSum[p.debtId] = newPaid
            }
        }

        // 4. Validate Audit Events
        val auditIds = HashSet<String>()
        for (a in data.auditEvents) {
            if (a.id.isBlank()) throw IllegalArgumentException("توجد هوية سجل تدقيق فارغة")
            if (!auditIds.add(a.id)) throw IllegalArgumentException("توجد هوية سجل تدقيق مكررة: ${a.id}")
            if (a.reason != null && a.reason.length > 2000) throw IllegalArgumentException("سبب التدقيق يتجاوز 2000 حرف (id=${a.id})")
            if (a.beforeJson != null && a.beforeJson.length > 5000) throw IllegalArgumentException("طول beforeJson يتجاوز 5000 حرف (id=${a.id})")
            if (a.afterJson != null && a.afterJson.length > 5000) throw IllegalArgumentException("طول afterJson يتجاوز 5000 حرف (id=${a.id})")
        }
    }
}
