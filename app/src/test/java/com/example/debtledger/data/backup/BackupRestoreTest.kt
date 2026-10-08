package com.example.debtledger.data.backup

import com.example.debtledger.domain.Currency
import com.example.debtledger.domain.DebtDirection
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.OutputStream
import javax.crypto.AEADBadTagException

class BackupRestoreTest {

    private val json = Json {
        ignoreUnknownKeys = false
        isLenient = false
    }

    private fun createValidSamplePayload(): PlainBackupPayloadV1 {
        val person = PersonDto("p1", "أحمد محمود", "0599000000", "ملاحظة أصلية", 12345L, 1000L, 1000L)
        val debt1 = DebtDto("d1", "p1", DebtDirection.RECEIVABLE.name, Currency.ILS.name, 10000L, 100L, "دين تجريبي شيكل", "ملاحظة دين", null, 1000L, 1000L)
        val debt2 = DebtDto("d2", "p1", DebtDirection.PAYABLE.name, Currency.USD.name, 50000L, 101L, "دين تجريبي دولار", "ملاحظة دولار", 1500L, 1000L, 1500L)
        val payment1 = PaymentDto("pay1", "d1", "op1", 5000L, 102L, "دفعة حقيقية", null, 1000L, 1000L)
        val payment2 = PaymentDto("pay2", "d1", "op2", 2000L, 103L, "دفعة ملغاة", 1600L, 1000L, 1600L)
        val audit = AuditDto("a1", "PAYMENT", "pay1", "CREATE", null, "{\"amountMinor\":5000}", "إنشاء دفعة", 1000L)

        val metadata = BackupMetadataDto(
            formatIdentifier = "DebtLedgerBackup",
            formatVersion = 1,
            databaseVersion = 1,
            appVersion = "0.1.0",
            exportTimestamp = 1000L,
            counts = BackupCountsDto(1, 2, 2, 1)
        )
        val data = BackupDataDto(
            persons = listOf(person),
            debts = listOf(debt1, debt2),
            payments = listOf(payment1, payment2),
            auditEvents = listOf(audit)
        )
        return PlainBackupPayloadV1(metadata, data)
    }

    // ════════════════════════════════════════════════════════════════
    // Item 1: Real Production Exporter Tests
    // ════════════════════════════════════════════════════════════════

    @Test
    fun testExportWriteFailure() = runBlocking {
        var cleanupCalled = false
        val failingStream = object : OutputStream() {
            override fun write(b: Int) {
                throw IOException("فشل الكتابة الأصلي")
            }
        }
        val destination = object : BackupDestination {
            override fun openOutputStream(): OutputStream = failingStream
            override fun deletePartialFile(): Boolean {
                cleanupCalled = true
                return true
            }
        }

        val provider = BackupDataProvider { createValidSamplePayload() }

        try {
            BackupExporter.export(provider, destination, null)
            fail("Expected IOException")
        } catch (e: IOException) {
            assertEquals("فشل الكتابة الأصلي", e.message)
            assertTrue("Expected cleanup deletePartialFile to be called on write failure", cleanupCalled)
        }
    }

    @Test
    fun testExportCloseFailure() = runBlocking {
        var streamClosed = false
        var cleanupCalled = false
        val failingCloseStream = object : OutputStream() {
            override fun write(b: Int) {}
            override fun close() {
                streamClosed = true
                throw IOException("فشل الإغلاق الأصلي")
            }
        }
        val destination = object : BackupDestination {
            override fun openOutputStream(): OutputStream = failingCloseStream
            override fun deletePartialFile(): Boolean {
                cleanupCalled = true
                return true
            }
        }

        val provider = BackupDataProvider { createValidSamplePayload() }

        try {
            BackupExporter.export(provider, destination, null)
            fail("Expected IOException")
        } catch (e: IOException) {
            assertEquals("فشل الإغلاق الأصلي", e.message)
            assertTrue("Expected stream to be closed", streamClosed)
            assertTrue("Expected cleanup deletePartialFile to be called on close failure", cleanupCalled)
        }
    }

