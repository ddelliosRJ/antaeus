package io.pleo.antaeus.models

import java.util.*

/**
 * @author Dimitris Dellios
 */
data class Payment(
    val id: Int,
    val invoiceId: Int,
    val amount: Money,
    val chargeDate: Date,
    val chargeSuccess: Boolean
)