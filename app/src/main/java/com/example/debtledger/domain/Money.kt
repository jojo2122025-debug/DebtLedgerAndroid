package com.example.debtledger.domain

import java.math.BigDecimal
import java.math.RoundingMode

enum class Currency { ILS, USD }
enum class DebtDirection { RECEIVABLE, PAYABLE }
enum class DebtStatus { UNPAID, PARTIAL, PAID }
enum class RuleError {
    INVALID_AMOUNT, AMOUNT_PRECISION, AMOUNT_OUT_OF_RANGE, NON_POSITIVE_AMOUNT,
    OVERPAYMENT, BELOW_PAID, INVALID_DATE, FUTURE_DATE, BEFORE_DEBT_DATE,
    DEBT_DATE_AFTER_PAYMENT, CURRENCY_MISMATCH, INCONSISTENT_BALANCE, STALE_WRITE
}
class RuleViolation(val code: RuleError) : IllegalArgumentException(code.name)
internal fun demand(condition: Boolean, error: RuleError) {
    if (!condition) throw RuleViolation(error)
}
/** Nonnegative stored amount. Signed net balances are represented separately. */
data class Money(val amountMinor: Long, val currency: Currency) {
    init { demand(amountMinor >= 0, RuleError.INVALID_AMOUNT) }
    operator fun plus(other: Money): Money {
        demand(currency == other.currency, RuleError.CURRENCY_MISMATCH)
        return Money(exactAdd(amountMinor, other.amountMinor), currency)
    }
    fun decimalText(): String = BigDecimal.valueOf(amountMinor, 2).toPlainString()
}
internal fun exactAdd(a: Long, b: Long): Long = try {
    Math.addExact(a,b)
} catch (_: ArithmeticException) { throw RuleViolation(RuleError.AMOUNT_OUT_OF_RANGE) }

object MoneyParser {
    /** No thousands separators, exponents, signs, or silent rounding. */
    fun positive(text: String, currency: Currency): Money {
        demand(text.length <= 128, RuleError.INVALID_AMOUNT)
        val normalized = text.trim().map { ch -> when(ch) {
            in '٠'..'٩' -> ('0'.code + (ch.code - '٠'.code)).toChar()
            in '۰'..'۹' -> ('0'.code + (ch.code - '۰'.code)).toChar()
            ',' , '٫' -> '.'
            else -> ch
        } }.joinToString("")
        demand(Regex("[0-9]+(?:\\.[0-9]+)?").matches(normalized), RuleError.INVALID_AMOUNT)
        val fraction = normalized.substringAfter('.', "")
        demand(fraction.length <= 2, RuleError.AMOUNT_PRECISION)
        // Avoid parsing arbitrarily large user input, while allowing leading zeros.
        val integer = normalized.substringBefore('.').trimStart('0')
        demand(integer.length <= 17, RuleError.AMOUNT_OUT_OF_RANGE)
        val amount = try {
            BigDecimal(normalized).setScale(2, RoundingMode.UNNECESSARY)
                .movePointRight(2).longValueExact()
        } catch (_: ArithmeticException) { throw RuleViolation(RuleError.AMOUNT_OUT_OF_RANGE) }
        demand(amount > 0, RuleError.NON_POSITIVE_AMOUNT)
        return Money(amount,currency)
    }
}
