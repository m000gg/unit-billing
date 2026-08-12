package com.m000gg.billing.ledger;

import java.util.UUID;

public class LedgerEntryAdminViewModel extends LedgerEntryUserViewModel {
    private UUID subscriberId;
    private UUID originalEntryId;
    private UUID id;

    public UUID getId() {
        return id;
    }
    public void setId(UUID id) {
        this.id = id;
    }
    public UUID getSubscriberId() {
        return subscriberId;
    }
    public void setSubscriberId(UUID subscriberId) {
        this.subscriberId = subscriberId;
    }
    public UUID getOriginalEntryId() {
        return originalEntryId;
    }
    public void setOriginalEntryId(UUID originalEntryId) {
        this.originalEntryId = originalEntryId;
    }
}
