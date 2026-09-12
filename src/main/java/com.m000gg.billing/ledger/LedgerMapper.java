/*
 * Copyright 2026 Vladyslav Livandovskyi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.m000gg.billing.ledger;

import com.m000gg.billing.identity.Admin;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
public class LedgerMapper {
    public LedgerEntry createLedgerEntryFromTopUpRequestDto(TopUpRequestDto topUpRequestDto, UUID subscriberId, UUID currentAdminId) {
        LedgerEntry ledgerEntry = new LedgerEntry();
        ledgerEntry.setAmount(topUpRequestDto.getAmount());
        ledgerEntry.setSource(EntrySource.ADMIN);
        ledgerEntry.setPerformedByAdmin(currentAdminId);
        ledgerEntry.setType(EntryType.PAYMENT);
        ledgerEntry.setCreatedAt(Instant.now());
        ledgerEntry.setDescription(topUpRequestDto.getDescription());
        ledgerEntry.setSubscriberId(subscriberId);
        ledgerEntry.setUserCurrency(topUpRequestDto.getUserCurrency());
        ledgerEntry.setBaseCurrency(topUpRequestDto.getBaseCurrency());
        ledgerEntry.setExchangeRate(topUpRequestDto.getExchangeRate());
        ledgerEntry.setExchangeRateSource(topUpRequestDto.getExchangeRateSource());
        ledgerEntry.setAmountInBaseCurrency(topUpRequestDto.getAmountInBaseCurrency());

        return ledgerEntry;
    }

    public LedgerEntry createLedgerEntryFromBillRequestDto(BillRequestDto billRequestDto, UUID subscriberId, UUID currentAdminId) {
        LedgerEntry ledgerEntry = new LedgerEntry();
        ledgerEntry.setAmount(billRequestDto.getAmount());
        ledgerEntry.setSource(EntrySource.ADMIN);
        ledgerEntry.setPerformedByAdmin(currentAdminId);
        ledgerEntry.setType(EntryType.CHARGE);
        ledgerEntry.setCreatedAt(Instant.now());
        ledgerEntry.setDescription(billRequestDto.getDescription());
        ledgerEntry.setSubscriberId(subscriberId);
        ledgerEntry.setUserCurrency(billRequestDto.getUserCurrency());
        ledgerEntry.setBaseCurrency(billRequestDto.getBaseCurrency());
        ledgerEntry.setExchangeRate(billRequestDto.getExchangeRate());
        ledgerEntry.setExchangeRateSource(billRequestDto.getExchangeRateSource());
        ledgerEntry.setAmountInBaseCurrency(billRequestDto.getAmountInBaseCurrency());

        return ledgerEntry;
    }

    public LedgerEntry createLedgerEntryFromCorrectionRequestDto(CorrectionRequestDto correctionRequestDto, UUID subscriberId, UUID currentAdminId) {
        LedgerEntry ledgerEntry = new LedgerEntry();
        ledgerEntry.setAmount(correctionRequestDto.getAmount());
        ledgerEntry.setSource(EntrySource.ADMIN);
        ledgerEntry.setPerformedByAdmin(currentAdminId);
        ledgerEntry.setDescription(correctionRequestDto.getDescription());
        ledgerEntry.setCreatedAt(Instant.now());
        ledgerEntry.setSubscriberId(subscriberId);
        ledgerEntry.setUserCurrency(correctionRequestDto.getUserCurrency());
        ledgerEntry.setBaseCurrency(correctionRequestDto.getBaseCurrency());
        ledgerEntry.setExchangeRate(correctionRequestDto.getExchangeRate());
        ledgerEntry.setExchangeRateSource(correctionRequestDto.getExchangeRateSource());
        ledgerEntry.setAmountInBaseCurrency(correctionRequestDto.getAmountInBaseCurrency());

        if (correctionRequestDto.getDirection() == CorrectionDirection.INCREASE) {
            ledgerEntry.setType(EntryType.CORRECTION_INCREASE);
        } else {
            ledgerEntry.setType(EntryType.CORRECTION_DECREASE);
        }

        return ledgerEntry;
    }

    public LedgerEntry createLedgerEntryFromRefundRequestDto(RefundRequestDto refundRequestDto, UUID subscriberId, UUID currentAdminId) {
        LedgerEntry ledgerEntry = new LedgerEntry();
        ledgerEntry.setAmount(refundRequestDto.getAmount());
        ledgerEntry.setSource(EntrySource.ADMIN);
        ledgerEntry.setPerformedByAdmin(currentAdminId);
        ledgerEntry.setType(EntryType.REFUND);
        ledgerEntry.setCreatedAt(Instant.now());
        ledgerEntry.setSubscriberId(subscriberId);
        ledgerEntry.setOriginalEntryId(refundRequestDto.getOriginalEntryId());
        ledgerEntry.setDescription(refundRequestDto.getDescription());
        ledgerEntry.setUserCurrency(refundRequestDto.getUserCurrency());
        ledgerEntry.setBaseCurrency(refundRequestDto.getBaseCurrency());
        ledgerEntry.setExchangeRate(refundRequestDto.getExchangeRate());
        ledgerEntry.setExchangeRateSource(refundRequestDto.getExchangeRateSource());
        ledgerEntry.setAmountInBaseCurrency(refundRequestDto.getAmountInBaseCurrency());

        return ledgerEntry;
    }

    public LedgerEntryUserViewModel createLedgerEntryUserViewModelFromLedgerEntry(LedgerEntry ledgerEntry) {
        LedgerEntryUserViewModel ledgerEntryUserViewModel = new LedgerEntryUserViewModel();
        ledgerEntryUserViewModel.setAmount(ledgerEntry.getAmount());
        ledgerEntryUserViewModel.setCreatedAt(ledgerEntry.getCreatedAt());
        ledgerEntryUserViewModel.setDescription(ledgerEntry.getDescription());
        ledgerEntryUserViewModel.setType(ledgerEntry.getType());
        ledgerEntryUserViewModel.setSource(ledgerEntry.getSource());
        ledgerEntryUserViewModel.setUserCurrency(ledgerEntry.getUserCurrency());
        return ledgerEntryUserViewModel;
    }

    public List<LedgerEntryUserViewModel> createLedgerEntryViewModelsFromLedgerEntries(List<LedgerEntry> ledgerEntries) {
        return ledgerEntries.stream()
                .map(this::createLedgerEntryUserViewModelFromLedgerEntry)
                .toList();
    }


    public LedgerEntryAdminViewModel createLedgerEntryAdminViewModelFromLedgerEntry(LedgerEntry ledgerEntry) {
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
        ledgerEntryAdminViewModel.setUserCurrency(ledgerEntry.getUserCurrency());
        ledgerEntryAdminViewModel.setAmountInBaseCurrency(ledgerEntry.getAmountInBaseCurrency());
        ledgerEntryAdminViewModel.setExchangeRate(ledgerEntry.getExchangeRate());
        ledgerEntryAdminViewModel.setExchangeRateSource(ledgerEntry.getExchangeRateSource());
        ledgerEntryAdminViewModel.setBaseCurrency(ledgerEntry.getBaseCurrency());
        return ledgerEntryAdminViewModel;
    }

    public List<LedgerEntryAdminViewModel> createLedgerEntryAdminViewModelsFromLedgerEntries(List<LedgerEntry> ledgerEntries) {
        return ledgerEntries.stream()
                .map(this::createLedgerEntryAdminViewModelFromLedgerEntry)
                .toList();
    }

    public LedgerEntryAdminViewModel createLedgerEntryAdminViewModelFromLedgerEntry(LedgerEntry ledgerEntry, boolean refundable) {
        LedgerEntryAdminViewModel viewModel = createLedgerEntryAdminViewModelFromLedgerEntry(ledgerEntry);
        viewModel.setRefundable(refundable);
        return viewModel;
    }
}
