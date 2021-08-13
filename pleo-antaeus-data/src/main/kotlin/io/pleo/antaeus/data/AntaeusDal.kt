/*
    Implements the data access layer (DAL).
    The data access layer generates and executes requests to the database.

    See the `mappings` module for the conversions between database rows and Kotlin objects.
 */

package io.pleo.antaeus.data

import io.pleo.antaeus.models.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import mu.KotlinLogging
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.joda.time.DateTime
import java.util.Date

class AntaeusDal(private val db: Database) {
    // Add logger
    private val logger = KotlinLogging.logger {}

    // Implement suspended transaction for sql data retrieval, advised coroutines for method structure
    suspend fun <T> transaction(
        context: CoroutineDispatcher = Dispatchers.Default,
        action: suspend Transaction.() -> T
    ): T {
        return newSuspendedTransaction(context, db, action)
    }

    suspend fun fetchInvoice(id: Int): Invoice? {
        // transaction runs the internal query as a new database transaction.
        return transaction(Dispatchers.IO) {
            // Returns the first invoice with matching id.
            InvoiceTable
                .select { InvoiceTable.id.eq(id) }
                .firstOrNull()
                ?.toInvoice()
        }
    }

    suspend fun fetchInvoices(): List<Invoice> {
        return transaction(Dispatchers.IO) {
            InvoiceTable
                .selectAll()
                .map { it.toInvoice() }
        }
    }

    suspend fun createInvoice(
        amount: Money,
        customer: Customer,
        status: InvoiceStatus = InvoiceStatus.PENDING
    ): Invoice? {
        val id = transaction {
            addLogger(StdOutSqlLogger)
            // TODO:Maybe logging here is too much noise because of the amount of invoices?
            logger.info { "Creating new invoice for customer: ${customer.id} with status [$status]" }
            // Insert the invoice and returns its new id.
            InvoiceTable
                .insert {
                    it[this.value] = amount.value
                    it[this.currency] = amount.currency.toString()
                    it[this.status] = status.toString()
                    it[this.customerId] = customer.id
                } get InvoiceTable.id
        }

        return fetchInvoice(id)
    }

    suspend fun updateInvoiceStatus(id: Int, status: InvoiceStatus): Invoice? {
        val invoiceId = transaction {
            // Update invoice status
            InvoiceTable
                .update({ InvoiceTable.id eq id }) {
                    it[this.status] = status.toString()
                }
        }

        return fetchInvoice(invoiceId)
    }

    suspend fun fetchCustomer(id: Int): Customer? {
        return transaction(Dispatchers.IO) {
            CustomerTable
                .select { CustomerTable.id.eq(id) }
                .firstOrNull()
                ?.toCustomer()
        }
    }

    suspend fun fetchCustomers(): List<Customer> {
        return transaction(Dispatchers.IO) {
            CustomerTable
                .selectAll()
                .map { it.toCustomer() }
        }
    }

    suspend fun createCustomer(currency: Currency): Customer? {
        val id = transaction {
            addLogger(StdOutSqlLogger)
            logger.info { "Creating new customer with currency: [$currency]" }
            // Insert the customer and return its new id.
            CustomerTable.insert {
                it[this.currency] = currency.toString()
            } get CustomerTable.id
        }

        return fetchCustomer(id)
    }

    suspend fun fetchPayments(invoiceId: Int): List<Payment> {
        return transaction(Dispatchers.IO) {
            PaymentTable
                .select { PaymentTable.invoiceId.eq(invoiceId) }
                .map { it.toPayment() }
        }
    }

    suspend fun fetchPayment(id: Int): Payment? {
        return transaction(Dispatchers.IO) {
            PaymentTable
                .select { PaymentTable.id eq id }
                .firstOrNull()
                ?.toPayment()
        }
    }

    suspend fun createPayment(
        amount: Money,
        invoice: Invoice,
        chargeSuccess: Boolean = false,
        chargeDate: Date = Date()
    ): Int {
        return transaction {
            addLogger(StdOutSqlLogger)
            logger.info { "Creating new payment entry for invoice: ${invoice.id}" }

            // Insert the invoice into the PaymentTable and return its new id.
            PaymentTable
                .insert {
                    it[value] = amount.value
                    it[currency] = amount.currency.toString()
                    it[this.chargeDate] = DateTime(chargeDate)
                    it[this.chargeSuccess] = chargeSuccess
                    it[invoiceId] = invoice.id
                } get PaymentTable.id
        }
    }

    fun updatePaymentStatus(id: Int, chargeSuccess: Boolean, chargeDate: Date): Int {
        return PaymentTable
            .update({ PaymentTable.id eq id }) {
                it[this.chargeSuccess] = chargeSuccess
                it[this.chargeDate] = DateTime(chargeDate)
            }
    }

}
