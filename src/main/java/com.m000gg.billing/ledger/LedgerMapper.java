package com.m000gg.billing.ledger;

import org.springframework.stereotype.Component;
import java.time.Instant;
import java.util.UUID;

@Component
public class LedgerMapper {
    public LedgerEntry createLedgerEntryFromTopUpRequestDto(TopUpRequestDto topUpRequestDto, UUID SubscriberId){
        LedgerEntry ledgerEntry = new  LedgerEntry();
        ledgerEntry.setAmount(topUpRequestDto.getAmount());
        ledgerEntry.setType(EntryType.PAYMENT);
        ledgerEntry.setCreatedAt(Instant.now());
        ledgerEntry.setDescription(topUpRequestDto.getDescription());
        ledgerEntry.setSubscriberId(SubscriberId);

        return ledgerEntry;
    }

    public LedgerEntry createLedgerEntryFromBillRequestDto(BillRequestDto billRequestDto, UUID SubscriberId){
        LedgerEntry ledgerEntry = new  LedgerEntry();
        ledgerEntry.setAmount(billRequestDto.getAmount());
        ledgerEntry.setType(EntryType.CHARGE);
        ledgerEntry.setCreatedAt(Instant.now());
        ledgerEntry.setDescription(billRequestDto.getDescription());
        ledgerEntry.setSubscriberId(SubscriberId);

        return ledgerEntry;
    }
}
