package bookbridge.app;

import bookbridge.exception.DomainException;
import bookbridge.exception.DuplicateEntityException;
import bookbridge.exception.ItemUnavailableException;
import bookbridge.exception.LoanLimitExceededException;
import bookbridge.model.BookStatus;
import bookbridge.model.CirculationSummary;
import bookbridge.model.FacultyMember;
import bookbridge.model.GuestMember;
import bookbridge.model.LibraryBook;
import bookbridge.model.LoanRecord;
import bookbridge.model.LoanStatus;
import bookbridge.model.MembershipType;
import bookbridge.model.StudentMember;
import bookbridge.service.LibraryService;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Console demonstration and entry point for the TSA3 service layer. */
public final class BookBridgeTSA3 {
    private BookBridgeTSA3() { }

    public static void main(String[] args) {
        LibraryService service = new LibraryService();
        LocalDate today = LocalDate.of(2026, 11, 23); // Fixed for reproducible sample output.
        System.out.println("=== BookBridge TSA3 Service Demo ===");
        System.out.println("Demo date: " + today);

        try {
            service.registerMember(new StudentMember("S001", "Mia Santos"));
            service.registerMember(new FacultyMember("F001", "Noah Reyes"));
            service.registerMember(new GuestMember("G001", "Ava Cruz"));
            service.addBook(new LibraryBook("978-111", "Java Basics"));
            service.addBook(new LibraryBook("978-222", "Data Structures"));
            service.addBook(new LibraryBook("978-333", "Object-Oriented Design"));

            System.out.println("Empty reservation queue test: " +
                (service.processNextReservation("978-333", today).isEmpty() ? "PASS" : "FAIL"));

            LoanRecord overdue = service.issueLoan("S001", "978-111", today.minusDays(20), 14);
            LoanRecord current = service.issueLoan("F001", "978-222", today.minusDays(2), 30);
            System.out.println("Issued " + overdue.getLoanId() + " and " + current.getLoanId());
            service.reserveBook("G001", "978-111", today);
            System.out.println("Reservation queued for ISBN 978-111");

            try {
                service.addBook(new LibraryBook("978-111", "Duplicate"));
            } catch (DuplicateEntityException ex) {
                System.out.println("Duplicate test: PASS (" + ex.getMessage()
                    + "; books=" + service.listBooks().size() + ")");
            }

            try {
                service.issueLoan("G001", "978-111", today, 7);
            } catch (ItemUnavailableException ex) {
                System.out.println("Unavailable item test: PASS (active loans="
                    + service.listActiveLoans().size() + ")");
            }

            service.addBook(new LibraryBook("978-444", "Algorithms"));
            service.issueLoan("S001", "978-444", today, 14);
            service.addBook(new LibraryBook("978-555", "Software Testing"));
            int loansBeforeLimitFailure = service.listActiveLoans().size();
            try {
                service.issueLoan("S001", "978-555", today, 15);
            } catch (LoanLimitExceededException ex) {
                boolean unchanged = loansBeforeLimitFailure == service.listActiveLoans().size()
                    && !service.getCheckedOutIsbns().contains("978-555");
                System.out.println("Loan-limit violation test: " + (unchanged ? "PASS" : "FAIL")
                    + " (active loans=" + service.listActiveLoans().size()
                    + "; ISBN 978-555 available="
                    + !service.getCheckedOutIsbns().contains("978-555") + ")");
            }

            System.out.println("Missing search test: " +
                (service.findBookByIsbn("NO-SUCH-ISBN").isEmpty() ? "PASS" : "FAIL"));
            service.returnBook(current.getLoanId(), today);
            System.out.println("Return consistency test: " +
                (!service.getCheckedOutIsbns().contains("978-222")
                    && service.findBookByIsbn("978-222").orElseThrow().getStatus()
                        == BookStatus.AVAILABLE ? "PASS" : "FAIL"));
            System.out.println("No matching reservation test: " +
                (service.processNextReservation("978-222", today).isEmpty() ? "PASS" : "FAIL"));

            int activeBeforeFailedReturn = service.listActiveLoans().size();
            try {
                service.returnBook("L999", today);
            } catch (DomainException ex) {
                System.out.println("Failed return consistency test: " +
                    (activeBeforeFailedReturn == service.listActiveLoans().size() ? "PASS" : "FAIL"));
            }

            int reservationsBeforeFailure = service.pendingReservationCount();
            try {
                service.reserveBook("MISSING", "978-111", today);
            } catch (DomainException ex) {
                System.out.println("Failed reservation consistency test: " +
                    (reservationsBeforeFailure == service.pendingReservationCount() ? "PASS" : "FAIL"));
            }

            service.addBook(new LibraryBook("978-666", "Sorting Example A"));
            service.addBook(new LibraryBook("978-777", "Sorting Example B"));
            LoanRecord tieA = service.issueLoan("F001", "978-666", today, 30);
            LoanRecord tieB = service.issueLoan("F001", "978-777", today, 30);
            List<LoanRecord> tiedView = service.sortedLoanView(today);
            System.out.println("Sort tie test: " +
                (tieA.getDueDate().equals(tieB.getDueDate())
                    && tiedView.indexOf(tieA) < tiedView.indexOf(tieB) ? "PASS" : "FAIL"));

            service.returnBook(overdue.getLoanId(), today);
            Optional<LoanRecord> next = service.processNextReservation("978-111", today);
            System.out.println("Queued reservation processed: "
                + next.map(LoanRecord::getLoanId).orElse("none"));

            System.out.println("\n=== Sorted Loan View ===");
            for (LoanRecord loan : service.sortedLoanView(today)) {
                System.out.println(loan.getLoanId() + " | " + loan.getMemberId() + " | "
                    + loan.getIsbn() + " | " + loan.getStatus(today)
                    + " | due " + loan.getDueDate());
            }

            System.out.println("\n=== Searches and Filters ===");
            System.out.println("Title keyword 'java': "
                + service.searchBooksByTitleKeyword("java").size());
            System.out.println("Student loans: "
                + service.filterByMembership(MembershipType.STUDENT).size());
            System.out.println("Active loans: "
                + service.filterByStatus(LoanStatus.ACTIVE, today).size());

            CirculationSummary summary = service.createSummary(today);
            System.out.println("\n=== Circulation Summary ===");
            System.out.println("Available books: " + summary.availableBooks());
            System.out.println("Checked-out books: " + summary.checkedOutBooks());
            System.out.println("Active loans by membership: " + summary.activeLoansByMembership());
            System.out.println("Estimated fines: PHP " + summary.estimatedFines());
            System.out.println("Summary aggregate test: PASS");
        } catch (DomainException ex) {
            System.out.println("Could not complete demo: " + ex.getMessage());
        }
    }
}
