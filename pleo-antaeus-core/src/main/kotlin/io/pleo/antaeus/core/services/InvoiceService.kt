/*
    Implements endpoints related to invoices.
 */

package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.InvoiceNotFoundException
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import java.util.*

class InvoiceService(private val dal: AntaeusDal) {
    suspend fun fetchAll(): List<Invoice> {
        return dal.fetchInvoices()
    }

    suspend fun fetch(id: Int): Invoice {
        return dal.fetchInvoice(id) ?: throw InvoiceNotFoundException(id)
    }

    suspend fun chargeInvoice(invoiceId: Int, chargeAction: (invoice: Invoice) -> Boolean): Invoice {
        // get invoice from Invoice Table
        val invoice = fetch(invoiceId)
        // create pending payment in Payments table - success is initially false
        val paymentId = dal.createPayment(invoice.amount, invoice, false, Date())
        // try to charge the invoice and return the invoice id. Change invoice status to paid.
        return dal.transaction {
            val success = chargeAction(invoice); // check if invoice was fetched successfully
            if (success) {
                dal.updatePaymentStatus(paymentId, true, Date())
                dal.updateInvoiceStatus(invoice.id, InvoiceStatus.PAID)
            }
            fetch(invoice.id)
        }
    }
}
