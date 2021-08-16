/*
    Implements endpoints related to payments.
 */
package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.PaymentNotFoundException
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.Payment

/**
 * @author Dimitris Dellios
 */
class PaymentService(private val dal: AntaeusDal) {
    suspend fun fetchAll(): List<Payment> {
        return dal.fetchPayments()
    }

    suspend fun fetch(id: Int): Payment {
        return dal.fetchPayment(id) ?: throw PaymentNotFoundException(id)
    }
}