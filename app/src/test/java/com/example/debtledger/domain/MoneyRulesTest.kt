package com.example.debtledger.domain

import org.junit.Assert.*
import org.junit.Test

class MoneyRulesTest {
    private fun fails(code: RuleError, block: () -> Unit) {
        try { block(); fail("Expected $code") }
        catch(e: RuleViolation) { assertEquals(code,e.code) }
    }
    @Test fun readsArabicEnglishAndPersianDigits() {
        listOf("1000.50","١٠٠٠٫٥٠","۱۰۰۰,۵۰").forEach {
            assertEquals(100050L,MoneyParser.positive(it,Currency.ILS).amountMinor)
        }
        assertEquals("0.01",MoneyParser.positive("0.01",Currency.USD).decimalText())
    }
    @Test fun rejectsPrecisionAndMalformedInput() {
        fails(RuleError.AMOUNT_PRECISION) { MoneyParser.positive("1.001",Currency.ILS) }
        listOf("1,000.50","1e3","-1","+1","1 000",".5","1.","NaN","").forEach {
            fails(RuleError.INVALID_AMOUNT) { MoneyParser.positive(it,Currency.ILS) }
        }
        fails(RuleError.NON_POSITIVE_AMOUNT) { MoneyParser.positive("0",Currency.ILS) }
    }
    @Test fun boundsAndOverflow() {
        assertEquals(Long.MAX_VALUE,MoneyParser.positive("92233720368547758.07",Currency.ILS).amountMinor)
        fails(RuleError.AMOUNT_OUT_OF_RANGE) { MoneyParser.positive("92233720368547758.08",Currency.ILS) }
        fails(RuleError.AMOUNT_OUT_OF_RANGE) { Money(Long.MAX_VALUE,Currency.ILS)+Money(1,Currency.ILS) }
        fails(RuleError.CURRENCY_MISMATCH) { Money(1,Currency.ILS)+Money(1,Currency.USD) }
    }
    @Test fun statuses() {
        assertEquals(DebtStatus.UNPAID,DebtAmounts(100000,0).status)
        assertEquals(50000L,DebtAmounts(100000,50000).remainingMinor)
        assertEquals(DebtStatus.PARTIAL,DebtAmounts(100000,50000).status)
        assertEquals(DebtStatus.PAID,DebtAmounts(100000,100000).status)
    }
    @Test fun validatesPaymentAndEdit() {
        LedgerRules.payment(100000,50000,50000,15,10,20)
        fails(RuleError.OVERPAYMENT) { LedgerRules.payment(100000,50000,50001,15,10,20) }
        fails(RuleError.BEFORE_DEBT_DATE) { LedgerRules.payment(100000,0,100,9,10,20) }
        fails(RuleError.FUTURE_DATE) { LedgerRules.payment(100000,0,100,21,10,20) }
        fails(RuleError.BELOW_PAID) { LedgerRules.debtEdit(49999,50000,10,15,20) }
        fails(RuleError.DEBT_DATE_AFTER_PAYMENT) { LedgerRules.debtEdit(100000,50000,16,15,20) }
    }
    @Test fun separatesCurrenciesAndCancelledDebts() {
        val debts=listOf(
            BalanceInput(Currency.ILS,DebtDirection.RECEIVABLE,DebtAmounts(100000,50000)),
            BalanceInput(Currency.ILS,DebtDirection.PAYABLE,DebtAmounts(15000,0)),
            BalanceInput(Currency.USD,DebtDirection.RECEIVABLE,DebtAmounts(20000,5000)),
            BalanceInput(Currency.ILS,DebtDirection.RECEIVABLE,DebtAmounts(90000,0),true))
        assertEquals(35000L,LedgerRules.aggregate(Currency.ILS,debts).netMinor)
        assertEquals(15000L,LedgerRules.aggregate(Currency.USD,debts).netMinor)
        assertEquals(0L,LedgerRules.aggregate(Currency.USD,emptyList()).netMinor)
    }
}
