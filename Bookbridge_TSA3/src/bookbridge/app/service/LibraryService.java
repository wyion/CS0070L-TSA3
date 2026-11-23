package bookbridge.service;

import bookbridge.exception.DuplicateEntityException;
import bookbridge.exception.DomainException;
import bookbridge.exception.EntityNotFoundException;
import bookbridge.exception.ItemUnavailableException;
import bookbridge.exception.LoanLimitExceededException;
import bookbridge.model.BookStatus;
import bookbridge.model.CirculationSummary;
import bookbridge.model.LibraryBook;
import bookbridge.model.LoanRecord;
import bookbridge.model.LoanStatus;
import bookbridge.model.Member;
import bookbridge.model.MembershipType;
import bookbridge.model.Reservation;
import java.time.LocalDate;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.function.Function;

/** Business layer. All catalog, circulation, and queue mutations pass through this class. */
public final class LibraryService {
    private final Map<String, LibraryBook> books = new LinkedHashMap<>();
    private final Map<String, Member> members = new LinkedHashMap<>();
    // ISBN is the lookup key because only one active loan can exist per copy/ISBN.
    private final Map<String, LoanRecord> activeLoans = new LinkedHashMap<>();
    private final Queue<Reservation> pendingReservations = new ArrayDeque<>();
    private final Set<String> checkedOutIsbns = new HashSet<>();
    private final List<LoanRecord> loanHistory = new ArrayList<>();
    private int nextLoanNumber = 1;

    public void registerMember(Member member) throws DuplicateEntityException {
        Objects.requireNonNull(member, "member");
        if (members.containsKey(member.getId())) {
            throw new DuplicateEntityException("Member already exists: " + member.getId());
        }
        members.put(member.getId(), member);
    }

    public void addBook(LibraryBook book) throws DuplicateEntityException {
        Objects.requireNonNull(book, "book");
        if (books.containsKey(book.getIsbn())) {
            throw new DuplicateEntityException("Book already exists: " + book.getIsbn());
        }
        books.put(book.getIsbn(), book);
    }

    public LoanRecord issueLoan(String memberId, String isbn, LocalDate checkoutDate,
                                int requestedDays) throws DomainException {
        Member member = members.get(memberId);
        LibraryBook book = books.get(isbn);
        if (member == null) throw new EntityNotFoundException("Member not found: " + memberId);
        if (book == null) throw new EntityNotFoundException("Book not found: " + isbn);
        if (checkoutDate == null) throw new DomainException("Checkout date is required.");
        if (requestedDays < 1 || requestedDays > 30) {
            throw new DomainException("Requested days must be from 1 to 30.");
        }
        if (requestedDays > member.getMaximumLoanDays()) {
            throw new LoanLimitExceededException(member.getType() + " members may borrow for at most "
                + member.getMaximumLoanDays() + " days.");
        }
        if (book.getStatus() != BookStatus.AVAILABLE || checkedOutIsbns.contains(isbn)) {
            throw new ItemUnavailableException("Book is not available: " + isbn);
        }

        LoanRecord loan = new LoanRecord("L" + String.format("%03d", nextLoanNumber),
            member, book, checkoutDate, requestedDays);
        // Validation is complete before synchronized state is changed.
        nextLoanNumber++;
        book.updateStatusFromService(BookStatus.CHECKED_OUT);
        checkedOutIsbns.add(isbn);
        activeLoans.put(isbn, loan);
        loanHistory.add(loan);
        return loan;
    }

    public void returnBook(String loanId, LocalDate returnDate) throws DomainException {
        if (returnDate == null) throw new DomainException("Return date is required.");
        LoanRecord loan = findByKey(activeLoans.values(), LoanRecord::getLoanId, loanId)
            .orElseThrow(() -> new EntityNotFoundException("Active loan not found: " + loanId));
        LibraryBook book = books.get(loan.getIsbn());
        if (book == null || !checkedOutIsbns.contains(loan.getIsbn())) {
            throw new DomainException("Loan and catalog state are inconsistent; no data was changed.");
        }

        loan.markReturnedFromService(returnDate);
        activeLoans.remove(loan.getIsbn());
        checkedOutIsbns.remove(loan.getIsbn());
        book.updateStatusFromService(BookStatus.AVAILABLE);
    }

