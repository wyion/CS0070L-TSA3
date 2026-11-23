package bookbridge.model;

import bookbridge.exception.DomainException;

public abstract class Member {
    private final String id;
    private final String name;

    protected Member(String id, String name) throws DomainException {
        if (id == null || id.trim().isEmpty()) {
            throw new DomainException("Member ID cannot be blank.");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new DomainException("Member name cannot be blank.");
        }
        this.id = id.trim();
        this.name = name.trim();
    }

    public final String getId() { return id; }
    public final String getName() { return name; }
    public abstract MembershipType getType();
    public abstract int getMaximumLoanDays();
    public abstract int getDailyFine();
}
