package io.pleo.antaeus.core.services

import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.spyk
import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.exceptions.InvoiceChargedException
import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.data.*
import io.pleo.antaeus.models.*
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
import org.junit.jupiter.api.assertThrows
import java.io.File
import java.sql.Connection

/**
 * @author Dimitris Dellios
 */
val notRegisteredCustomer = createInvoice(
    id = 1,
    customerId = 1,
    status = InvoiceStatus.PENDING)
val currencyMismatch = createInvoice(
    id = 11,
    customerId = 2,
    status = InvoiceStatus.PENDING,
    amount = Money((10).toBigDecimal(), Currency.GBP))
val networkException = createInvoice(
    id = 21,
    customerId = 3,
    status = InvoiceStatus.PENDING)
val charged = createInvoice(
    id = 31,
    customerId = 4,
    status = InvoiceStatus.PENDING)
val notCharged = createInvoice(
    id = 41,
    customerId = 5,
    status = InvoiceStatus.PENDING)
val alreadyCharged = createInvoice(
    id = 32,
    customerId = 4,
    status = InvoiceStatus.PAID)


class BillingServiceTest {

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

    private fun createBillingService(
        invoice: Invoice,
        chargeInvoiceMock: PaymentProvider.(invoice: Invoice) -> Unit = {
            coEvery { charge(it) } returns false
        },
        fetchInvoiceMock: InvoiceService.(invoice: Invoice) -> Unit = {
            coEvery { fetch(it.id) } returns it
        },
        fetchCustomerMock: CustomerService.(invoice: Invoice) -> Unit = {
            coEvery { fetch(it.customerId) } returns Customer(it.customerId, it.amount.currency)
        }
    ): BillingService {
        val paymentProvider = mockk<PaymentProvider> { chargeInvoiceMock(invoice) }
        val invoiceService = spyk(InvoiceService(dal)) { fetchInvoiceMock(invoice) }
        val customerService = mockk<CustomerService> { fetchCustomerMock(invoice) }

        return BillingService(paymentProvider, invoiceService, customerService)
    }

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
    internal fun `charging failure for non existing customer`() = runBlocking {
        val billingService = createBillingService(
            invoice = notRegisteredCustomer,
            chargeInvoiceMock = { invoice ->
                coEvery { charge(invoice) } throws CustomerNotFoundException(invoice.customerId)
            }
        )
        assertThrows<CustomerNotFoundException> {
            billingService.invoicePaymentProcessor(notRegisteredCustomer)
        }
        assertChargingEffort(notRegisteredCustomer, paymentsNumber = 1, paymentsSuccess = false)
    }

    @Test
    internal fun `charging failure due to currency mismatch`() = runBlocking {
        val billingService = createBillingService(
            invoice = currencyMismatch,
            fetchCustomerMock = {
                coEvery { fetch(it.customerId) } returns Customer(it.customerId, Currency.EUR)
            }
        )
        assertThrows<CurrencyMismatchException> {
            billingService.invoicePaymentProcessor(currencyMismatch)
        }
        assertChargingEffort(currencyMismatch, paymentsNumber = 0, paymentsSuccess = false)
    }

    // TODO: maybe add check for currency mismatch on payment provider side?

    @Test
    internal fun `charging failure when network exception occurs`() = runBlocking {
        val billingService = createBillingService(
            invoice = networkException,
            chargeInvoiceMock = {
                coEvery { charge(it) } throws NetworkException()
            }
        )
        assertThrows<NetworkException> {
            billingService.invoicePaymentProcessor(networkException)
        }
        assertChargingEffort(networkException, paymentsNumber = 1, paymentsSuccess = false)
    }

    @Test
    internal fun `charging failure when insufficient customer funds`() = runBlocking {
        val billingService = createBillingService(invoice = notCharged)
        billingService.invoicePaymentProcessor(notCharged)
        assertChargingEffort(notCharged, paymentsNumber = 1, paymentsSuccess = false)
    }

    @Test
    internal fun `charging success`() = runBlocking {
        val billingService = createBillingService(
            invoice = charged,
            chargeInvoiceMock = {
                coEvery { charge(it) } returns true
            }
        )
        billingService.invoicePaymentProcessor(charged)
        assertChargingEffort(charged, paymentsNumber = 1, paymentsSuccess = true)
    }

    @Test
    internal fun `charging failure when invoice already charged`() = runBlocking<Unit> {
        val billingService = createBillingService(
            invoice = alreadyCharged,
            chargeInvoiceMock = {
                coEvery { charge(it) } returns true
            }
        )

        assertThrows<InvoiceChargedException> {
            billingService.invoicePaymentProcessor(alreadyCharged)
        }
    }

    // --------------------------------------HELPERS FOR COMPLETED PAYMENT ASSERTIONS ---------------------------------------------
    private suspend fun assertChargingEffort(invoice: Invoice, paymentsNumber: Int, paymentsSuccess: Boolean) {
        val payments = dal.fetchPaymentByInvoiceId(invoice.id)
        Assertions.assertEquals(paymentsNumber, payments.size)
        payments.firstOrNull()?.let {
            Assertions.assertEquals(paymentsSuccess, it.chargeSuccess)
        }
    }
}