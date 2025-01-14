package io.pleo.antaeus.core.services

import io.mockk.coEvery
import io.mockk.mockk
import io.pleo.antaeus.core.exceptions.InvoiceNotFoundException
import io.pleo.antaeus.data.AntaeusDal
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class InvoiceServiceTest {
    private val dal = mockk<AntaeusDal> {
        coEvery { fetchInvoice(404) } returns null
        coEvery { fetchInvoices() } returns (0..9).map { createInvoice() }

    }

    private val invoiceService = InvoiceService(dal = dal)

    @Test
    fun `will throw if invoice is not found`() = runBlockingTest {
        assertThrows<InvoiceNotFoundException> {
            invoiceService.fetch(404)
        }
    }

    // Add success testing
    @Test
    fun `will return all invoices`() = runBlockingTest {
        Assertions.assertEquals(invoiceService.fetchAll().size, 10)
    }
}