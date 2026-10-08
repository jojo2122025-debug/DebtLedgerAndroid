package com.example.debtledger.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.debtledger.DebtLedgerApplication
import com.example.debtledger.data.backup.*
import com.example.debtledger.data.local.*
import com.example.debtledger.domain.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream
import javax.crypto.AEADBadTagException

class LedgerViewModel(application: Application) : AndroidViewModel(application) {
    val app = application as DebtLedgerApplication
    val dao = app.database.ledgerDao()
    val repository = app.repository
    val darkTheme = app.darkTheme
    fun setDarkTheme(isDark: Boolean) { app.setDarkTheme(isDark) }

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private val _backupMessage = MutableStateFlow<String?>(null)
    val backupMessage = _backupMessage.asStateFlow()

    private val _pendingPreviewPayload = MutableStateFlow<PlainBackupPayloadV1?>(null)
    val pendingPreviewPayload = _pendingPreviewPayload.asStateFlow()

    private val _pendingPreviewFingerprint = MutableStateFlow<String?>(null)

    val persons = dao.observePersons().catch { _error.value = "تعذر قراءة الأشخاص"; emit(emptyList()) }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val debts = dao.observeAllDebts().catch { _error.value = "تعذر قراءة الديون"; emit(emptyList()) }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val activity = dao.observeActivity().catch { _error.value = "تعذر قراءة سجل النشاط"; emit(emptyList()) }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    private val _busy = MutableStateFlow(false)
    val busy = _busy.asStateFlow()

    fun clearError() { _error.value = null }
    fun clearBackupMessage() { _backupMessage.value = null }
    fun dismissPreview() {
        _pendingPreviewPayload.value = null
        _pendingPreviewFingerprint.value = null
    }

    fun payments(id: String) = dao.observePayments(id).catch { _error.value = "تعذر قراءة الدفعات"; emit(emptyList()) }

    private val jsonConfig = Json {
        ignoreUnknownKeys = false
        isLenient = false
    }

    fun exportBackupToDestination(destination: BackupDestination, password: String?, onComplete: (() -> Unit)? = null): Job {
        if (_busy.value) {
            val emptyJob = CompletableDeferred<Unit>()
            emptyJob.complete(Unit)
            return emptyJob
        }
        _busy.value = true
        _error.value = null
        _backupMessage.value = null

        return viewModelScope.launch(Dispatchers.IO) {
            try {
                BackupExporter.export(
                    dataProvider = { repository.exportBackupPayload() },
                    destination = destination,
                    password = password
                )
                _backupMessage.value = "تمت عملية إنشاء النسخة الاحتياطية بنجاح"
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _error.value = "تعذر تصدير النسخة الاحتياطية: ${e.localizedMessage ?: "خطأ أثناء الكتابة"}"
            } finally {
                _busy.value = false
                onComplete?.invoke()
            }
        }
    }

    fun exportBackup(outputStream: OutputStream, password: String?) {
        val simpleDestination = object : BackupDestination {
            override fun openOutputStream(): OutputStream = outputStream
            override fun deletePartialFile(): Boolean = false
        }
        exportBackupToDestination(simpleDestination, password)
    }

