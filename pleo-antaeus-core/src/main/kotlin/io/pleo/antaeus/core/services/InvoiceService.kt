/*
    Implements endpoints related to invoices.
 */

package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.InvoiceChargedException
import io.pleo.antaeus.core.exceptions.InvoiceNotFoundException
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import mu.KotlinLogging
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.experimental.suspendedTransaction
import java.util.*

class InvoiceService(private val dal: AntaeusDal) {

    private val logger = KotlinLogging.logger {}

    suspend fun fetchAll(): List<Invoice> {
        return dal.fetchInvoices()
    }

    suspend fun fetch(id: Int): Invoice {
        return dal.fetchInvoice(id) ?: throw InvoiceNotFoundException(id)
    }

    suspend fun fetchByStatus(status: String): List<Invoice> {
        return dal.fetchInvoicesByStatus(status)
    }

    suspend fun chargeInvoice(invoiceId: Int, tryProviderCharge: suspend (invoice: Invoice) -> Boolean): String {
        // Initialize payment status
        var paymentStatus = ""
        dal.transaction {
            addLogger(StdOutSqlLogger)
            // Fetch invoice from db - this can be removed but keep for method usability
            val invoice = fetch(invoiceId)
            // Add check to prevent double charging a paid invoice
            // (since we are only dealing with PENDING, this is just for sanity check and future method extension)
            if (invoice.status == InvoiceStatus.PAID) {
                throw InvoiceChargedException(invoiceId)
            }
            // Create payment in payment table
            val invoicePaymentId = dal.createPayment(invoice.amount, invoice, false, Date())
            logger.info { "Created pending payment: $invoicePaymentId" }
            // Get random boolean from payment provider to simulate possible errors
            val success = tryProviderCharge(invoice)
            // Update status in both tables
            if (success) {
                suspendedTransaction {
                    logger.info { "Will update invoice status in Invoice and Payment table" }
                    dal.updatePaymentStatus(invoicePaymentId, chargeSuccess = true, chargeDate = Date())
                    dal.updateInvoiceStatus(invoice.id, status = InvoiceStatus.PAID)
                    paymentStatus = "Payment successful for invoice [${invoice.id}], at ${Date()}"
                }
            } else {
                logger.error { "Charge action could not be completed" }
                paymentStatus = "Payment failed for invoice [${invoice.id}]"
            }
        }
        return paymentStatus
    }
}