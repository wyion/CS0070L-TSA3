package bookbridge.model;

import java.time.LocalDate;
import java.util.Objects;

public final class Reservation {
    private final Member member;
    private final LibraryBook book;
    private final LocalDate requestedOn;

    public Reservation(Member member, LibraryBook book, LocalDate requestedOn) {
        this.member = Objects.requireNonNull(member);
        this.book = Objects.requireNonNull(book);
        this.requestedOn = Objects.requireNonNull(requestedOn);
    }

    public Member getMember() { return member; }
    public String getMemberId() { return member.getId(); }
    public LibraryBook getBook() { return book; }
    public String getIsbn() { return book.getIsbn(); }
    public LocalDate getRequestedOn() { return requestedOn; }
}