    fun importBackupPreview(inputStream: InputStream, password: String?, isEncryptedFile: Boolean) {
        if (_busy.value) return
        _busy.value = true
        _error.value = null
        _backupMessage.value = null

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val text = inputStream.use { stream ->
                    BoundedInputStream(stream).bufferedReader(Charsets.UTF_8).readText()
                }

                val fileType = BackupParser.detectFileTypeAndValidateStructure(text)
                var header: BackupHeaderV1? = null

                val payload = if (fileType == BackupFileType.ENCRYPTED_ENVELOPE) {
                    if (password.isNullOrBlank()) {
                        throw IllegalArgumentException("يرجى إدخال كلمة المرور لفك تشفير النسخة الاحتياطية")
                    }
                    val envelope = try {
                        jsonConfig.decodeFromString(BackupEnvelopeV1.serializer(), text)
                    } catch (e: Exception) {
                        throw IllegalArgumentException("صيغة غلاف النسخة الاحتياطية المشفرة غير صالحة")
                    }
                    header = envelope.header
                    BackupValidation.validateHeader(envelope.header)

                    val decryptedJson = try {
                        BackupCrypto.decrypt(envelope, password)
                    } catch (e: AEADBadTagException) {
                        throw IllegalArgumentException("كلمة المرور غير صحيحة أو أن ملف النسخة الاحتياطية معدّل/تالف")
                    }

                    // Validate structural depth and keys of decrypted JSON payload before DTO parsing
                    BackupParser.detectFileTypeAndValidateStructure(decryptedJson)

                    jsonConfig.decodeFromString(PlainBackupPayloadV1.serializer(), decryptedJson)
                } else {
                    if (isEncryptedFile) {
                        throw IllegalArgumentException("الملف المختار ينتهي بـ .dlbak ولكنه لا يحتوي بنية غلاف مشفرة صالحة")
                    }
                    try {
                        jsonConfig.decodeFromString(PlainBackupPayloadV1.serializer(), text)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        throw IllegalArgumentException("صيغة النسخة الاحتياطية غير المشفرة غير صالحة أو تحتوي على حقول غير معروفة/مكررة")
                    }
                }

                // Run pre-restore validation pipeline
                BackupValidation.validatePayload(payload, header)

                val fingerprint = repository.getDatabaseFingerprint()
                _pendingPreviewFingerprint.value = fingerprint
                _pendingPreviewPayload.value = payload

            } catch (e: CancellationException) {
                throw e
            } catch (e: IllegalArgumentException) {
                _error.value = e.message
            } catch (e: AEADBadTagException) {
                _error.value = "كلمة المرور غير صحيحة أو أن ملف النسخة الاحتياطية معدّل/تالف"
            } catch (e: Exception) {
                _error.value = "تعذر فحص قراءة ملف النسخة الاحتياطية: ${e.localizedMessage ?: "ملف غير صالح"}"
            } finally {
                _busy.value = false
            }
        }
    }

    fun confirmRestore(onSuccess: () -> Unit) {
        val payload = _pendingPreviewPayload.value
        val fingerprint = _pendingPreviewFingerprint.value
        if (payload == null || fingerprint == null) {
            _error.value = "لا توجد نسخة معتمدة للاستعادة"
            return
        }

        if (_busy.value) return
        _busy.value = true
        _error.value = null

        viewModelScope.launch(Dispatchers.IO) {
            var restoreCommitted = false
            try {
                repository.restoreBackupPayload(payload, fingerprint)
                restoreCommitted = true
                _pendingPreviewPayload.value = null
                _pendingPreviewFingerprint.value = null
                _backupMessage.value = "تمت استعادة البيانات بالكامل بنجاح"
                withContext(Dispatchers.Main) {
                    onSuccess()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: RuleViolation) {
                if (!restoreCommitted) {
                    _error.value = if (e.code == RuleError.STALE_WRITE) {
                        "تغيرت البيانات المحلية أثناء المعاينة؛ يرجى إعادة اختيار الملف والمعاينة مجدداً"
                    } else {
                        "فشلت عملية الاستعادة: ${e.message}"
                    }
                    _pendingPreviewPayload.value = null
                    _pendingPreviewFingerprint.value = null
                }
            } catch (e: Exception) {
                if (!restoreCommitted) {
                    _error.value = "فشلت عملية الاستعادة؛ تم إلغاء المعاملة وبقيت البيانات الحالية كاملة 100%"
                    _pendingPreviewPayload.value = null
                    _pendingPreviewFingerprint.value = null
                }
            } finally {
                _busy.value = false
            }
        }
    }

    fun act(success: (String?) -> Unit = {}, action: suspend () -> String?) {
        if (_busy.value) return
        _busy.value = true; _error.value = null
        viewModelScope.launch {
            try {
                success(action())
            } catch (e: CancellationException) {
                throw e
            } catch (e: RuleViolation) {
                _error.value = when (e.code) {
                    RuleError.OVERPAYMENT -> "المبلغ يتجاوز المتبقي من الدين"
                    RuleError.BELOW_PAID -> "قيمة الدين أقل من مجموع الدفعات"
                    RuleError.FUTURE_DATE -> "لا يمكن تسجيل تاريخ مستقبلي"
                    RuleError.BEFORE_DEBT_DATE -> "تاريخ الدفعة يسبق تاريخ الدين"
                    RuleError.DEBT_DATE_AFTER_PAYMENT -> "تاريخ الدين أصبح بعد إحدى دفعاته"
                    RuleError.AMOUNT_PRECISION -> "المبلغ يقبل منزلتين عشريتين فقط"
                    RuleError.NON_POSITIVE_AMOUNT -> "المبلغ يجب أن يكون أكبر من صفر"
                    RuleError.AMOUNT_OUT_OF_RANGE -> "المبلغ أكبر من الحدود المتاحة"
                    RuleError.STALE_WRITE -> "تغيرت البيانات أثناء المعالجة، يرجى إعادة المحاولة"
                    else -> "راجع المبلغ والتاريخ المدخلين"
                }
            } catch (e: IllegalArgumentException) {
                _error.value = "العملية غير مسموحة؛ راجع البيانات وارتباطاتها"
            } catch (e: Exception) {
                _error.value = "تعذر حفظ العملية. بقيت البيانات السابقة محفوظة؛ أعد المحاولة"
            } finally {
                _busy.value = false
            }
        }
    }
}
