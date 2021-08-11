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

* App needs to charge invoices the first day of the month and handle fail cases (network, currency mismatch, customer-invoice not found etc)
* A second attempt should happen the next day, to try to charge the failed invoices again  
* Customers should be informed about failed charges
  
##Complimentary requirements
* Customer inactivity: If a customer could not be charged after some time (3 fails), he should be alerted and be set to inactive.
* Alert and monitoring: Implement some monitoring/logging solution
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

* Explore Kotlin coroutines and implement asynchronous execution

## Development

### 1st day 

1. Build app locally
2. Build docker image
3. Test given scripts for docker
4. Test endpoints (Insomnia)
5. Understand application structure and given requirements