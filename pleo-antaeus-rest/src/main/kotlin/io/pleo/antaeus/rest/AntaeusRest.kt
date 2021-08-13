/*
    Configures the rest app along with basic exception handling and URL endpoints.
 */

package io.pleo.antaeus.rest

import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.get
import io.javalin.apibuilder.ApiBuilder.path
import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.exceptions.EntityNotFoundException
import io.pleo.antaeus.core.exceptions.InvoiceNotFoundException
import io.pleo.antaeus.core.services.BillingService
import io.pleo.antaeus.core.services.CustomerService
import io.pleo.antaeus.core.services.InvoiceService
import kotlinx.coroutines.runBlocking

class AntaeusRest(

    private val invoiceService: InvoiceService,
    private val customerService: CustomerService,
    private val billingService: BillingService

) : Runnable {

    override fun run() {
        app.start(7000)
    }

    // Set up Javalin rest app
    private val app = Javalin
        .create()
        .apply {
            // Handle all possible exceptions with the appropriate status codes
            // EntityNotFoundException: return 404 HTTP status code
            exception(EntityNotFoundException::class.java) { _, ctx ->
                ctx.status(404)
            }
            // CustomerNotFoundException: return 404 HTTP status code
            exception(CustomerNotFoundException::class.java) { _, ctx ->
                ctx.status(404)
            }
            // InvoiceNotFoundException: return 404 HTTP status code
            exception(InvoiceNotFoundException::class.java) { _, ctx ->
                ctx.status(404)
            }
            // InvoiceNotFoundException: return 400 HTTP status code
            exception(CurrencyMismatchException::class.java) { _, ctx ->
                ctx.status(400)
            }
            // Unexpected exception: return HTTP 500
            exception(Exception::class.java) { _, ctx ->
                ctx.status(500)
            }
            // On 404: return message
            error(404) { ctx -> ctx.json("Not Found") }
            // On 400: return message
            error(400) { ctx -> ctx.json("Bad Request: possible currency mismatch") }
            // On 404: return message
            error(500) { ctx -> ctx.json("Internal Server Error") }
        }

    init {
        // Set up URL endpoints for the rest app
        app.routes {
            get("/") {
                it.result("Welcome to Antaeus! see AntaeusRest class for routes")
            }
            path("rest") {
                // Route to check whether the app is running
                // URL: /rest/health
                get("health") {
                    it.json("ok")
                }

                // V1
                path("v1") {
                    path("invoices") {
                        // URL: /rest/v1/invoices
                        get {
                            runBlocking {
                                it.json(invoiceService.fetchAll())
                            }
                        }

                        // URL: /rest/v1/invoices/{:id}
                        get(":id") {
                            runBlocking {
                                it.json(invoiceService.fetch(it.pathParam("id").toInt()))
                            }
                        }
                    }

                    path("customers") {
                        // URL: /rest/v1/customers
                        get {
                            runBlocking {
                                it.json(customerService.fetchAll())
                            }
                        }

                        // URL: /rest/v1/customers/{:id}
                        get(":id") {
                            runBlocking {
                                it.json(customerService.fetch(it.pathParam("id").toInt()))
                            }
                        }
                    }
                    // expose status path to be able to fetch PENDING invoices
                    path("status") {
                        // URL: /rest/v1/status/{:status}
                        get(":status") {
                            runBlocking {
                                it.json(invoiceService.fetchByStatus(it.pathParam("status")))
                            }
                        }
                    }

                    // expose status path to be able to fetch PENDING invoices
                    path("billing") {
                        // URL: /rest/v1/billing
                        get {
                            runBlocking {
                                it.json(billingService.charge())
                            }
                        }
                    }
                }
            }
        }
    }
}
