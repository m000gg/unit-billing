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
import com.m000gg.billing.ledger.exception.ExchangeRateMismatchException;
import com.m000gg.billing.ledger.exception.InsufficientBalanceException;
import com.m000gg.billing.ledger.exception.InvalidRefundTargetException;
import com.m000gg.billing.ledger.exception.RefundExceedsOriginalChargeException;
import com.m000gg.billing.subscribers.ApplicationUser;
import com.m000gg.billing.subscribers.ApplicationUserRepository;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Currency;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class LedgerService {

    @Autowired
    private LedgerMapper ledgerMapper;

    @Autowired
    private LedgerEntryRepository ledgerEntryRepository;

    @Autowired
    private ApplicationUserRepository applicationUserRepository;

    private void validateExchangeRate(BigDecimal amount, BigDecimal amountInBaseCurrency, BigDecimal exchangeRate) {
        BigDecimal halfCent = new BigDecimal("0.005");
        BigDecimal expectedAmount = amountInBaseCurrency.multiply(exchangeRate);
        BigDecimal tolerance = halfCent.multiply(exchangeRate).add(halfCent);
        if (expectedAmount.subtract(amount).abs().compareTo(tolerance) > 0) {
            throw new ExchangeRateMismatchException(amount, amountInBaseCurrency, exchangeRate, expectedAmount.setScale(2, RoundingMode.HALF_UP));
        }
    }

    private BigDecimal calculateNewBalance(BigDecimal currentBalance, BigDecimal delta, boolean isAddition) {
        BigDecimal result = isAddition ? currentBalance.add(delta) : currentBalance.subtract(delta);
        return result.setScale(2, RoundingMode.HALF_UP);
    }

    @Transactional
    public void applyTopUp(TopUpRequestDto topUpRequestDto, ApplicationUser user, Admin currentAdmin) {
        validateExchangeRate(topUpRequestDto.getAmount(), topUpRequestDto.getAmountInBaseCurrency(), topUpRequestDto.getExchangeRate());
        LedgerEntry entry = ledgerMapper.createLedgerEntryFromTopUpRequestDto(topUpRequestDto, user.getId(), currentAdmin.getId());
        user.setBalance(calculateNewBalance(user.getBalance(), topUpRequestDto.getAmount(), true));
        ledgerEntryRepository.save(entry);
        applicationUserRepository.save(user);
    }

    @Transactional
    public void issueBill(@Valid BillRequestDto billRequestDto, ApplicationUser user, Admin currentAdmin) {
        validateExchangeRate(billRequestDto.getAmount(), billRequestDto.getAmountInBaseCurrency(), billRequestDto.getExchangeRate());

        if (billRequestDto.getAmount().compareTo(user.getBalance()) > 0) {
            throw new InsufficientBalanceException(user.getId(), billRequestDto.getAmount(), user.getBalance());
        }
        LedgerEntry entry = ledgerMapper.createLedgerEntryFromBillRequestDto(billRequestDto, user.getId(), currentAdmin.getId());
        user.setBalance(calculateNewBalance(user.getBalance(), billRequestDto.getAmount(), false));

        ledgerEntryRepository.save(entry);
        applicationUserRepository.save(user);
    }

    @Transactional
    public void applyCorrection(CorrectionRequestDto correctionRequestDto, ApplicationUser user, Admin currentAdmin) {
        validateExchangeRate(correctionRequestDto.getAmount(), correctionRequestDto.getAmountInBaseCurrency(), correctionRequestDto.getExchangeRate());
        CorrectionDirection correctionDirection = correctionRequestDto.getDirection();
        if (correctionDirection == CorrectionDirection.DECREASE && correctionRequestDto.getAmount().compareTo(user.getBalance()) > 0) {
            throw new InsufficientBalanceException(user.getId(), correctionRequestDto.getAmount(), user.getBalance());
        }
        LedgerEntry entry = ledgerMapper.createLedgerEntryFromCorrectionRequestDto(correctionRequestDto, user.getId(), currentAdmin.getId());
        boolean isAddition = (correctionDirection == CorrectionDirection.INCREASE);
        user.setBalance(calculateNewBalance(user.getBalance(), correctionRequestDto.getAmount(), isAddition));

        ledgerEntryRepository.save(entry);
        applicationUserRepository.save(user);
    }

    @Transactional
    public void applyRefund(RefundRequestDto refundRequestDto, ApplicationUser user, Admin currentAdmin) {
        LedgerEntry originalChargeLedger = ledgerEntryRepository.findById(refundRequestDto.getOriginalEntryId())
                .orElseThrow(() -> new InvalidRefundTargetException(refundRequestDto.getOriginalEntryId()));
        boolean alreadyRefunded = ledgerEntryRepository.existsByOriginalEntryIdAndType(
                originalChargeLedger.getId(), EntryType.REFUND);
        if (originalChargeLedger.getType() != EntryType.CHARGE
                || !originalChargeLedger.getSubscriberId().equals(user.getId()) || alreadyRefunded) {
            throw new InvalidRefundTargetException(refundRequestDto.getOriginalEntryId());
        }
        // Refund is denominated in base currency: limit is checked against the original base amount,
        // the exchange rate of the refund may differ from the charge.
        BigDecimal refundBase = refundRequestDto.getAmountInBaseCurrency().setScale(2, RoundingMode.HALF_UP);
        if (refundBase.compareTo(originalChargeLedger.getAmountInBaseCurrency()) > 0) {
            throw new RefundExceedsOriginalChargeException(
                    originalChargeLedger.getId(), refundBase, originalChargeLedger.getAmountInBaseCurrency());
        }

        validateExchangeRate(refundRequestDto.getAmount(), refundRequestDto.getAmountInBaseCurrency(), refundRequestDto.getExchangeRate());
        LedgerEntry entry = ledgerMapper.createLedgerEntryFromRefundRequestDto(refundRequestDto, user.getId(), currentAdmin.getId());
        user.setBalance(calculateNewBalance(user.getBalance(), refundRequestDto.getAmount(), true));
        ledgerEntryRepository.save(entry);
        applicationUserRepository.save(user);
    }

    public List<LedgerEntry> findRefundableCharges(UUID userId) {
        return ledgerEntryRepository.findRefundableCharges(userId);
    }

    public List<LedgerEntryUserViewModel> getUserLedgerEntryInformation(ApplicationUser applicationUser) {
        UUID userId = applicationUser.getId();
        List<LedgerEntry> ledgerEntries = ledgerEntryRepository.findBySubscriberIdOrderByCreatedAtDesc(userId);
        return ledgerMapper.createLedgerEntryViewModelsFromLedgerEntries(ledgerEntries);
    }

    public List<LedgerEntryUserViewModel> getUserLastFiveLedgerEntries(ApplicationUser applicationUser) {
        UUID userId = applicationUser.getId();
        List<LedgerEntry> ledgerEntries = ledgerEntryRepository.findTop5BySubscriberIdOrderByCreatedAtDesc(userId);
        return ledgerMapper.createLedgerEntryViewModelsFromLedgerEntries(ledgerEntries);
    }

    public Page<LedgerEntryUserViewModel> search(UUID subscriberId, String search, EntryType type, Instant dateFrom, Instant dateTo, Pageable pageable) {
        Page<LedgerEntry> page = ledgerEntryRepository.search(subscriberId, search, type, dateFrom, dateTo, pageable);
        return page.map(ledgerMapper::createLedgerEntryUserViewModelFromLedgerEntry);
    }

    public List<LedgerEntryAdminViewModel> getUserLastFiveLedgerEntriesForAdmin(ApplicationUser user) {
        UUID userId = user.getId();
        List<LedgerEntry> ledgerEntries = ledgerEntryRepository.findTop5BySubscriberIdOrderByCreatedAtDesc(userId);
        return ledgerMapper.createLedgerEntryAdminViewModelsFromLedgerEntries(ledgerEntries);
    }

    public Page<LedgerEntryAdminViewModel> searchForAdmin(UUID subscriberId, String search, EntryType type, Instant dateFrom, Instant dateTo, Pageable pageable) {
        Page<LedgerEntry> page = ledgerEntryRepository.search(subscriberId, search, type, dateFrom, dateTo, pageable);

        Set<UUID> refundableChargeIds = ledgerEntryRepository.findRefundableCharges(subscriberId).stream()
                .map(LedgerEntry::getId)
                .collect(Collectors.toSet());

        return page.map(entry -> ledgerMapper.createLedgerEntryAdminViewModelFromLedgerEntry(
                entry,
                refundableChargeIds.contains(entry.getId())
        ));
    }
}
