package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.models.Invoice
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

class BillingService(
    private val paymentProvider: PaymentProvider,
    private val invoiceService: InvoiceService,
    private val customerService: CustomerService
) {

    suspend fun chargePendingInvoices(): List<String> {
        // Initialize invoice payment status list
        val paymentStatusList: ArrayList<String> = arrayListOf()
        // Fetch pending invoices from invoice table
        val pendingInvoices = invoiceService.fetchByStatus("PENDING")
        // Try charge for every invoice
        pendingInvoices.forEach { invoice ->
            try {
                logger.info { "Will now try to charge invoice with id [${invoice.id}]" }
                paymentStatusList.add(invoicePaymentProcessor(invoice))
            } catch (e: Exception) {
                logger.error(e) { "Unexpected Error: Invoice Charge" }
            }
        }
        return paymentStatusList
    }

    suspend fun invoicePaymentProcessor(invoice: Invoice): String {
        logger.info { "Begin charging invoice [${invoice.id}] for ${invoice.amount.value} ${invoice.amount.currency}" }
        val customer = customerService.fetch(invoice.customerId)
        // Check if customer currency is different than the invoice
        if (customer.currency != invoice.amount.currency) {
            throw CurrencyMismatchException(
                invoiceId = invoice.id,
                customerId = invoice.customerId
            )
        }
        return invoiceService.chargeInvoice(invoice.id) { existingInvoice ->
            retry(2, false) {
                paymentProvider.charge(existingInvoice)
            }
        }
    }
}

// Add retry method to try again on payment provider failure
suspend fun <T> retry(
    times: Int,
    failureValue: T,
    delayMs: Long = 1000,
    block: suspend () -> T
) = coroutineScope rt@{

    (1..times).fold(failureValue) { _, retryNum ->
        try {
            return@rt block()
        } catch (e: NetworkException) {
            logger.error(e) { "Trying again... $retryNum" }
            delay(delayMs)
            return@fold if (retryNum == times) throw e else failureValue
        }
    }
}