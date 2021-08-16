package io.pleo.antaeus.core.exceptions

class PaymentNotFoundException(id: Int) : EntityNotFoundException("Payment", id)