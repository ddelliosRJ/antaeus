package io.pleo.antaeus.core.services

import io.mockk.coEvery
import io.mockk.mockk
import io.pleo.antaeus.core.exceptions.PaymentNotFoundException
import io.pleo.antaeus.data.AntaeusDal
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * @author Dimitris Dellios
 */
class PaymentServiceTest {
    private val dal = mockk<AntaeusDal> {
        coEvery { fetchPayment(404) } returns null
        coEvery { fetchPayments() } returns (0..4).map { createPayment() }

    }

    private val paymentService = PaymentService(dal = dal)

    @Test
    fun `will throw if payment is not found`() = runBlockingTest {
        assertThrows<PaymentNotFoundException> {
            paymentService.fetch(404)
        }
    }

    // Add success testing
    @Test
    fun `will return all payments`() = runBlockingTest {
        Assertions.assertEquals(paymentService.fetchAll().size, 5)
    }

}