package com.example.debtledger.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.debtledger.DebtLedgerApplication
import com.example.debtledger.domain.*
import com.example.debtledger.data.local.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class LedgerViewModel(application: Application) : AndroidViewModel(application) {
    val app = application as DebtLedgerApplication
    val dao = app.database.ledgerDao()
    val repository = app.repository
    val darkTheme = app.darkTheme
    fun setDarkTheme(isDark: Boolean) { app.setDarkTheme(isDark) }

    private val _error=MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()
    val persons = dao.observePersons().catch {_error.value="تعذر قراءة الأشخاص";emit(emptyList())}.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),emptyList())
    val debts = dao.observeAllDebts().catch {_error.value="تعذر قراءة الديون";emit(emptyList())}.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),emptyList())
    val activity = dao.observeActivity().catch {_error.value="تعذر قراءة سجل النشاط";emit(emptyList())}.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),emptyList())
    private val _busy=MutableStateFlow(false)
    val busy = _busy.asStateFlow()
    fun clearError() { _error.value=null }
    fun payments(id: String) = dao.observePayments(id).catch {_error.value="تعذر قراءة الدفعات";emit(emptyList())}
    fun act(success: (String?) -> Unit = {}, action: suspend () -> String?) {
        if(_busy.value) return
        _busy.value=true; _error.value=null
        viewModelScope.launch {
            try { success(action()) }
            catch(e: CancellationException) { throw e }
            catch(e: RuleViolation) { _error.value = when(e.code) {
                RuleError.OVERPAYMENT -> "المبلغ يتجاوز المتبقي من الدين"
                RuleError.BELOW_PAID -> "قيمة الدين أقل من مجموع الدفعات"
                RuleError.FUTURE_DATE -> "لا يمكن تسجيل تاريخ مستقبلي"
                RuleError.BEFORE_DEBT_DATE -> "تاريخ الدفعة يسبق تاريخ الدين"
                RuleError.DEBT_DATE_AFTER_PAYMENT -> "تاريخ الدين أصبح بعد إحدى دفعاته"
                RuleError.AMOUNT_PRECISION -> "المبلغ يقبل منزلتين عشريتين فقط"
                RuleError.NON_POSITIVE_AMOUNT -> "المبلغ يجب أن يكون أكبر من صفر"
                RuleError.AMOUNT_OUT_OF_RANGE -> "المبلغ أكبر من الحدود المتاحة"
                else -> "راجع المبلغ والتاريخ المدخلين"
            } }
            catch(e: IllegalArgumentException) { _error.value="العملية غير مسموحة؛ راجع البيانات وارتباطاتها" }
            catch(e: Exception) { _error.value="تعذر حفظ العملية. بقيت البيانات السابقة محفوظة؛ أعد المحاولة" }
            finally { _busy.value=false }
        }
    }
}