    @Test
    fun testExportWriteAndCloseFailurePreservesOriginal() = runBlocking {
        var cleanupCalled = false
        val failingBothStream = object : OutputStream() {
            override fun write(b: Int) {
                throw IOException("فشل الكتابة الأصلي")
            }
            override fun close() {
                throw IOException("فشل الإغلاق الثانوي")
            }
        }
        val destination = object : BackupDestination {
            override fun openOutputStream(): OutputStream = failingBothStream
            override fun deletePartialFile(): Boolean {
                cleanupCalled = true
                return true
            }
        }

        val provider = BackupDataProvider { createValidSamplePayload() }

        try {
            BackupExporter.export(provider, destination, null)
            fail("Expected IOException")
        } catch (e: IOException) {
            assertEquals("فشل الكتابة الأصلي", e.message)
            assertEquals(1, e.suppressed.size)
            assertEquals("فشل الإغلاق الثانوي", e.suppressed[0].message)
            assertTrue("Expected cleanup deletePartialFile to be called", cleanupCalled)
        }
    }

    @Test
    fun testExportCleanupFailurePreservesOriginal() = runBlocking {
        val failingStream = object : OutputStream() {
            override fun write(b: Int) {
                throw IOException("فشل الكتابة الأصلي")
            }
        }
        val destination = object : BackupDestination {
            override fun openOutputStream(): OutputStream = failingStream
            override fun deletePartialFile(): Boolean {
                throw RuntimeException("فشل حذف الملف الجزئي")
            }
        }

        val provider = BackupDataProvider { createValidSamplePayload() }

        try {
            BackupExporter.export(provider, destination, null)
            fail("Expected IOException")
        } catch (e: IOException) {
            assertEquals("فشل الكتابة الأصلي", e.message)
        }
    }

    @Test
    fun testExportSuccessProducesImportableBackup() = runBlocking {
        val payload = createValidSamplePayload()
        val provider = BackupDataProvider { payload }
        val outStream = ByteArrayOutputStream()

        val destination = object : BackupDestination {
            override fun openOutputStream(): OutputStream = outStream
            override fun deletePartialFile(): Boolean = false
        }

        BackupExporter.export(provider, destination, "P@ssw0rd123")

        val exportedBytes = outStream.toByteArray()
        assertTrue(exportedBytes.isNotEmpty())

        val text = exportedBytes.toString(Charsets.UTF_8)
        val fileType = BackupParser.detectFileTypeAndValidateStructure(text)
        assertEquals(BackupFileType.ENCRYPTED_ENVELOPE, fileType)

        val envelope = json.decodeFromString(BackupEnvelopeV1.serializer(), text)
        BackupValidation.validateHeader(envelope.header)

        val decryptedJson = BackupCrypto.decrypt(envelope, "P@ssw0rd123")
        BackupParser.detectFileTypeAndValidateStructure(decryptedJson)

        val restoredPayload = json.decodeFromString(PlainBackupPayloadV1.serializer(), decryptedJson)
        BackupValidation.validatePayload(restoredPayload, envelope.header)
        assertEquals(payload, restoredPayload)
    }

    // ════════════════════════════════════════════════════════════════
    // Item 3: Pre-JSON Building Limits & Structure Validation Tests
    // ════════════════════════════════════════════════════════════════

