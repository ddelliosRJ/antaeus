package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.models.Invoice
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

class BillingService(
    private val paymentProvider: PaymentProvider,
    private val invoiceService: InvoiceService,
    private val customerService: CustomerService
) {

    suspend fun charge(): String {
        val pendingInvoices = invoiceService.fetchByStatus("PENDING")
        pendingInvoices.forEach { invoice ->
            try {
                logger.info { "Will now try to charge invoice with id [${invoice.id}]" }
                val completedInvoice = chargeInvoice(invoice)
                if (completedInvoice.status.equals("PENDING")) return "Charging failed"

            } catch (e: Exception) {
                logger.error(e) { "Unexpected Error: Invoice Charge" }
            }
        }
        return "Everything went well"
    }

    private suspend fun chargeInvoice(invoice: Invoice): Invoice {
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
            paymentProvider.charge(existingInvoice)
        }
    }
}