package com.example.debtledger.data.backup

import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import java.io.OutputStream

interface BackupDestination {
    fun openOutputStream(): OutputStream
    fun deletePartialFile(): Boolean
}

fun interface BackupDataProvider {
    suspend fun getPayload(): PlainBackupPayloadV1
}

object BackupExporter {
    private val jsonConfig = Json {
        ignoreUnknownKeys = false
        isLenient = false
    }

    suspend fun export(
        dataProvider: BackupDataProvider,
        destination: BackupDestination,
        password: String?
    ) {
        var streamOpened = false
        var writeException: Throwable? = null
        var outputStream: OutputStream? = null

        try {
            val payload = dataProvider.getPayload()

            // Validate payload rules before exporting
            BackupValidation.validatePayload(payload)

            val plainJson = jsonConfig.encodeToString(PlainBackupPayloadV1.serializer(), payload)

            val fileText = if (!password.isNullOrBlank()) {
                val envelope = BackupCrypto.encrypt(plainJson, password, payload.metadata.exportTimestamp)
                jsonConfig.encodeToString(BackupEnvelopeV1.serializer(), envelope)
            } else {
                plainJson
            }

            val bytes = fileText.toByteArray(Charsets.UTF_8)
            if (bytes.size > BackupCrypto.MAX_FILE_SIZE_BYTES) {
                throw IllegalArgumentException("حجم ملف النسخة الاحتياطية (${bytes.size} بايت) يتجاوز الحد الأقصى المسموح (15 ميجابايت)")
            }

            outputStream = destination.openOutputStream()
            streamOpened = true

            try {
                outputStream.write(bytes)
                outputStream.flush()
            } catch (e: Throwable) {
                writeException = e
                throw e
            } finally {
                try {
                    outputStream.close()
                } catch (closeEx: Throwable) {
                    if (writeException != null) {
                        writeException.addSuppressed(closeEx)
                    } else {
                        throw closeEx
                    }
                }
            }
        } catch (e: Throwable) {
            if (streamOpened) {
                runCatching { destination.deletePartialFile() }
            }
            if (e is CancellationException) {
                throw e
            }
            throw e
        }
    }
}
