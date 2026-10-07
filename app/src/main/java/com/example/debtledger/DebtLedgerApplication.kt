package com.example.debtledger

import android.app.Application
import android.content.Context
import androidx.room.Room
import com.example.debtledger.data.LedgerRepository
import com.example.debtledger.data.local.AppDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate

class DebtLedgerApplication : Application() {
    val database: AppDatabase by lazy {
        Room.databaseBuilder(this,AppDatabase::class.java,"debt-ledger.db").build()
    }
    val repository: LedgerRepository by lazy {
        LedgerRepository(database,System::currentTimeMillis) { LocalDate.now().toEpochDay() }
    }
    private val _darkTheme = MutableStateFlow<Boolean?>(null)
    val darkTheme = _darkTheme.asStateFlow()

    override fun onCreate() {
        super.onCreate()
        val prefs = getSharedPreferences("debt_ledger_prefs", MODE_PRIVATE)
        if (prefs.contains("dark_theme")) {
            _darkTheme.value = prefs.getBoolean("dark_theme", false)
        }
    }

    fun setDarkTheme(isDark: Boolean) {
        val prefs = getSharedPreferences("debt_ledger_prefs", MODE_PRIVATE)
        if (isDark == null) {
            prefs.edit().remove("dark_theme").apply()
        } else {
            prefs.edit().putBoolean("dark_theme", isDark).apply()
        }
        _darkTheme.value = isDark
    }
}
