package bookbridge.exception;

public final class LoanLimitExceededException extends DomainException {
    public LoanLimitExceededException(String message) { super(message); }
    public LoanLimitExceededException(String message, Throwable cause) { super(message, cause); }
}