    public void reserveBook(String memberId, String isbn, LocalDate requestedOn) throws DomainException {
        Member member = members.get(memberId);
        LibraryBook book = books.get(isbn);
        if (member == null) throw new EntityNotFoundException("Member not found: " + memberId);
        if (book == null) throw new EntityNotFoundException("Book not found: " + isbn);
        if (requestedOn == null) throw new DomainException("Reservation date is required.");
        if (!checkedOutIsbns.contains(isbn)) {
            throw new ItemUnavailableException("A reservation can be placed only for a checked-out book.");
        }
        pendingReservations.add(new Reservation(member, book, requestedOn));
    }

    /** Issues an available title to the earliest matching reservation; leaves the queue unchanged on failure. */
    public Optional<LoanRecord> processNextReservation(String isbn, LocalDate date) throws DomainException {
        Iterator<Reservation> iterator = pendingReservations.iterator();
        while (iterator.hasNext()) {
            Reservation reservation = iterator.next();
            if (reservation.getIsbn().equals(isbn)
                    && books.get(isbn).getStatus() == BookStatus.AVAILABLE) {
                int days = reservation.getMember().getMaximumLoanDays();
                LoanRecord loan = issueLoan(reservation.getMemberId(), isbn, date, days);
                iterator.remove();
                return Optional.of(loan);
            }
        }
        return Optional.empty();
    }

    public Optional<LibraryBook> findBookByIsbn(String isbn) {
        return Optional.ofNullable(books.get(isbn));
    }

    public Optional<Member> findMemberById(String memberId) {
        return Optional.ofNullable(members.get(memberId));
    }

    public Optional<LoanRecord> findActiveLoanByIsbn(String isbn) {
        return Optional.ofNullable(activeLoans.get(isbn));
    }

    public List<LoanRecord> searchLoansByMemberId(String memberId) {
        return loanHistory.stream().filter(loan -> loan.getMemberId().equals(memberId)).toList();
    }

    public List<LibraryBook> searchBooksByTitleKeyword(String keyword) {
        String query = keyword.toLowerCase(java.util.Locale.ROOT);
        return books.values().stream()
            .filter(book -> book.getTitle().toLowerCase(java.util.Locale.ROOT).contains(query))
            .toList();
    }

    public List<LibraryBook> listBooks() { return List.copyOf(books.values()); }
    public List<Member> listMembers() { return List.copyOf(members.values()); }
    public List<LoanRecord> listActiveLoans() { return List.copyOf(activeLoans.values()); }
    public List<LoanRecord> listLoanHistory() { return List.copyOf(loanHistory); }
    public List<Reservation> listReservations() { return List.copyOf(pendingReservations); }
    public Set<String> getCheckedOutIsbns() { return Set.copyOf(checkedOutIsbns); }

    public List<LoanRecord> filterByStatus(LoanStatus status, LocalDate asOf) {
        return loanHistory.stream().filter(loan -> loan.getStatus(asOf) == status).toList();
    }

    public List<LoanRecord> filterByMembership(MembershipType type) {
        return loanHistory.stream().filter(loan -> loan.getMember().getType() == type).toList();
    }

    /** Returns a defensive view ordered by overdue first, due date, then loan ID. */
    public List<LoanRecord> sortedLoanView(LocalDate asOf) {
        Comparator<LoanRecord> order = Comparator
            .comparing((LoanRecord loan) -> loan.getStatus(asOf) != LoanStatus.OVERDUE)
            .thenComparing(LoanRecord::getDueDate)
            .thenComparing(LoanRecord::getLoanId);
        return loanHistory.stream().sorted(order).toList();
    }

    public CirculationSummary createSummary(LocalDate asOf) {
        int available = (int) books.values().stream()
            .filter(book -> book.getStatus() == BookStatus.AVAILABLE).count();
        EnumMap<MembershipType, Long> activeByType = new EnumMap<>(MembershipType.class);
        for (MembershipType type : MembershipType.values()) {
            long count = activeLoans.values().stream()
                .filter(loan -> loan.getMember().getType() == type).count();
            activeByType.put(type, count);
        }
        int fines = loanHistory.stream().mapToInt(loan -> loan.getEstimatedFine(asOf)).sum();
        return new CirculationSummary(available, checkedOutIsbns.size(),
            Collections.unmodifiableMap(activeByType), fines);
    }

    public int pendingReservationCount() { return pendingReservations.size(); }

    /** Generic search helper reused for lookup by a selected key. */
    public static <T, K> Optional<T> findByKey(Collection<T> values,
                                                Function<T, K> keyExtractor,
                                                K target) {
        return values.stream().filter(value -> Objects.equals(keyExtractor.apply(value), target))
            .findFirst();
    }
}
