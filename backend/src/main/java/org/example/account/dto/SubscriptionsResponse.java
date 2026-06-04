package org.example.account.dto;

import java.time.LocalDate;
import java.util.List;

public record SubscriptionsResponse(
        LocalDate asOf,
        boolean apiKeyConfigured,
        List<SubscriptionItem> special,
        List<SubscriptionItem> firstRank,
        List<SubscriptionItem> secondRank,
        List<SubscriptionItem> remainder
) {
}
