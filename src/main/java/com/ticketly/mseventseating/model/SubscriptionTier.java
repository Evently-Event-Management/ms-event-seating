package com.ticketly.mseventseating.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SubscriptionTier {
    FREE(0),
    PRO(1),
    ENTERPRISE(2);

    private final int level;
}
