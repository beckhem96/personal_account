package org.example.account.controller;

import lombok.RequiredArgsConstructor;
import org.example.account.dto.LhNoticesResponse;
import org.example.account.dto.SubscriptionsResponse;
import org.example.account.service.LhSubscriptionService;
import org.example.account.service.SubscriptionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    private final LhSubscriptionService lhSubscriptionService;

    @GetMapping("/today")
    public ResponseEntity<SubscriptionsResponse> getTodaySubscriptions() {
        return ResponseEntity.ok(subscriptionService.findActiveToday());
    }

    @GetMapping("/lh/today")
    public ResponseEntity<LhNoticesResponse> getTodayLhNotices() {
        return ResponseEntity.ok(lhSubscriptionService.findActiveToday());
    }
}
