package bookbridge.model;

import java.util.Map;

public record CirculationSummary(
    int availableBooks,
    int checkedOutBooks,
    Map<MembershipType, Long> activeLoansByMembership,
    int estimatedFines
) { }
