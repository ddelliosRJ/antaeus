## Antaeus

Antaeus (/ænˈtiːəs/), in Greek mythology, a giant of Libya, the son of the sea god Poseidon and the Earth goddess Gaia. He compelled all strangers who were passing through the country to wrestle with him. Whenever Antaeus touched the Earth (his mother), his strength was renewed, so that even if thrown to the ground, he was invincible. Heracles, in combat with him, discovered the source of his strength and, lifting him up from Earth, crushed him to death.

Welcome to our challenge.

## The challenge

As most "Software as a Service" (SaaS) companies, Pleo needs to charge a subscription fee every month. Our database contains a few invoices for the different markets in which we operate. Your task is to build the logic that will schedule payment of those invoices on the first of the month. While this may seem simple, there is space for some decisions to be taken and you will be expected to justify them.

## Instructions

Fork this repo with your solution. Ideally, we'd like to see your progression through commits, and don't forget to update the README.md to explain your thought process.

Please let us know how long the challenge takes you. We're not looking for how speedy or lengthy you are. It's just really to give us a clearer idea of what you've produced in the time you decided to take. Feel free to go as big or as small as you want.

## Developing

Requirements:
- \>= Java 11 environment

Open the project using your favorite text editor. If you are using IntelliJ, you can open the `build.gradle.kts` file and it is gonna setup the project in the IDE for you.

### Building

```
./gradlew clean build
```

### Running

There are 2 options for running Anteus. You either need libsqlite3 or docker. Docker is easier but requires some docker knowledge. We do recommend docker though.

*Running Natively*

Native java with sqlite (requires libsqlite3):

If you use homebrew on MacOS `brew install sqlite`.

```
./gradlew run
```

*Running through docker*

Install docker for your platform

```
docker build -t antaeus
docker run antaeus
```

Alternatively you can run the provided scripts in the repo, which do all the job for you

To build a docker container with pre-configured resources:
```
./docker-start.sh
```

To clean up all resources:
```
./docker-clean.sh
```

### App Structure
The code given is structured as follows. Feel free however to modify the structure to fit your needs.
```
├── buildSrc
|  | gradle build scripts and project wide dependency declarations
|  └ src/main/kotlin/utils.kt 
|      Dependencies
|
├── pleo-antaeus-app
|       main() & initialization
|
├── pleo-antaeus-core
|       This is probably where you will introduce most of your new code.
|       Pay attention to the PaymentProvider and BillingService class.
|
├── pleo-antaeus-data
|       Module interfacing with the database. Contains the database 
|       models, mappings and access layer.
|
├── pleo-antaeus-models
|       Definition of the Internal and API models used throughout the
|       application.
|
└── pleo-antaeus-rest
        Entry point for HTTP REST API. This is where the routes are defined.
```

