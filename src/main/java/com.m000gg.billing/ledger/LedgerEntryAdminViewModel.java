package com.m000gg.billing.ledger;

import java.math.BigDecimal;
import java.util.UUID;

public class LedgerEntryAdminViewModel extends LedgerEntryUserViewModel {
    private UUID subscriberId;
    private UUID originalEntryId;
    private UUID id;
    private boolean refundable;
    private EntrySource source;
    private UUID performedByAdmin;
    private BigDecimal amountInBaseCurrency;
    private BigDecimal exchangeRate;
    private String exchangeRateSource;
    private String baseCurrency;

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
    public boolean isRefundable() {
        return refundable;
    }
    public void setRefundable(boolean refundable) {
        this.refundable = refundable;
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
    public UUID getOriginalEntryId() {
        return originalEntryId;
    }
    public void setOriginalEntryId(UUID originalEntryId) {
        this.originalEntryId = originalEntryId;
    }
    public BigDecimal getAmountInBaseCurrency() {
        return amountInBaseCurrency;
    }
    public void setAmountInBaseCurrency(BigDecimal amountInBaseCurrency) {
        this.amountInBaseCurrency = amountInBaseCurrency;
    }
    public BigDecimal getExchangeRate() {
        return exchangeRate;
    }
    public void setExchangeRate(BigDecimal exchangeRate) {
        this.exchangeRate = exchangeRate;
    }
    public String getExchangeRateSource() {
        return exchangeRateSource;
    }
    public void setExchangeRateSource(String exchangeRateSource) {
        this.exchangeRateSource = exchangeRateSource;
    }
    public String getBaseCurrency() {
        return baseCurrency;
    }
    public void setBaseCurrency(String baseCurrency) {
        this.baseCurrency = baseCurrency;
    }
}
