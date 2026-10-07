package com.example.debtledger

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.room.Room
import com.example.debtledger.data.LedgerRepository
import com.example.debtledger.data.local.AppDatabase
import com.example.debtledger.domain.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class RepositoryTest {
    private lateinit var db: AppDatabase
    private lateinit var repo: LedgerRepository

    @Before fun setup() = runBlocking {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java).build()
        repo = LedgerRepository(db, { 1000L }, { 20L })
    }

    @After fun close() { db.close() }

    @Test fun paymentCorrectionAndCancellation() = runBlocking {
        val p = repo.savePerson(null, "أحمد", null, null)
        val d = repo.saveDebt(null, p, DebtDirection.RECEIVABLE, Currency.ILS, 100000, 10, "قرض", null)
        val id = repo.addPayment(UUID.randomUUID().toString(), d, 20000, 11, null)
        repo.editPayment(id, 30000, 12, null, "تصحيح مبلغ")
        Assert.assertEquals(30000L, db.ledgerDao().payments(d).single().amountMinor)
        repo.voidPayment(id, "سجلت بالخطأ")
        Assert.assertNotNull(db.ledgerDao().payments(d).single().voidedAt)
    }

    @Test fun preventsOverpaymentAndDuplicate() = runBlocking {
        val p = repo.savePerson(null, "أحمد", null, null)
        val d = repo.saveDebt(null, p, DebtDirection.RECEIVABLE, Currency.ILS, 100000, 10, "قرض", null)
        val op = UUID.randomUUID().toString()
        val first = repo.addPayment(op, d, 60000, 11, null)
        Assert.assertEquals(first, repo.addPayment(op, d, 60000, 11, null))
        try {
            repo.addPayment(UUID.randomUUID().toString(), d, 40001, 12, null)
            Assert.fail("Overpayment")
        } catch (e: RuleViolation) {
            Assert.assertEquals(RuleError.OVERPAYMENT, e.code)
        }
        Assert.assertEquals(1, db.ledgerDao().payments(d).size)
    }

    @Test fun atomicRejectionSnapshotsAndRuleErrors() = runBlocking {
        val p = repo.savePerson(null, "سعيد", null, null)
        val d = repo.saveDebt(null, p, DebtDirection.RECEIVABLE, Currency.ILS, 100000, 10, "قرض", null)
        val p1 = repo.addPayment(UUID.randomUUID().toString(), d, 40000, 11, null)

        val debtsBefore = db.ledgerDao().debt(d)
        val paymentsBefore = db.ledgerDao().payments(d)
        val auditBefore = db.ledgerDao().observeActivity(100).first().size

        try {
            repo.editPayment(p1, 105000, 11, null, "تعديل يتجاوز الأصل")
            Assert.fail("Expected OVERPAYMENT")
        } catch (e: RuleViolation) {
            Assert.assertEquals(RuleError.OVERPAYMENT, e.code)
        }

        Assert.assertEquals(debtsBefore, db.ledgerDao().debt(d))
        Assert.assertEquals(paymentsBefore, db.ledgerDao().payments(d))
        Assert.assertEquals(auditBefore, db.ledgerDao().observeActivity(100).first().size)

        try {
            repo.saveDebt(d, p, DebtDirection.RECEIVABLE, Currency.ILS, 30000, 10, "تقليل الأصل دون المدفوع", null)
            Assert.fail("Expected BELOW_PAID")
        } catch (e: RuleViolation) {
            Assert.assertEquals(RuleError.BELOW_PAID, e.code)
        }
    }

    @Test fun operationIdIdempotencyAndIndependentNew() = runBlocking {
        val p = repo.savePerson(null, "فاطمة", null, null)
        val d = repo.saveDebt(null, p, DebtDirection.RECEIVABLE, Currency.ILS, 50000, 10, "قرض", null)
        val op = UUID.randomUUID().toString()
        val id1 = repo.addPayment(op, d, 10000, 11, "دفعة أولى")
        val auditCountBefore = db.ledgerDao().observeActivity(100).first().size
        
        val id2 = repo.addPayment(op, d, 10000, 11, "دفعة أولى")
        Assert.assertEquals(id1, id2)
        Assert.assertEquals(1, db.ledgerDao().payments(d).size)
        Assert.assertEquals(auditCountBefore, db.ledgerDao().observeActivity(100).first().size)

        val idNew = repo.addPayment(UUID.randomUUID().toString(), d, 15000, 12, "دفعة ثانية مستقلة")
        Assert.assertNotEquals(id1, idNew)
        Assert.assertEquals(2, db.ledgerDao().payments(d).size)
        Assert.assertEquals(auditCountBefore + 1, db.ledgerDao().observeActivity(100).first().size)
    }

    @Test fun accountingScenarioPayableSequenceStepByStep() = runBlocking {
        val pInd = repo.savePerson(null, "محمود", null, null)
        val payDebt = repo.saveDebt(null, pInd, DebtDirection.PAYABLE, Currency.ILS, 100000, 10, "دين علي", null)

        var dBal = db.ledgerDao().observeDebt(payDebt).first()!!
        Assert.assertEquals(0L, dBal.paidMinor)
        Assert.assertEquals(100000L, dBal.remainingMinor)
        Assert.assertEquals("UNPAID", dBal.status)
        var totals = db.ledgerDao().observeTotals(pInd).first()
        var curTotal = totals.find { it.currency == Currency.ILS }!!
        Assert.assertEquals(0L, curTotal.collectedMinor)
        Assert.assertEquals(0L, curTotal.repaidMinor)

        val p1 = repo.addPayment(UUID.randomUUID().toString(), payDebt, 20000, 11, null)
        dBal = db.ledgerDao().observeDebt(payDebt).first()!!
        Assert.assertEquals(20000L, dBal.paidMinor)
        Assert.assertEquals(80000L, dBal.remainingMinor)
        Assert.assertEquals("PARTIAL", dBal.status)
        totals = db.ledgerDao().observeTotals(pInd).first()
        curTotal = totals.find { it.currency == Currency.ILS }!!
        Assert.assertEquals(0L, curTotal.collectedMinor)
        Assert.assertEquals(20000L, curTotal.repaidMinor)

        val p2 = repo.addPayment(UUID.randomUUID().toString(), payDebt, 30000, 12, null)
        dBal = db.ledgerDao().observeDebt(payDebt).first()!!
        Assert.assertEquals(50000L, dBal.paidMinor)
        Assert.assertEquals(50000L, dBal.remainingMinor)
        Assert.assertEquals("PARTIAL", dBal.status)
        totals = db.ledgerDao().observeTotals(pInd).first()
        curTotal = totals.find { it.currency == Currency.ILS }!!
        Assert.assertEquals(0L, curTotal.collectedMinor)
        Assert.assertEquals(50000L, curTotal.repaidMinor)

        repo.editPayment(p2, 25000, 12, null, "تعديل دفعة")
        dBal = db.ledgerDao().observeDebt(payDebt).first()!!
        Assert.assertEquals(45000L, dBal.paidMinor)
        Assert.assertEquals(55000L, dBal.remainingMinor)
        Assert.assertEquals("PARTIAL", dBal.status)
        totals = db.ledgerDao().observeTotals(pInd).first()
        curTotal = totals.find { it.currency == Currency.ILS }!!
        Assert.assertEquals(0L, curTotal.collectedMinor)
        Assert.assertEquals(45000L, curTotal.repaidMinor)

        repo.voidPayment(p1, "إلغاء خطأ")
        dBal = db.ledgerDao().observeDebt(payDebt).first()!!
        Assert.assertEquals(25000L, dBal.paidMinor)
        Assert.assertEquals(75000L, dBal.remainingMinor)
        Assert.assertEquals("PARTIAL", dBal.status)
        totals = db.ledgerDao().observeTotals(pInd).first()
        curTotal = totals.find { it.currency == Currency.ILS }!!
        Assert.assertEquals(0L, curTotal.collectedMinor)
        Assert.assertEquals(25000L, curTotal.repaidMinor)

        repo.addPayment(UUID.randomUUID().toString(), payDebt, 75000, 15, "سداد كامل")
        dBal = db.ledgerDao().observeDebt(payDebt).first()!!
        Assert.assertEquals(100000L, dBal.paidMinor)
        Assert.assertEquals(0L, dBal.remainingMinor)
        Assert.assertEquals("PAID", dBal.status)
        totals = db.ledgerDao().observeTotals(pInd).first()
        curTotal = totals.find { it.currency == Currency.ILS }!!
        Assert.assertEquals(0L, curTotal.collectedMinor)
        Assert.assertEquals(100000L, curTotal.repaidMinor)
    }

    @Test fun currencyIndependenceScenarioPrecise() = runBlocking {
        val p2 = repo.savePerson(null, "محمد", null, null)
        val d1 = repo.saveDebt(null, p2, DebtDirection.RECEIVABLE, Currency.ILS, 100000, 10, "ILS rec", null)
        repo.addPayment(UUID.randomUUID().toString(), d1, 25000, 11, null)
        val d2 = repo.saveDebt(null, p2, DebtDirection.PAYABLE, Currency.ILS, 40000, 10, "ILS pay", null)
        repo.addPayment(UUID.randomUUID().toString(), d2, 10000, 11, null)

        val d3 = repo.saveDebt(null, p2, DebtDirection.RECEIVABLE, Currency.USD, 10000, 10, "USD rec", null)
        repo.addPayment(UUID.randomUUID().toString(), d3, 2500, 11, null)
        val d4 = repo.saveDebt(null, p2, DebtDirection.PAYABLE, Currency.USD, 8000, 10, "USD pay", null)
        repo.addPayment(UUID.randomUUID().toString(), d4, 2000, 11, null)

        val totals = db.ledgerDao().observeTotals(p2).first()
        val ils = totals.find { it.currency == Currency.ILS }!!
        Assert.assertEquals(75000L, ils.receivableMinor)
        Assert.assertEquals(30000L, ils.payableMinor)
        Assert.assertEquals(25000L, ils.collectedMinor)
        Assert.assertEquals(10000L, ils.repaidMinor)
        Assert.assertEquals(45000L, ils.receivableMinor - ils.payableMinor)

        val usd = totals.find { it.currency == Currency.USD }!!
        Assert.assertEquals(7500L, usd.receivableMinor)
        Assert.assertEquals(6000L, usd.payableMinor)
        Assert.assertEquals(2500L, usd.collectedMinor)
        Assert.assertEquals(2000L, usd.repaidMinor)
        Assert.assertEquals(1500L, usd.receivableMinor - usd.payableMinor)
    }
}
