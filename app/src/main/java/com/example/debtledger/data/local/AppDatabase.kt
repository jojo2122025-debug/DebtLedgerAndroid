package com.example.debtledger.data.local

import androidx.room.*

@Database(entities = [PersonEntity::class, DebtEntity::class, PaymentEntity::class,
    AuditEntity::class], views = [DebtBalance::class], version = 1, exportSchema = true)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun ledgerDao(): LedgerDao
}