    @Test
    fun testStringLengthBoundaryLimitsAcceptsLimitAndRejectsLimitPlusOne() {
        val name200 = "أ".repeat(200)
        val json200 = """
            {
              "metadata": {
                "formatIdentifier": "DebtLedgerBackup",
                "formatVersion": 1,
                "databaseVersion": 1,
                "appVersion": "0.1.0",
                "exportTimestamp": 1000,
                "counts": { "persons": 1, "debts": 0, "payments": 0, "auditEvents": 0 }
              },
              "data": {
                "persons": [
                  { "id": "p1", "name": "$name200", "createdAt": 1000, "updatedAt": 1000 }
                ],
                "debts": [], "payments": [], "auditEvents": []
              }
            }
        """.trimIndent()

        // Should be accepted
        assertEquals(BackupFileType.PLAIN_JSON, BackupParser.detectFileTypeAndValidateStructure(json200))

        val name201 = "أ".repeat(201)
        val json201 = """
            {
              "metadata": {
                "formatIdentifier": "DebtLedgerBackup",
                "formatVersion": 1,
                "databaseVersion": 1,
                "appVersion": "0.1.0",
                "exportTimestamp": 1000,
                "counts": { "persons": 1, "debts": 0, "payments": 0, "auditEvents": 0 }
              },
              "data": {
                "persons": [
                  { "id": "p1", "name": "$name201", "createdAt": 1000, "updatedAt": 1000 }
                ],
                "debts": [], "payments": [], "auditEvents": []
              }
            }
        """.trimIndent()

        try {
            BackupParser.detectFileTypeAndValidateStructure(json201)
            fail("Expected IllegalArgumentException for string length 201")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("يتجاوز الحد الأقصى"))
        }
    }

    @Test
    fun testRecordsCountBoundaryLimitsAcceptsLimitAndRejectsLimitPlusOne() {
        val persons50000Buf = StringBuilder("""{"metadata":{"formatIdentifier":"DebtLedgerBackup","formatVersion":1,"databaseVersion":1,"appVersion":"0.1.0","exportTimestamp":1000,"counts":{"persons":50000,"debts":0,"payments":0,"auditEvents":0}},"data":{"persons":[""")
        for (i in 0 until 50000) {
            if (i > 0) persons50000Buf.append(",")
            persons50000Buf.append("""{"id":"p$i","name":"شخص $i","createdAt":1000,"updatedAt":1000}""")
        }
        persons50000Buf.append("""],"debts":[],"payments":[],"auditEvents":[]}}""")

        assertEquals(BackupFileType.PLAIN_JSON, BackupParser.detectFileTypeAndValidateStructure(persons50000Buf.toString()))

        val persons50001Buf = StringBuilder("""{"metadata":{"formatIdentifier":"DebtLedgerBackup","formatVersion":1,"databaseVersion":1,"appVersion":"0.1.0","exportTimestamp":1000,"counts":{"persons":50001,"debts":0,"payments":0,"auditEvents":0}},"data":{"persons":[""")
        for (i in 0 until 50001) {
            if (i > 0) persons50001Buf.append(",")
            persons50001Buf.append("""{"id":"p$i","name":"شخص $i","createdAt":1000,"updatedAt":1000}""")
        }
        persons50001Buf.append("""],"debts":[],"payments":[],"auditEvents":[]}}""")

        try {
            BackupParser.detectFileTypeAndValidateStructure(persons50001Buf.toString())
            fail("Expected IllegalArgumentException for 50001 records")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("يتجاوز الحد الأقصى"))
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun testDeepArrayNestingExceedingFiveLevelsThrowsException() {
        val jsonDeepNesting = """
            {
              "deep": [[[[[[1]]]]]]
            }
        """.trimIndent()

        BackupParser.detectFileTypeAndValidateStructure(jsonDeepNesting)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testDuplicateKeysWithUnicodeEscapesThrowsIllegalArgumentException() {
        val jsonWithUnicodeEscapeDuplicate = """
            {
              "metadata": {
                "formatIdentifier": "DebtLedgerBackup",
                "formatVersion": 1,
                "databaseVersion": 1,
                "appVersion": "0.1.0",
                "exportTimestamp": 1000,
                "counts": { "persons": 0, "debts": 0, "payments": 0, "auditEvents": 0 }
              },
              "data": {
                "persons": [
                  {
                    "id": "p1",
                    "name": "علي",
                    "\u006eame": "علي مكرر",
                    "createdAt": 1000,
                    "updatedAt": 1000
                  }
                ],
                "debts": [],
                "payments": [],
                "auditEvents": []
              }
            }
        """.trimIndent()

        BackupParser.detectFileTypeAndValidateStructure(jsonWithUnicodeEscapeDuplicate)
    }

    @Test
    fun testEncryptedPayloadViolatingValidationRulesIsRejectedAfterDecryption() {
        val invalidPayloadJson = """
            {
              "metadata": {
                "formatIdentifier": "DebtLedgerBackup",
                "formatVersion": 1,
                "databaseVersion": 1,
                "appVersion": "0.1.0",
                "exportTimestamp": 1000,
                "counts": { "persons": 1, "debts": 0, "payments": 0, "auditEvents": 0 }
              },
              "data": {
                "persons": [
                  {
                    "id": "p1",
                    "name": "علي",
                    "\u006eame": "مكرر",
                    "createdAt": 1000,
                    "updatedAt": 1000
                  }
                ],
                "debts": [], "payments": [], "auditEvents": []
              }
            }
        """.trimIndent()

        val envelope = BackupCrypto.encrypt(invalidPayloadJson, "Password123", 1000L)

        val decryptedJson = BackupCrypto.decrypt(envelope, "Password123")

        try {
            BackupParser.detectFileTypeAndValidateStructure(decryptedJson)
            fail("Expected IllegalArgumentException when validating decrypted JSON with duplicate key")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("مفتاح مكرر"))
        }
    }

    @Test
    fun testLargeCiphertextExceedingTwoMillionCharsAcceptedIfUnderFifteenMiB() {
        val largePadding = "A".repeat(2500000)
        val jsonWithLargeCiphertext = """
            {
              "header": {
                "formatIdentifier": "DebtLedgerBackup",
                "formatVersion": 1,
                "databaseVersion": 1,
                "cipherAlgorithm": "AES-256-GCM",
                "kdf": "PBKDF2WithHmacSHA256",
                "kdfIterations": 100000,
                "salt": "AAECAwQFBgcICQoLDA0ODw==",
                "nonce": "AAECAwQFBgcICQoL",
                "exportTimestamp": 1000
              },
              "ciphertext": "$largePadding"
            }
        """.trimIndent()

        val fileType = BackupParser.detectFileTypeAndValidateStructure(jsonWithLargeCiphertext)
        assertEquals(BackupFileType.ENCRYPTED_ENVELOPE, fileType)
    }

    // ════════════════════════════════════════════════════════════════
    // Round-trip & Crypto Tests
    // ════════════════════════════════════════════════════════════════

    @Test
    fun testFullRoundTripAllEntitiesArchivedVoidedCurrencies() {
        val payload = createValidSamplePayload()
        BackupValidation.validatePayload(payload)

        val jsonStr = json.encodeToString(PlainBackupPayloadV1.serializer(), payload)
        val fileType = BackupParser.detectFileTypeAndValidateStructure(jsonStr)
        assertEquals(BackupFileType.PLAIN_JSON, fileType)

        val decoded = json.decodeFromString(PlainBackupPayloadV1.serializer(), jsonStr)
        assertEquals(payload, decoded)
    }

    @Test
    fun testEncryptedBackupRoundTripAndFileTypeDetection() {
        val payload = createValidSamplePayload()
        val jsonStr = json.encodeToString(PlainBackupPayloadV1.serializer(), payload)

        val envelope = BackupCrypto.encrypt(jsonStr, "P@ssw0rd123", payload.metadata.exportTimestamp)
        val envelopeJson = json.encodeToString(BackupEnvelopeV1.serializer(), envelope)

        val fileType = BackupParser.detectFileTypeAndValidateStructure(envelopeJson)
        assertEquals(BackupFileType.ENCRYPTED_ENVELOPE, fileType)

        val decodedEnvelope = json.decodeFromString(BackupEnvelopeV1.serializer(), envelopeJson)
        BackupValidation.validateHeader(decodedEnvelope.header)

        val decryptedJson = BackupCrypto.decrypt(decodedEnvelope, "P@ssw0rd123")
        val restoredPayload = json.decodeFromString(PlainBackupPayloadV1.serializer(), decryptedJson)

        BackupValidation.validatePayload(restoredPayload, decodedEnvelope.header)
        assertEquals(payload, restoredPayload)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testDuplicateKeysInJsonObjectThrowsIllegalArgumentException() {
        val jsonWithDuplicateKey = """
            {
              "metadata": {
                "formatIdentifier": "DebtLedgerBackup",
                "formatVersion": 1,
                "databaseVersion": 1,
                "appVersion": "0.1.0",
                "exportTimestamp": 1000,
                "exportTimestamp": 2000,
                "counts": { "persons": 0, "debts": 0, "payments": 0, "auditEvents": 0 }
              },
              "data": { "persons": [], "debts": [], "payments": [], "auditEvents": [] }
            }
        """.trimIndent()

        BackupParser.detectFileTypeAndValidateStructure(jsonWithDuplicateKey)
    }

    @Test(expected = SerializationException::class)
    fun testUnknownJsonFieldsThrowSerializationException() {
        val jsonWithUnknownField = """
            {
              "metadata": {
                "formatIdentifier": "DebtLedgerBackup",
                "formatVersion": 1,
                "databaseVersion": 1,
                "appVersion": "0.1.0",
                "exportTimestamp": 1000,
                "unknownKey": "hackerValue",
                "counts": { "persons": 0, "debts": 0, "payments": 0, "auditEvents": 0 }
              },
              "data": { "persons": [], "debts": [], "payments": [], "auditEvents": [] }
            }
        """.trimIndent()

        json.decodeFromString(PlainBackupPayloadV1.serializer(), jsonWithUnknownField)
    }

    @Test(expected = AEADBadTagException::class)
    fun testWrongPasswordThrowsAEADBadTagException() {
        val payload = createValidSamplePayload()
        val jsonStr = json.encodeToString(PlainBackupPayloadV1.serializer(), payload)

        val envelope = BackupCrypto.encrypt(jsonStr, "CorrectPassword", payload.metadata.exportTimestamp)
        BackupCrypto.decrypt(envelope, "WrongPassword")
    }

    @Test(expected = AEADBadTagException::class)
    fun testTamperedCiphertextThrowsAEADBadTagException() {
        val payload = createValidSamplePayload()
        val jsonStr = json.encodeToString(PlainBackupPayloadV1.serializer(), payload)

        val envelope = BackupCrypto.encrypt(jsonStr, "P@ssw0rd123", payload.metadata.exportTimestamp)
        val tamperedCiphertext = envelope.ciphertext.dropLast(4) + "AAAA"
        val tamperedEnvelope = envelope.copy(ciphertext = tamperedCiphertext)

        BackupCrypto.decrypt(tamperedEnvelope, "P@ssw0rd123")
    }

    @Test(expected = AEADBadTagException::class)
    fun testTamperedHeaderSaltThrowsAEADBadTagException() {
        val payload = createValidSamplePayload()
        val jsonStr = json.encodeToString(PlainBackupPayloadV1.serializer(), payload)

        val envelope = BackupCrypto.encrypt(jsonStr, "P@ssw0rd123", payload.metadata.exportTimestamp)
        val tamperedHeader = envelope.header.copy(salt = "AAECAwQFBgcICQoLDA0OAZ==")
        val tamperedEnvelope = envelope.copy(header = tamperedHeader)

        BackupCrypto.decrypt(tamperedEnvelope, "P@ssw0rd123")
    }

    @Test(expected = IllegalArgumentException::class)
    fun testHeaderUnsupportedFormatVersionThrowsIllegalArgumentException() {
        val header = BackupHeaderV1(
            formatIdentifier = "DebtLedgerBackup",
            formatVersion = 99,
            databaseVersion = 1,
            cipherAlgorithm = "AES-256-GCM",
            kdf = "PBKDF2WithHmacSHA256",
            kdfIterations = 100000,
            salt = "AAECAwQFBgcICQoLDA0ODw==",
            nonce = "AAECAwQFBgcICQoL",
            exportTimestamp = 1000L
        )
        BackupValidation.validateHeader(header)
    }

    @Test
    fun testBoundedInputStreamBoundaryLimits() {
        val maxBytes = 100L

        val streamExact = ByteArrayInputStream(ByteArray(100))
        val boundedExact = BoundedInputStream(streamExact, maxBytes = maxBytes)
        val bufExact = ByteArray(100)
        assertEquals(100, boundedExact.read(bufExact, 0, 100))

        val streamExceed = ByteArrayInputStream(ByteArray(101))
        val boundedExceed = BoundedInputStream(streamExceed, maxBytes = maxBytes)
        try {
            val buf = ByteArray(101)
            boundedExceed.read(buf, 0, 101)
            fail("Expected BoundedInputStream to throw IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("تجاوز حجم الملف"))
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun testOverpaymentValidationFails() {
        val person = PersonDto("p1", "علي", null, null, null, 1000L, 1000L)
        val debt = DebtDto("d1", "p1", DebtDirection.RECEIVABLE.name, Currency.ILS.name, 10000L, 100L, "دين", null, null, 1000L, 1000L)
        val payment = PaymentDto("pay1", "d1", "op1", 15000L, 101L, null, null, 1000L, 1000L)

        val data = BackupDataDto(listOf(person), listOf(debt), listOf(payment), emptyList())
        BackupValidation.validateData(data)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testTotalCurrencyOverflowFailsValidation() {
        val person = PersonDto("p1", "علي", null, null, null, 1000L, 1000L)
        val debt1 = DebtDto("d1", "p1", DebtDirection.RECEIVABLE.name, Currency.ILS.name, Long.MAX_VALUE - 100L, 100L, "دين 1", null, null, 1000L, 1000L)
        val debt2 = DebtDto("d2", "p1", DebtDirection.RECEIVABLE.name, Currency.ILS.name, 500L, 101L, "دين 2", null, null, 1000L, 1000L)

        val data = BackupDataDto(listOf(person), listOf(debt1, debt2), emptyList(), emptyList())
        BackupValidation.validateData(data)
    }
}
