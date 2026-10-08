package com.example.debtledger

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.debtledger.data.LedgerRepository
import com.example.debtledger.data.RestoreTestFailurePoint
import com.example.debtledger.data.backup.*
import com.example.debtledger.data.local.AppDatabase
import com.example.debtledger.domain.Currency
import com.example.debtledger.domain.DebtDirection
import com.example.debtledger.domain.RuleError
import com.example.debtledger.domain.RuleViolation
import com.example.debtledger.ui.LedgerViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import java.io.OutputStream
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class BackupRestoreAndroidTest {
    private lateinit var db: AppDatabase
    private lateinit var repo: LedgerRepository

    @Before
    fun setup() = runBlocking {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).build()
        repo = LedgerRepository(db, { 1000L }, { 20L })
    }

    @After
    fun close() {
        db.close()
    }

    @Test
    fun testViewModelExportToFailingDestination() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<DebtLedgerApplication>()
        val testDb = Room.inMemoryDatabaseBuilder(app, AppDatabase::class.java).build()
        val testRepo = LedgerRepository(testDb, { 1000L }, { 20L })
        app.overrideDatabase = testDb
        app.overrideRepository = testRepo

        try {
            val viewModel = LedgerViewModel(app)

            var streamClosed = false
            var cleanupCalled = false

            val failingStream = object : OutputStream() {
                override fun write(b: Int) {
                    throw IOException("خطأ كتابة القرص")
                }
                override fun close() {
                    streamClosed = true
                }
            }

            val failingDestination = object : BackupDestination {
                override fun openOutputStream(): OutputStream = failingStream
                override fun deletePartialFile(): Boolean {
                    cleanupCalled = true
                    return true
                }
            }

            val job = viewModel.exportBackupToDestination(failingDestination, "P@ssw0rd123")
            job.join()

            assertFalse("Expected viewModel busy to be false after export completion", viewModel.busy.value)
            assertNull("Expected backupMessage to be null on export failure", viewModel.backupMessage.value)
            assertNotNull("Expected error to contain error message on export failure", viewModel.error.value)
            assertTrue("Expected error message to contain export failure notice", viewModel.error.value!!.contains("تعذر تصدير النسخة الاحتياطية"))
            assertTrue("Expected stream to be closed", streamClosed)
            assertTrue("Expected cleanup deletePartialFile to be called", cleanupCalled)
        } finally {
            app.overrideDatabase = null
            app.overrideRepository = null
            testDb.close()
        }
    }

    @Test
    fun testRepositoryExportAndRestoreRoundTrip() = runBlocking {
        val pId = repo.savePerson(null, "محمود شاكر", "0599111222", "ملاحظة")
        val dId = repo.saveDebt(null, pId, DebtDirection.RECEIVABLE, Currency.ILS, 100000L, 10L, "دين تجاري", "بدون")
        val payId = repo.addPayment(UUID.randomUUID().toString(), dId, 25000L, 11L, "دفعة نقدية")
        repo.archivePerson(pId)

        val exportedPayload = repo.exportBackupPayload()
        val fingerprint = repo.getDatabaseFingerprint()

        assertEquals(1, exportedPayload.data.persons.size)
        assertEquals(1, exportedPayload.data.debts.size)
        assertEquals(1, exportedPayload.data.payments.size)
        assertTrue(exportedPayload.data.auditEvents.isNotEmpty())

        // Restore
        repo.restoreBackupPayload(exportedPayload, fingerprint)

        val personsAfter = db.ledgerDao().observePersons().first()
        val debtsAfter = db.ledgerDao().observeAllDebts().first()
        val paymentsAfter = db.ledgerDao().observePayments(dId).first()
        val activityAfter = db.ledgerDao().observeActivity(100).first()

        assertEquals(1, personsAfter.size)
        assertEquals("محمود شاكر", personsAfter[0].name)
        assertNotNull(personsAfter[0].archivedAt)

        assertEquals(1, debtsAfter.size)
        assertEquals(100000L, debtsAfter[0].originalAmountMinor)
        assertEquals(25000L, debtsAfter[0].paidMinor)
        assertEquals(75000L, debtsAfter[0].remainingMinor)

        assertEquals(1, paymentsAfter.size)
        assertEquals(25000L, paymentsAfter[0].amountMinor)
        assertTrue(activityAfter.isNotEmpty())
    }

    @Test
    fun testRealRollbackInjectedAfterDeletePreservesAllFourTablesFieldByField() = runBlocking {
        val pId = repo.savePerson(null, "سليمان العلي", "0598765432", "ملاحظات شخصية")
        val dId = repo.saveDebt(null, pId, DebtDirection.RECEIVABLE, Currency.ILS, 150000L, 10L, "تجارة مواشي", "تفاصيل دين")
        val payId = repo.addPayment(UUID.randomUUID().toString(), dId, 50000L, 11L, "دفعة أولى")

        val personsBefore = db.ledgerDao().getAllPersons()
        val debtsBefore = db.ledgerDao().getAllDebts()
        val paymentsBefore = db.ledgerDao().getAllPayments()
        val auditsBefore = db.ledgerDao().getAllAuditEvents()

        val validPayload = repo.exportBackupPayload()
        val fingerprint = repo.getDatabaseFingerprint()
        val initialGen = repo.currentGeneration

        repo.testRestoreFailurePoint = RestoreTestFailurePoint.AFTER_DELETE

        try {
            repo.restoreBackupPayload(validPayload, fingerprint)
            fail("Expected injected failure AFTER_DELETE")
        } catch (e: IllegalStateException) {
            assertEquals("TEST_INJECTED_FAILURE_AFTER_DELETE", e.message)
        } finally {
            repo.testRestoreFailurePoint = null
        }

        val personsAfter = db.ledgerDao().getAllPersons()
        val debtsAfter = db.ledgerDao().getAllDebts()
        val paymentsAfter = db.ledgerDao().getAllPayments()
        val auditsAfter = db.ledgerDao().getAllAuditEvents()

        assertEquals(personsBefore, personsAfter)
        assertEquals(debtsBefore, debtsAfter)
        assertEquals(paymentsBefore, paymentsAfter)
        assertEquals(auditsBefore, auditsAfter)
        assertEquals(initialGen, repo.currentGeneration)
    }

    @Test
    fun testRealRollbackInjectedMidInsertPreservesAllFourTablesFieldByField() = runBlocking {
        val pId = repo.savePerson(null, "خالد مصطفى", "0599000111", "ملاحظات")
        val dId = repo.saveDebt(null, pId, DebtDirection.PAYABLE, Currency.USD, 200000L, 10L, "قرض بالدولار", "تفاصيل")
        val payId = repo.addPayment(UUID.randomUUID().toString(), dId, 40000L, 11L, "دفعة دولارية")

        val personsBefore = db.ledgerDao().getAllPersons()
        val debtsBefore = db.ledgerDao().getAllDebts()
        val paymentsBefore = db.ledgerDao().getAllPayments()
        val auditsBefore = db.ledgerDao().getAllAuditEvents()

        val validPayload = repo.exportBackupPayload()
        val fingerprint = repo.getDatabaseFingerprint()
        val initialGen = repo.currentGeneration

        repo.testRestoreFailurePoint = RestoreTestFailurePoint.MID_INSERT

        try {
            repo.restoreBackupPayload(validPayload, fingerprint)
            fail("Expected injected failure MID_INSERT")
        } catch (e: IllegalStateException) {
            assertEquals("TEST_INJECTED_FAILURE_MID_INSERT", e.message)
        } finally {
            repo.testRestoreFailurePoint = null
        }

        val personsAfter = db.ledgerDao().getAllPersons()
        val debtsAfter = db.ledgerDao().getAllDebts()
        val paymentsAfter = db.ledgerDao().getAllPayments()
        val auditsAfter = db.ledgerDao().getAllAuditEvents()

        assertEquals(personsBefore, personsAfter)
        assertEquals(debtsBefore, debtsAfter)
        assertEquals(paymentsBefore, paymentsAfter)
        assertEquals(auditsBefore, auditsAfter)
        assertEquals(initialGen, repo.currentGeneration)
    }

    @Test
    fun testGenerationBarrierRejectsPendingStaleWritesOnRestoreSuccess() = runBlocking {
        withTimeout(10000) {
            try {
                val pId = repo.savePerson(null, "خالد", "0599001122", null)
                val dId = repo.saveDebt(null, pId, DebtDirection.RECEIVABLE, Currency.ILS, 50000L, 10L, "دين", null)

                val exportedPayload = repo.exportBackupPayload()
                val validFingerprint = repo.getDatabaseFingerprint()

                val writeStarted = CompletableDeferred<Unit>()
                val restoreFinished = CompletableDeferred<Unit>()

                // Configure test barrier inside actual repository write path!
                repo.testOnBeforeAcquireWriteLock = {
                    writeStarted.complete(Unit)
                    // Keep write operation pending until restore completes!
                    restoreFinished.await()
                }

                val pendingWrite = async {
                    runCatching {
                        repo.savePerson(pId, "اسم معدل من نموذج قديم", null, null)
                    }
                }

                // 1. Wait until savePerson has captured startGen = 0 and is paused before write lock
                writeStarted.await()

                // 2. Perform restore FIRST (increments currentGeneration to 1) while write operation remains paused!
                repo.restoreBackupPayload(exportedPayload, validFingerprint)

                val personsAfterRestore = db.ledgerDao().getAllPersons()
                val debtsAfterRestore = db.ledgerDao().getAllDebts()
                val paymentsAfterRestore = db.ledgerDao().getAllPayments()
                val auditsAfterRestore = db.ledgerDao().getAllAuditEvents()

                // 3. ONLY AFTER restore completes successfully, release the write operation!
                restoreFinished.complete(Unit)

                // 4. Wait for pending write result
                val result = pendingWrite.await()
                assertTrue(result.isFailure)
                val exception = result.exceptionOrNull()
                assertTrue(exception is RuleViolation)
                assertEquals(RuleError.STALE_WRITE, (exception as RuleViolation).code)

                // 5. Verify all 4 tables and generation match restored data exactly, unaltered by stale write
                assertEquals(personsAfterRestore, db.ledgerDao().getAllPersons())
                assertEquals(debtsAfterRestore, db.ledgerDao().getAllDebts())
                assertEquals(paymentsAfterRestore, db.ledgerDao().getAllPayments())
                assertEquals(auditsAfterRestore, db.ledgerDao().getAllAuditEvents())
                assertEquals(1L, repo.currentGeneration)
            } finally {
                repo.testOnBeforeAcquireWriteLock = null
            }
        }
    }

    @Test
    fun testGenerationBarrierAllowsWriteOnFailedRestoreWhenPausedBeforeLock() = runBlocking {
        withTimeout(10000) {
            try {
                val pId = repo.savePerson(null, "حسن", null, null)
                val dId = repo.saveDebt(null, pId, DebtDirection.RECEIVABLE, Currency.ILS, 50000L, 10L, "دين", null)

                val exportedPayload = repo.exportBackupPayload()
                val fingerprint = repo.getDatabaseFingerprint()
                val genBefore = repo.currentGeneration

                val personsBefore = db.ledgerDao().getAllPersons()
                val debtsBefore = db.ledgerDao().getAllDebts()
                val paymentsBefore = db.ledgerDao().getAllPayments()
                val auditsBefore = db.ledgerDao().getAllAuditEvents()

                val writeStarted = CompletableDeferred<Unit>()
                val restoreFinished = CompletableDeferred<Unit>()

                repo.testOnBeforeAcquireWriteLock = {
                    writeStarted.complete(Unit)
                    // Keep write operation pending until restore completes or fails!
                    restoreFinished.await()
                }

                val pendingWrite = async {
                    runCatching {
                        repo.savePerson(pId, "حسن المعدل", null, null)
                    }
                }

                // 1. Wait until savePerson has captured startGen = 0 and is paused before write lock
                writeStarted.await()

                // 2. Inject failure AFTER_DELETE and execute restore FIRST
                repo.testRestoreFailurePoint = RestoreTestFailurePoint.AFTER_DELETE
                try {
                    repo.restoreBackupPayload(exportedPayload, fingerprint)
                    fail("Expected injected failure AFTER_DELETE")
                } catch (e: IllegalStateException) {
                    assertEquals("TEST_INJECTED_FAILURE_AFTER_DELETE", e.message)
                }

                // 3. Verify rollback: all 4 tables and generation remain unchanged
                assertEquals(personsBefore, db.ledgerDao().getAllPersons())
                assertEquals(debtsBefore, db.ledgerDao().getAllDebts())
                assertEquals(paymentsBefore, db.ledgerDao().getAllPayments())
                assertEquals(auditsBefore, db.ledgerDao().getAllAuditEvents())
                assertEquals(genBefore, repo.currentGeneration)

                // 4. ONLY AFTER restore fails and rolls back, release the write operation!
                restoreFinished.complete(Unit)

                // 5. Verify write operation succeeds under original generation
                val result = pendingWrite.await()
                assertTrue(result.isSuccess)
                assertEquals(pId, result.getOrNull())
                assertEquals("حسن المعدل", db.ledgerDao().person(pId)?.name)
                assertEquals(genBefore, repo.currentGeneration)
            } finally {
                repo.testRestoreFailurePoint = null
                repo.testOnBeforeAcquireWriteLock = null
            }
        }
    }

    @Test
    fun testFormCapturedGenerationRejectsSavingStaleFormOpenedBeforeRestore() = runBlocking {
        val pId = repo.savePerson(null, "حسن", null, null)
        val dId = repo.saveDebt(null, pId, DebtDirection.RECEIVABLE, Currency.ILS, 50000L, 10L, "دين", null)

        val exportedPayload = repo.exportBackupPayload()
        val fingerprint = repo.getDatabaseFingerprint()

        // Form opened before restore captures formGen = 0
        val formGenBeforeRestore = repo.currentGeneration

        // Perform restore -> increments currentGeneration to 1
        repo.restoreBackupPayload(exportedPayload, fingerprint)

        // Attempting to save form with formGen = 0 fails with STALE_WRITE
        try {
            repo.savePerson(pId, "حسن المعدل من نموذج مفتوح سابقاً", null, null, formGeneration = formGenBeforeRestore)
            fail("Expected STALE_WRITE error when saving form opened before restore")
        } catch (e: RuleViolation) {
            assertEquals(RuleError.STALE_WRITE, e.code)
        }
    }

    @Test
    fun testRestoreCancellationPreservesDatabaseAndGeneration() = runBlocking {
        val pId = repo.savePerson(null, "جمال", "0599222333", null)
        val exportedPayload = repo.exportBackupPayload()
        val fingerprint = repo.getDatabaseFingerprint()
        val genBefore = repo.currentGeneration

        val job = launch {
            coroutineContext.job.cancel()
            repo.restoreBackupPayload(exportedPayload, fingerprint)
        }
        job.join()

        assertEquals(genBefore, repo.currentGeneration)
        val persons = db.ledgerDao().getAllPersons()
        assertEquals(1, persons.size)
        assertEquals("جمال", persons[0].name)
    }

    @Test
    fun testCancellationAfterCommitStillAdvancesGeneration() = runBlocking {
        val barrierReached = CompletableDeferred<Unit>()
        val releaseBarrier = CompletableDeferred<Unit>()

        try {
            withTimeout(10000) {
                val pId = repo.savePerson(null, "بيانات أولية قبل الاستعادة", "0599000000", null)
                val genBefore = repo.currentGeneration

                // Prepare restore payload with DIFFERENT data
                val restorePerson = PersonDto("p_restored", "شخص من النسخة المستعادة", "0599999999", null, null, 2000L, 2000L)
                val restoreDebt = DebtDto("d_restored", "p_restored", DebtDirection.RECEIVABLE.name, Currency.ILS.name, 99000L, 100L, "دين مستعاد", null, null, 2000L, 2000L)
                val restorePayment = PaymentDto("pay_restored", "d_restored", "op_restored", 33000L, 101L, null, null, 2000L, 2000L)
                val restoreAudit = AuditDto("a_restored", "PAYMENT", "pay_restored", "CREATE", null, null, null, 2000L)

                val restorePayload = PlainBackupPayloadV1(
                    metadata = BackupMetadataDto("DebtLedgerBackup", 1, 1, "0.1.0", 2000L, BackupCountsDto(1, 1, 1, 1)),
                    data = BackupDataDto(listOf(restorePerson), listOf(restoreDebt), listOf(restorePayment), listOf(restoreAudit))
                )
                val fingerprint = repo.getDatabaseFingerprint()

                // Inject barrier inside NonCancellable block AFTER Room transaction commit and BEFORE generation increment
                repo.testOnBeforeGenerationIncrementInNonCancellable = {
                    barrierReached.complete(Unit)
                    releaseBarrier.await()
                }

                // Launch restore in a separate job
                val restoreJob = launch {
                    runCatching {
                        repo.restoreBackupPayload(restorePayload, fingerprint)
                    }
                }

                try {
                    // 1. Wait until Room transaction has committed and reached barrier
                    barrierReached.await()

                    // 2. Cancel the restore job while at the barrier
                    restoreJob.cancel()
                } finally {
                    // Release the barrier inside withTimeout so coroutine inside NonCancellable is unblocked before timeout
                    releaseBarrier.complete(Unit)
                }

                restoreJob.join()

                // 4. Verify generation WAS incremented by 1
                assertEquals(genBefore + 1, repo.currentGeneration)

                // 5. Verify all 4 tables match restored data 100%
                assertEquals(listOf(restorePerson.toEntity()), db.ledgerDao().getAllPersons())
                assertEquals(listOf(restoreDebt.toEntity()), db.ledgerDao().getAllDebts())
                assertEquals(listOf(restorePayment.toEntity()), db.ledgerDao().getAllPayments())
                assertEquals(listOf(restoreAudit.toEntity()), db.ledgerDao().getAllAuditEvents())

                // 6. Attempting to save a form with old generation fails with STALE_WRITE
                try {
                    repo.savePerson(pId, "حفظ نموذج قديم", null, null, formGeneration = genBefore)
                    fail("Expected STALE_WRITE error when saving form opened before restore")
                } catch (e: RuleViolation) {
                    assertEquals(RuleError.STALE_WRITE, e.code)
                }

                // 7. Writing with current generation succeeds
                val newPersonId = repo.savePerson(null, "كتابة جديدة بالجيل الجديد", null, null, formGeneration = repo.currentGeneration)
                assertNotNull(newPersonId)
            }
        } finally {
            releaseBarrier.complete(Unit) // Guarantees coroutine unblocks even if timeout or assertion fails!
            repo.testOnBeforeGenerationIncrementInNonCancellable = null
        }
    }
}
