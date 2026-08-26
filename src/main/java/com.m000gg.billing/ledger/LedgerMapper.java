package com.m000gg.billing.ledger;

import com.m000gg.billing.identity.Admin;
import org.springframework.stereotype.Component;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
public class LedgerMapper {
    public LedgerEntry createLedgerEntryFromTopUpRequestDto(TopUpRequestDto topUpRequestDto, UUID SubscriberId, UUID currentAdminId){
        LedgerEntry ledgerEntry = new  LedgerEntry();
        ledgerEntry.setAmount(topUpRequestDto.getAmount());
        ledgerEntry.setSource(EntrySource.ADMIN);
        ledgerEntry.setPerformedByAdmin(currentAdminId);
        ledgerEntry.setType(EntryType.PAYMENT);
        ledgerEntry.setCreatedAt(Instant.now());
        ledgerEntry.setDescription(topUpRequestDto.getDescription());
        ledgerEntry.setSubscriberId(SubscriberId);

        return ledgerEntry;
    }

    public LedgerEntry createLedgerEntryFromBillRequestDto(BillRequestDto billRequestDto, UUID SubscriberId, UUID currentAdminId){
        LedgerEntry ledgerEntry = new  LedgerEntry();
        ledgerEntry.setAmount(billRequestDto.getAmount());
        ledgerEntry.setSource(EntrySource.ADMIN);
        ledgerEntry.setPerformedByAdmin(currentAdminId);
        ledgerEntry.setType(EntryType.CHARGE);
        ledgerEntry.setCreatedAt(Instant.now());
        ledgerEntry.setDescription(billRequestDto.getDescription());
        ledgerEntry.setSubscriberId(SubscriberId);

        return ledgerEntry;
    }

    public LedgerEntry createLedgerEntryFromCorrectionRequestDto(CorrectionRequestDto correctionRequestDto, UUID SubscriberId, UUID currentAdminId){
        LedgerEntry ledgerEntry = new  LedgerEntry();
        ledgerEntry.setAmount(correctionRequestDto.getAmount());
        ledgerEntry.setSource(EntrySource.ADMIN);
        ledgerEntry.setPerformedByAdmin(currentAdminId);
        ledgerEntry.setDescription(correctionRequestDto.getDescription());
        ledgerEntry.setCreatedAt(Instant.now());
        ledgerEntry.setSubscriberId(SubscriberId);

        if (correctionRequestDto.getDirection() == CorrectionDirection.INCREASE){
            ledgerEntry.setType(EntryType.CORRECTION_INCREASE);
        } else{
            ledgerEntry.setType(EntryType.CORRECTION_DECREASE);
        }

        return ledgerEntry;
    }

    public LedgerEntry createLedgerEntryFromRefundRequestDto(RefundRequestDto refundRequestDto, UUID SubscriberId, UUID currentAdminId){
        LedgerEntry ledgerEntry = new  LedgerEntry();
        ledgerEntry.setAmount(refundRequestDto.getAmount());
        ledgerEntry.setSource(EntrySource.ADMIN);
        ledgerEntry.setPerformedByAdmin(currentAdminId);
        ledgerEntry.setType(EntryType.REFUND);
        ledgerEntry.setCreatedAt(Instant.now());
        ledgerEntry.setSubscriberId(SubscriberId);
        ledgerEntry.setOriginalEntryId(refundRequestDto.getOriginalEntryId());
        ledgerEntry.setDescription(refundRequestDto.getDescription());
        return ledgerEntry;

    }

    public LedgerEntryUserViewModel createLedgerEntryUserViewModelFromLedgerEntry(LedgerEntry ledgerEntry){
        LedgerEntryUserViewModel ledgerEntryUserViewModel = new LedgerEntryUserViewModel();
        ledgerEntryUserViewModel.setAmount(ledgerEntry.getAmount());
        ledgerEntryUserViewModel.setCreatedAt(ledgerEntry.getCreatedAt());
        ledgerEntryUserViewModel.setDescription(ledgerEntry.getDescription());
        ledgerEntryUserViewModel.setType(ledgerEntry.getType());
        ledgerEntryUserViewModel.setSource(ledgerEntry.getSource());
        return ledgerEntryUserViewModel;
    }

    public List<LedgerEntryUserViewModel> createLedgerEntryViewModelsFromLedgerEntries(List<LedgerEntry> ledgerEntries) {
        return ledgerEntries.stream()
                .map(this::createLedgerEntryUserViewModelFromLedgerEntry)
                .toList();
    }

    public LedgerEntryAdminViewModel createLedgerEntryAdminViewModelFromLedgerEntry(LedgerEntry ledgerEntry){
        LedgerEntryAdminViewModel ledgerEntryAdminViewModel = new LedgerEntryAdminViewModel();
        ledgerEntryAdminViewModel.setAmount(ledgerEntry.getAmount());
        ledgerEntryAdminViewModel.setCreatedAt(ledgerEntry.getCreatedAt());
        ledgerEntryAdminViewModel.setDescription(ledgerEntry.getDescription());
        ledgerEntryAdminViewModel.setType(ledgerEntry.getType());
        ledgerEntryAdminViewModel.setOriginalEntryId(ledgerEntry.getOriginalEntryId());
        ledgerEntryAdminViewModel.setId(ledgerEntry.getId());
        ledgerEntryAdminViewModel.setSubscriberId(ledgerEntry.getSubscriberId());
        ledgerEntryAdminViewModel.setSource(ledgerEntry.getSource());
        ledgerEntryAdminViewModel.setPerformedByAdmin(ledgerEntry.getPerformedByAdmin());
        return ledgerEntryAdminViewModel;
    }

    public List<LedgerEntryAdminViewModel> createLedgerEntryAdminViewModelsFromLedgerEntries(List<LedgerEntry> ledgerEntries){
        return  ledgerEntries.stream()
                .map(this::createLedgerEntryAdminViewModelFromLedgerEntry)
                .toList();
    }

    public LedgerEntryAdminViewModel createLedgerEntryAdminViewModelFromLedgerEntry(LedgerEntry ledgerEntry, boolean refundable) {
        LedgerEntryAdminViewModel viewModel = createLedgerEntryAdminViewModelFromLedgerEntry(ledgerEntry);
        viewModel.setRefundable(refundable);
        return viewModel;
    }
}
