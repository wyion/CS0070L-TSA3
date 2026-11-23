package bookbridge.exception;

public final class ItemUnavailableException extends DomainException {
    public ItemUnavailableException(String message) { super(message); }
    public ItemUnavailableException(String message, Throwable cause) { super(message, cause); }
}
