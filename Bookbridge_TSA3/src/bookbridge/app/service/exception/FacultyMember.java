package bookbridge.model;

import bookbridge.exception.DomainException;

public final class FacultyMember extends Member {
    public FacultyMember(String id, String name) throws DomainException { super(id, name); }
    @Override public MembershipType getType() { return MembershipType.FACULTY; }
    @Override public int getMaximumLoanDays() { return getType().getMaximumLoanDays(); }
    @Override public int getDailyFine() { return getType().getDailyFine(); }
}
