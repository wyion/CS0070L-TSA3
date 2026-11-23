package bookbridge.exception;

public final class DuplicateEntityException extends DomainException {
    public DuplicateEntityException(String message) { super(message); }
    public DuplicateEntityException(String message, Throwable cause) { super(message, cause); }
}
