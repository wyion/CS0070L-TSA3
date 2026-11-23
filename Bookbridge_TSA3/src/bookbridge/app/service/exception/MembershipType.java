package bookbridge.model;

public enum MembershipType {
    STUDENT(14, 5),
    FACULTY(30, 2),
    GUEST(7, 10);

    private final int maximumLoanDays;
    private final int dailyFine;

    MembershipType(int maximumLoanDays, int dailyFine) {
        this.maximumLoanDays = maximumLoanDays;
        this.dailyFine = dailyFine;
    }

    public int getMaximumLoanDays() { return maximumLoanDays; }
    public int getDailyFine() { return dailyFine; }
}