### Main Libraries and dependencies
* [Exposed](https://github.com/JetBrains/Exposed) - DSL for type-safe SQL
* [Javalin](https://javalin.io/) - Simple web framework (for REST)
* [kotlin-logging](https://github.com/MicroUtils/kotlin-logging) - Simple logging framework for Kotlin
* [JUnit 5](https://junit.org/junit5/) - Testing framework
* [Mockk](https://mockk.io/) - Mocking library
* [Sqlite3](https://sqlite.org/index.html) - Database storage engine

Happy hacking 😁!

---
# Preparation

## Requirements

* App needs to charge invoices the first day of the month and handle fail cases (network, currency mismatch, customer-invoice not found etc).
* A second attempt should happen the next day, to try to charge the failed invoices again.  
* Customers should be informed about failed charges.
  
## Complimentary requirements
* Customer inactivity: If a customer could not be charged after some time (3 fails), he should be alerted and be set to inactive.
* Alert and monitoring: Implement some monitoring/logging solution.
* Time zone handling: Run the billing service at a time that all customers' timezones are at the first day of the month.

## Proposed Implementation

* First, we are going to implement our solution without a scheduler.
* We need to fetch all invoices per customer, attempt to charge, and change the invoice status to PAID from PENDING.
* We also need to handle cases of failure, like network failure, double charge, customer not found etc.

**One proposed way to do this is to create a third table in our database, where we are going to handle only pending invoices and update their status accordingly.**

This way, our app will work like this:
* At a scheduled time, the billing service is going to fetch all pending invoices per customer, try to charge, handle fail cases and update the table.

## Scheduler proposed solutions

* We can use a kron scheduler to handle our monthly billing, or 
* We can create a docker cron job and expose a specific API endpoint to call the billing service and respond with a message. This way it's easier to expose that API to third party payment providers in the future, if it's necessary, with all necessary restrictions

## Concurrency

* Explore Kotlin coroutines and implement asynchronous execution.

## Development

### 1st day 

1. Build app locally.
2. Build docker image.
3. Test given scripts for docker.
4. Test endpoints (Insomnia).
5. Understand application structure and given requirements.

### 2nd day

1. Create a new table to handle the charging process and keep the other two intact.
   * This way, in case of any errors, only data from the new table with be messed up, and our database would be safe.
2. Dig into coroutines - (maybe I should have started building the solution first and them implementing async logic but 
   anyways!).
2. Dig deeper into coroutines - they probably seem the right thing to do.
3. Implement logic into project - suspend in methods, runBlocking, etc. Searched a lot online, still trying to figure out
   how to implement properly.
4. Try a first approach into constructing the billing service.
    * As it is now, it will try to fetch an invoice, create a Payment table and change status and state if charge was successful.
    
**TODO:**
* Figure out where else coroutines should be employed.
* Refactor chargeInvoice in InvoiceService - it should only fetch pending invoices and proceed with payments. 
* Add several safety locks - double charge, currency mismatch, etc.
* Create proper tests.

**FUTURE TODO:**
*Scheduler*
* I tend to lean towards cron job which calls and exposed API to do the charging versus internal scheduler.
* This way we can use the API as wished, and rely on the cron job rather than an internal scheduler.

### 3rd day

1. Second implementation of Billing service. Our app now is capable of the following:
* it can fetch invoices by status.
* it can proceed to charge only pending invoices.
* it can simulate failures.

2. Expose two API paths, one for fetching invoices by status and one for running the billing process.
* each call is idempotent, meaning no invoice is going to be charged twice.

3. Implement checks for Invoice double charge and customer and invoice currency mismatch.

4. Create dockerfile configuration and bash scripts to run a docker cron the first day of the month, at 12:00. 


**TODO:**
* Implement retries on Network failures in Invoice service.
* As it is right now, not all invoices are going to be charged with a single call, unless the Payment provider only 
  returns true.
* Figure out how to run billing service as long as it is necessary to complete all pending payments.
* Implement proper unit testing.

### 4th day 

1. Refactor project - remove obsolete empty lines, change method names to more meaningful ones.
2. Expose API paths to be able to fetch entities from the Payment table.
3. Implement BillingService, Antaeus DAL tests and refactor existing ones.
4. Add cron in docker image, use supervisor to run in paraller to the app and config crontab to run on the first day of 
   the month.

#### Notes:
* Supervisor is not the best possible solution, but it's an easy fix.
* Other alternatives fetched from here: https://stackoverflow.com/a/33847142.
* Supervisor setup and config fetched from here: https://github.com/binxio/blog-cron-supervisor-docker.

**TODO:**
* Implement Rest API tests.

---

## EDITOR NOTES

As it is right now, app can run natively or as a docker image.  

**To be able to run natively and test do the following:**
1. Build and run the app.
2. Run script `billing-cron.sh` and tail log file `/var/log/cron.log`  
   (Hint: `./billing-cron.sh && tail -f /var/log/cron.log`).
3. To check the cron job, add one to local environment, make it run every couple of minutes  
`echo "*/2 * * * * root {path-to-repo}/antaeus/billing-cron.sh >> /etc/crontab` and tail logs as before.
   
*Note:*   
Both the `echo` command, and the `billing-cron` script need to be run with `sudo` because both the log file, 
and the crontab require admin rights.
   
**To run the app as a docker image do the following:**
1. Simply run the modified `./docker-start.sh` script.  
The modified script exploits supervisor to run a cron job parallel to the app.  
(*There are a few alternatives but supervisor was chosen as the most straightforward*).
2. App can be tested as above, by running the `billing-cron.sh` script with `docker exec` command.
3. Cron job is pre-configured  to run the first day of every month, at 12:00, to try to charge invoices in the same day 
   and avoid as much as possible timezone problems. To fix that permanently, additional cron jobs could be scheduled for 
   several customers a day after. Cron job can be configured in `cronjob` file.
   
## Additional notes on the project logic

App could be extended to handle possible cases that are not handled as it is.
Additional features could involve the following:

1. A second cron job could be scheduled to run after a day or two to handle payment provided failures. This way, if the 
   provider fails or if the app crashes, a second or third attempt to charge will happen again when scheduled.
2. No flag or alerting is integrated to inform a customer for a failed invoice. The app could do that, or the billing 
   script to alert the customer, e.g., with an email.
3. The customer could be set to inactive after several failures.
4. An invoice cannot be charged twice, since only pending invoices are fetched and if the payment provider is successful,
   meaning no errors were encountered, the invoice status will change. However, no handling exists for possible app 
   crashes or CI/CD operations.
5. We are assuming that the payment provider (3rd party) is idempotent, meaning it cannot double charge an invoice.
6. No proper authentication is implemented in REST API, this one's mandatory, especially if we are going to use API with
   an external service or expose it out of the docker container.