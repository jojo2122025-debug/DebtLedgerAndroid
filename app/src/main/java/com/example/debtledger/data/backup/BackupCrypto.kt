package com.example.debtledger.data.backup

import java.io.InputStream
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object BackupCrypto {
    const val MAX_FILE_SIZE_BYTES = 15 * 1024 * 1024L // 15 MiB = 15,728,640 bytes
    const val MAX_RECORDS_LIMIT = 50000

    fun computeAadBytes(header: BackupHeaderV1): ByteArray {
        val canonicalStr = "formatIdentifier=${header.formatIdentifier}," +
                "formatVersion=${header.formatVersion}," +
                "databaseVersion=${header.databaseVersion}," +
                "cipherAlgorithm=${header.cipherAlgorithm}," +
                "kdf=${header.kdf}," +
                "kdfIterations=${header.kdfIterations}," +
                "salt=${header.salt}," +
                "nonce=${header.nonce}," +
                "exportTimestamp=${header.exportTimestamp}"
        return canonicalStr.toByteArray(Charsets.UTF_8)
    }

    private fun deriveKey(password: CharArray, salt: ByteArray, iterations: Int): SecretKeySpec {
        val spec = PBEKeySpec(password, salt, iterations, 256)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val keyBytes = factory.generateSecret(spec).encoded
        return SecretKeySpec(keyBytes, "AES")
    }

    fun encrypt(plaintextJson: String, password: String, exportTimestamp: Long): BackupEnvelopeV1 {
        val random = SecureRandom()
        val salt = ByteArray(16)
        val nonce = ByteArray(12)
        random.nextBytes(salt)
        random.nextBytes(nonce)

        val saltBase64 = Base64.getEncoder().encodeToString(salt)
        val nonceBase64 = Base64.getEncoder().encodeToString(nonce)

        val header = BackupHeaderV1(
            formatIdentifier = "DebtLedgerBackup",
            formatVersion = 1,
            databaseVersion = 1,
            cipherAlgorithm = "AES-256-GCM",
            kdf = "PBKDF2WithHmacSHA256",
            kdfIterations = 100000,
            salt = saltBase64,
            nonce = nonceBase64,
            exportTimestamp = exportTimestamp
        )

        val secretKey = deriveKey(password.toCharArray(), salt, header.kdfIterations)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(128, nonce))
        cipher.updateAAD(computeAadBytes(header))

        val encryptedBytes = cipher.doFinal(plaintextJson.toByteArray(Charsets.UTF_8))
        val ciphertextBase64 = Base64.getEncoder().encodeToString(encryptedBytes)

        return BackupEnvelopeV1(header = header, ciphertext = ciphertextBase64)
    }

    fun decrypt(envelope: BackupEnvelopeV1, password: String): String {
        val header = envelope.header

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

        val salt = try {
            Base64.getDecoder().decode(header.salt)
        } catch (e: Exception) {
            throw IllegalArgumentException("تنسيق Salt في الترويسة غير صريح")
        }
        if (salt.size != 16) throw IllegalArgumentException("طول Salt يجب أن يكون 16 بايت")

        val nonce = try {
            Base64.getDecoder().decode(header.nonce)
        } catch (e: Exception) {
            throw IllegalArgumentException("تنسيق Nonce في الترويسة غير صريح")
        }
        if (nonce.size != 12) throw IllegalArgumentException("طول Nonce يجب أن يكون 12 بايت")

        val ciphertextBytes = try {
            Base64.getDecoder().decode(envelope.ciphertext)
        } catch (e: Exception) {
            throw IllegalArgumentException("تنسيق النص المشفر غير صالح")
        }

        if (ciphertextBytes.size > MAX_FILE_SIZE_BYTES) {
            throw IllegalArgumentException("حجم النص المشفر يتجاوز الحد الأقصى المسموح (15 ميجابايت)")
        }

        val secretKey = deriveKey(password.toCharArray(), salt, header.kdfIterations)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(128, nonce))
        cipher.updateAAD(computeAadBytes(header))

        val decryptedBytes = cipher.doFinal(ciphertextBytes)
        return String(decryptedBytes, Charsets.UTF_8)
    }
}

class BoundedInputStream(
    private val delegate: InputStream,
    private val maxBytes: Long = BackupCrypto.MAX_FILE_SIZE_BYTES
) : InputStream() {
    private var totalBytesRead = 0L

    override fun read(): Int {
        val nextByte = delegate.read()
        if (nextByte != -1) {
            totalBytesRead++
            if (totalBytesRead > maxBytes) {
                throw IllegalArgumentException("تجاوز حجم الملف الحد الأقصى المسموح (15 ميجابايت)")
            }
        }
        return nextByte
    }

    override fun read(b: ByteArray, off: Int, len: Int): Int {
        val bytesRead = delegate.read(b, off, len)
        if (bytesRead != -1) {
            totalBytesRead += bytesRead
            if (totalBytesRead > maxBytes) {
                throw IllegalArgumentException("تجاوز حجم الملف الحد الأقصى المسموح (15 ميجابايت)")
            }
        }
        return bytesRead
    }

    override fun close() {
        delegate.close()
    }
}
