package com.m000gg.billing.ledger;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ledger_entries")
public class LedgerEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID subscriberId;

    @Column(name = "original_entry_id")
    private UUID originalEntryId;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EntryType type;

    @Column(nullable = false)
    private Instant createdAt;

    private String description;

    @Enumerated(EnumType.STRING)
    private EntrySource source;

    private UUID performedByAdmin;

    public EntrySource getSource() {
        return source;
    }
    public void setSource(EntrySource source) {
        this.source = source;
    }
    public UUID getPerformedByAdmin() {
        return performedByAdmin;
    }
    public void setPerformedByAdmin(UUID performedByAdmin) {
        this.performedByAdmin = performedByAdmin;
    }
    public UUID getOriginalEntryId() {
        return originalEntryId;
    }
    public void setOriginalEntryId(UUID originalEntryId) {
        this.originalEntryId = originalEntryId;
    }

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

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public EntryType getType() {
        return type;
    }

    public void setType(EntryType type) {
        this.type = type;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}

