package com.example.debtledger.domain

import java.time.DateTimeException
import java.time.LocalDate

data class DebtAmounts(val originalMinor: Long, val paidMinor: Long) {
    init {
        demand(originalMinor > 0 && paidMinor >= 0 && paidMinor <= originalMinor,
            RuleError.INCONSISTENT_BALANCE)
    }
    val remainingMinor: Long get() = originalMinor - paidMinor
    val status: DebtStatus get() = when {
        paidMinor == 0L -> DebtStatus.UNPAID
        remainingMinor == 0L -> DebtStatus.PAID
        else -> DebtStatus.PARTIAL
    }
}
data class BalanceInput(val currency: Currency, val direction: DebtDirection,
    val amounts: DebtAmounts, val cancelled: Boolean = false)
data class CurrencyBalance(val currency: Currency, val receivableMinor: Long,
    val payableMinor: Long, val collectedMinor: Long, val repaidMinor: Long) {
    // Both operands are nonnegative Long, so their difference cannot overflow.
    val netMinor: Long get() = receivableMinor - payableMinor
}
object LedgerRules {
    fun positive(amount: Long) = demand(amount > 0, RuleError.NON_POSITIVE_AMOUNT)
    fun date(day: Long, today: Long) {
        try { LocalDate.ofEpochDay(day); LocalDate.ofEpochDay(today) }
        catch (_: DateTimeException) { throw RuleViolation(RuleError.INVALID_DATE) }
        demand(day <= today, RuleError.FUTURE_DATE)
    }
    fun payment(original: Long, otherPaid: Long, amount: Long,
        paymentDay: Long, debtDay: Long, today: Long) {
        positive(amount); date(paymentDay,today)
        demand(paymentDay >= debtDay, RuleError.BEFORE_DEBT_DATE)
        val balance = DebtAmounts(original,otherPaid)
        demand(amount <= balance.remainingMinor, RuleError.OVERPAYMENT)
    }
    fun debtEdit(amount: Long, paid: Long, day: Long,
        earliestActivePayment: Long?, today: Long) {
        positive(amount); date(day,today)
        demand(paid >= 0,RuleError.INCONSISTENT_BALANCE)
        demand(amount >= paid,RuleError.BELOW_PAID)
        demand(earliestActivePayment == null || day <= earliestActivePayment,
            RuleError.DEBT_DATE_AFTER_PAYMENT)
    }
    fun aggregate(currency: Currency, debts: List<BalanceInput>): CurrencyBalance {
        var receivable=0L; var payable=0L; var collected=0L; var repaid=0L
        debts.filter { it.currency == currency && !it.cancelled }.forEach {
            if(it.direction == DebtDirection.RECEIVABLE) {
                receivable=exactAdd(receivable,it.amounts.remainingMinor)
                collected=exactAdd(collected,it.amounts.paidMinor)
            } else {
                payable=exactAdd(payable,it.amounts.remainingMinor)
                repaid=exactAdd(repaid,it.amounts.paidMinor)
            }
        }
        return CurrencyBalance(currency,receivable,payable,collected,repaid)
    }
}
