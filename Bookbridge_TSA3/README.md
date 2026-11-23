# BookBridge_TSA3 NetBeans source

This source tree separates the application into the TSA3-required packages:

- `bookbridge.app` — console demo and `BookBridgeTSA3` entry point
- `bookbridge.model` — members, books, loans, reservations, enums, and summary record
- `bookbridge.service` — collection ownership and circulation operations
- `bookbridge.exception` — checked domain exceptions

Create a Java Application project named `BookBridge_TSA3` in NetBeans, then copy the `src/bookbridge` directory into the project's `src` directory. Set the main class to `bookbridge.app.BookBridgeTSA3`. Use a Java 17 platform because the summary uses a record and the service uses `Stream.toList()`.

Membership rules continue from TSA1 and TSA2: Student = 14 loan days / PHP 5 per late day; Faculty = 30 / PHP 2; Guest = 7 / PHP 10. A `LoanLimitExceededException` is raised when a requested loan period exceeds that member type's maximum. The TSA3 instructions did not specify a separate concurrent-loan count limit, so none is invented here.
