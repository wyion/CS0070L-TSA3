package bookbridge.model;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

public final class LoanRecord {
    private final String loanId;
    private final Member member;
    private final LibraryBook book;
    private final LocalDate checkoutDate;
    private final LocalDate dueDate;
    private LocalDate returnDate;

    public LoanRecord(String loanId, Member member, LibraryBook book,
                      LocalDate checkoutDate, int requestedDays) {
        this.loanId = Objects.requireNonNull(loanId);
        this.member = Objects.requireNonNull(member);
        this.book = Objects.requireNonNull(book);
        this.checkoutDate = Objects.requireNonNull(checkoutDate);
        this.dueDate = checkoutDate.plusDays(requestedDays);
    }

    public String getLoanId() { return loanId; }
    public Member getMember() { return member; }
    public String getMemberId() { return member.getId(); }
    public LibraryBook getBook() { return book; }
    public String getIsbn() { return book.getIsbn(); }
    public LocalDate getCheckoutDate() { return checkoutDate; }
    public LocalDate getDueDate() { return dueDate; }
    public LocalDate getReturnDate() { return returnDate; }
    public boolean isActive() { return returnDate == null; }

    public LoanStatus getStatus(LocalDate asOf) {
        if (!isActive()) return LoanStatus.RETURNED;
        return asOf.isAfter(dueDate) ? LoanStatus.OVERDUE : LoanStatus.ACTIVE;
    }

    public int getEstimatedFine(LocalDate asOf) {
        LocalDate endDate = returnDate == null ? asOf : returnDate;
        long overdueDays = Math.max(0, ChronoUnit.DAYS.between(dueDate, endDate));
        return Math.toIntExact(overdueDays * member.getDailyFine());
    }

    // Called by LibraryService when a return has passed validation.
    public void markReturnedFromService(LocalDate date) { returnDate = date; }
}
