package bookbridge.model;

import bookbridge.exception.DomainException;

public final class StudentMember extends Member {
    public StudentMember(String id, String name) throws DomainException { super(id, name); }
    @Override public MembershipType getType() { return MembershipType.STUDENT; }
    @Override public int getMaximumLoanDays() { return getType().getMaximumLoanDays(); }
    @Override public int getDailyFine() { return getType().getDailyFine(); }
}
