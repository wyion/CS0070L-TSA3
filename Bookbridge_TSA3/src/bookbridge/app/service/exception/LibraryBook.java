package bookbridge.model;

import bookbridge.exception.DomainException;

public final class LibraryBook {
    private final String isbn;
    private final String title;
    private BookStatus status = BookStatus.AVAILABLE;

    public LibraryBook(String isbn, String title) throws DomainException {
        if (isbn == null || isbn.trim().isEmpty()) throw new DomainException("ISBN cannot be blank.");
        if (title == null || title.trim().isEmpty()) throw new DomainException("Book title cannot be blank.");
        this.isbn = isbn.trim();
        this.title = title.trim();
    }

    public String getIsbn() { return isbn; }
    public String getTitle() { return title; }
    public BookStatus getStatus() { return status; }

    // State changes are performed by LibraryService operations.
    public void updateStatusFromService(BookStatus newStatus) { status = newStatus; }
}
