package io.pleo.antaeus.core.services

import io.pleo.antaeus.models.*
import io.pleo.antaeus.models.Currency
import java.math.BigDecimal
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random

/**
 * @author Dimitris Dellios
 */
//------------------------CREATION HELPERS-----------------------------------------
var idCounter = AtomicInteger(1)

fun nextId() = idCounter.getAndIncrement()
fun randomCurrency() = Currency.values()[Random.nextInt(0, Currency.values().size)]
fun randomAmount() = BigDecimal(Random.nextDouble(10.0, 500.0))
fun randomStatus() = InvoiceStatus.values()[Random.nextInt(0, InvoiceStatus.values().size)]

fun createInvoice(
    id: Int = nextId(),
    customerId: Int = 1,
    amount: Money = Money(
        value = randomAmount(),
        currency = randomCurrency()
    ),
    status: InvoiceStatus = randomStatus()
) = Invoice(id, customerId, amount, status)

fun createCustomer(
    id: Int = nextId(),
    currency: Currency = randomCurrency()
) = Customer(id, currency)

fun createPayment(
    id: Int = nextId(),
    amount: Money = Money(
        value = randomAmount(),
        currency = randomCurrency()
    ),
    invoiceId: Int = 1,
    chargeSuccess: Boolean = true,
    chargeDate: Date = Date()
) = Payment(id, invoiceId, amount, chargeDate, chargeSuccess)