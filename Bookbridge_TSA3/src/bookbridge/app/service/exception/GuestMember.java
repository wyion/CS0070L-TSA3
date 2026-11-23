package bookbridge.model;

import bookbridge.exception.DomainException;

public final class GuestMember extends Member {
    public GuestMember(String id, String name) throws DomainException { super(id, name); }
    @Override public MembershipType getType() { return MembershipType.GUEST; }
    @Override public int getMaximumLoanDays() { return getType().getMaximumLoanDays(); }
    @Override public int getDailyFine() { return getType().getDailyFine(); }
}
