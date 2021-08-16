package io.pleo.antaeus.data

import io.pleo.antaeus.models.Customer
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import java.sql.Connection

/**
 * @author Dimitris Dellios
 */
class AntaeusDalTest {
    // Create tables for the test
    private val tables = arrayOf(InvoiceTable, CustomerTable, PaymentTable)

    // Connect to test database
    private val dbFile: File = File.createTempFile("test-db", ".sqlite")

    private val db = Database
        .connect(
            url = "jdbc:sqlite:${dbFile.absolutePath}",
            driver = "org.sqlite.JDBC",
            user = "root",
            password = ""
        )

    private val dal = AntaeusDal(db = db)

    @BeforeEach
    fun before() {
        TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE
        transaction(db) {
            addLogger(StdOutSqlLogger)
            // Drop all existing tables to ensure a clean slate on each run
            SchemaUtils.drop(*tables)
            // Create all tables
            SchemaUtils.create(*tables)
            runBlocking {
                setupInitialData(dal = dal)
            }
        }
    }

    @Test
    internal fun `get all invoices`() = runBlocking {
        val invoices = dal.fetchInvoices()
        Assertions.assertEquals(1000, invoices.size)
    }

    @Test
    internal fun `create new invoice`() = runBlocking {
        val newInvoice = createInvoice()
        val customer = Customer(newInvoice.customerId, newInvoice.amount.currency)

        val invoice = dal.createInvoice(amount = newInvoice.amount, customer = customer)
        Assertions.assertEquals(1001, invoice?.id)
    }
}