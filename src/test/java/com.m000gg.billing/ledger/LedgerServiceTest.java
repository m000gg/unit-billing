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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class LedgerServiceTest {

    @InjectMocks
    private LedgerService ledgerService;

    @Mock
    private LedgerEntryRepository ledgerEntryRepository;

    @Mock
    private ApplicationUserRepository applicationUserRepository;

    @Mock
    private LedgerMapper ledgerMapper;

    private ApplicationUser user;
    private Admin currentAdmin;

    @BeforeEach
    void setUp() {
        user = new ApplicationUser();
        user.setId(UUID.fromString("550e8400-e29b-41d4-a716-446655440001"));
        user.setBalance(BigDecimal.valueOf(100));

        currentAdmin = new Admin();
        currentAdmin.setEmail("test@admin.com");
    }

    //*----Top-Up-Tests----*//
    @Test
    void applyTopUp_successful() {
        TopUpRequestDto topUpRequestDto = new TopUpRequestDto();
        topUpRequestDto.setAmount(BigDecimal.valueOf(100));
        topUpRequestDto.setAmountInBaseCurrency(BigDecimal.valueOf(100));
        topUpRequestDto.setExchangeRate(BigDecimal.ONE);
        LedgerEntry expectedEntry = new LedgerEntry();
        expectedEntry.setType(EntryType.PAYMENT);
        when(ledgerMapper.createLedgerEntryFromTopUpRequestDto(any(), any(), any())).thenReturn(expectedEntry);
        ledgerService.applyTopUp(topUpRequestDto, user, currentAdmin);
        assertEquals(0, BigDecimal.valueOf(200).compareTo(user.getBalance()));
        verify(ledgerEntryRepository).save(argThat(ledgerEntry -> EntryType.PAYMENT.equals(ledgerEntry.getType())));
    }

    @Test
    void applyTopUp_mismatchedExchangeRate_throwsExchangeRateMismatchException() {
        TopUpRequestDto topUpRequestDto = new TopUpRequestDto();
        topUpRequestDto.setAmount(BigDecimal.valueOf(100));
        topUpRequestDto.setAmountInBaseCurrency(BigDecimal.valueOf(100));
        topUpRequestDto.setExchangeRate(BigDecimal.valueOf(2));
        assertThatThrownBy(() -> ledgerService.applyTopUp(topUpRequestDto, user, currentAdmin))
                .isInstanceOf(ExchangeRateMismatchException.class);
        verifyNoInteractions(applicationUserRepository);
        verify(ledgerEntryRepository, never()).save(any());
    }

    @Test
    void applyTopUp_balanceRoundedToTwoDecimals() {
        user.setBalance(new BigDecimal("100.005"));
        TopUpRequestDto topUpRequestDto = new TopUpRequestDto();
        topUpRequestDto.setAmount(new BigDecimal("0.004"));
        topUpRequestDto.setAmountInBaseCurrency(new BigDecimal("0.004"));
        topUpRequestDto.setExchangeRate(BigDecimal.ONE);
        LedgerEntry expectedEntry = new LedgerEntry();
        expectedEntry.setType(EntryType.PAYMENT);
        when(ledgerMapper.createLedgerEntryFromTopUpRequestDto(any(), any(), any())).thenReturn(expectedEntry);
        ledgerService.applyTopUp(topUpRequestDto, user, currentAdmin);
        assertEquals(0, new BigDecimal("100.01").compareTo(user.getBalance()));
    }

    //*----Bill-Tests----*//
    @Test
    void issueBill_successful() {
        BillRequestDto billRequestDto = new BillRequestDto();
        billRequestDto.setAmount(BigDecimal.valueOf(50));
        billRequestDto.setAmountInBaseCurrency(BigDecimal.valueOf(50));
        billRequestDto.setExchangeRate(BigDecimal.ONE);
        LedgerEntry expectedEntry = new LedgerEntry();
        expectedEntry.setType(EntryType.CHARGE);
        when(ledgerMapper.createLedgerEntryFromBillRequestDto(any(), any(), any())).thenReturn(expectedEntry);
        ledgerService.issueBill(billRequestDto, user, currentAdmin);
        assertEquals(0, BigDecimal.valueOf(50).compareTo(user.getBalance()));
        verify(ledgerEntryRepository).save(argThat(ledgerEntry -> EntryType.CHARGE.equals(ledgerEntry.getType())));
    }

    @Test
    void issueBill_raiseInsufficientBalanceException() {
        BillRequestDto billRequestDto = new BillRequestDto();
        billRequestDto.setAmount(BigDecimal.valueOf(150));
        billRequestDto.setAmountInBaseCurrency(BigDecimal.valueOf(150));
        billRequestDto.setExchangeRate(BigDecimal.ONE);

        assertThatThrownBy(() -> ledgerService.issueBill(billRequestDto, user, currentAdmin))
                .isInstanceOf(InsufficientBalanceException.class);

        verify(ledgerEntryRepository, never()).save(any());
    }

    @Test
    void issueBill_limitingCase() {
        BillRequestDto billRequestDto = new BillRequestDto();
        billRequestDto.setAmount(BigDecimal.valueOf(100));
        billRequestDto.setAmountInBaseCurrency(BigDecimal.valueOf(100));
        billRequestDto.setExchangeRate(BigDecimal.ONE);
        LedgerEntry expectedEntry = new LedgerEntry();
        expectedEntry.setType(EntryType.CHARGE);
        when(ledgerMapper.createLedgerEntryFromBillRequestDto(any(), any(), any())).thenReturn(expectedEntry);

        ledgerService.issueBill(billRequestDto, user, currentAdmin);

        assertEquals(0, BigDecimal.valueOf(0).compareTo(user.getBalance()));
        verify(ledgerEntryRepository).save(argThat(ledgerEntry -> EntryType.CHARGE.equals(ledgerEntry.getType())));
    }

    @Test
    void issueBill_mismatchedExchangeRate_checkedBeforeInsufficientBalance() {
        BillRequestDto billRequestDto = new BillRequestDto();
        billRequestDto.setAmount(BigDecimal.valueOf(150));
        billRequestDto.setAmountInBaseCurrency(BigDecimal.valueOf(150));
        billRequestDto.setExchangeRate(BigDecimal.valueOf(2));

        assertThatThrownBy(() -> ledgerService.issueBill(billRequestDto, user, currentAdmin))
                .isInstanceOf(ExchangeRateMismatchException.class);

        verifyNoInteractions(applicationUserRepository);
        verify(ledgerEntryRepository, never()).save(any());
    }

    //*----Correction-Tests----*//

    @Test
    void applyCorrectionIncrease_successful() {
        CorrectionRequestDto correctionRequestDto = new CorrectionRequestDto();
        correctionRequestDto.setAmount(BigDecimal.valueOf(50));
        correctionRequestDto.setAmountInBaseCurrency(BigDecimal.valueOf(50));
        correctionRequestDto.setExchangeRate(BigDecimal.ONE);
        correctionRequestDto.setDirection(CorrectionDirection.INCREASE);
        LedgerEntry expectedEntry = new LedgerEntry();
        expectedEntry.setType(EntryType.CORRECTION_INCREASE);
        when(ledgerMapper.createLedgerEntryFromCorrectionRequestDto(any(), any(), any())).thenReturn(expectedEntry);

        ledgerService.applyCorrection(correctionRequestDto, user, currentAdmin);

        assertEquals(0, BigDecimal.valueOf(150).compareTo(user.getBalance()));
        verify(ledgerEntryRepository).save(argThat(ledgerEntry -> EntryType.CORRECTION_INCREASE.equals(ledgerEntry.getType())));
    }

    @Test
    void applyCorrectionDecrease_successful() {
        CorrectionRequestDto correctionRequestDto = new CorrectionRequestDto();
        correctionRequestDto.setAmount(BigDecimal.valueOf(50));
        correctionRequestDto.setAmountInBaseCurrency(BigDecimal.valueOf(50));
        correctionRequestDto.setExchangeRate(BigDecimal.ONE);
        correctionRequestDto.setDirection(CorrectionDirection.DECREASE);
        LedgerEntry expectedEntry = new LedgerEntry();
        expectedEntry.setType(EntryType.CORRECTION_DECREASE);
        when(ledgerMapper.createLedgerEntryFromCorrectionRequestDto(any(), any(), any())).thenReturn(expectedEntry);

        ledgerService.applyCorrection(correctionRequestDto, user, currentAdmin);

        assertEquals(0, BigDecimal.valueOf(50).compareTo(user.getBalance()));
        verify(ledgerEntryRepository).save(argThat(ledgerEntry -> EntryType.CORRECTION_DECREASE.equals(ledgerEntry.getType())));
    }

    @Test
    void applyCorrectionDecrease_raiseInsufficientBalanceException() {
        CorrectionRequestDto correctionRequestDto = new CorrectionRequestDto();
        correctionRequestDto.setAmount(BigDecimal.valueOf(150));
        correctionRequestDto.setAmountInBaseCurrency(BigDecimal.valueOf(150));
        correctionRequestDto.setExchangeRate(BigDecimal.ONE);
        correctionRequestDto.setDirection(CorrectionDirection.DECREASE);

        assertThatThrownBy(() -> ledgerService.applyCorrection(correctionRequestDto, user, currentAdmin))
                .isInstanceOf(InsufficientBalanceException.class);

        verify(ledgerEntryRepository, never()).save(any());
    }

    @Test
    void applyCorrection_mismatchedExchangeRate_throwsExchangeRateMismatchException() {
        CorrectionRequestDto correctionRequestDto = new CorrectionRequestDto();
        correctionRequestDto.setAmount(BigDecimal.valueOf(50));
        correctionRequestDto.setAmountInBaseCurrency(BigDecimal.valueOf(50));
        correctionRequestDto.setExchangeRate(BigDecimal.valueOf(3));
        correctionRequestDto.setDirection(CorrectionDirection.INCREASE);
        assertThatThrownBy(() -> ledgerService.applyCorrection(correctionRequestDto, user, currentAdmin))
                .isInstanceOf(ExchangeRateMismatchException.class);
        verifyNoInteractions(applicationUserRepository);
        verify(ledgerEntryRepository, never()).save(any());
    }

    //*----Refund-Tests----*//

    @Test
    void applyRefund_successful() {
        UUID originalEntryId = UUID.fromString("890e8403-e29b-4fd0-a726-442655aa0493");
        LedgerEntry originalCharge = new LedgerEntry();
        originalCharge.setId(originalEntryId);
        originalCharge.setType(EntryType.CHARGE);
        originalCharge.setSubscriberId(user.getId());
        originalCharge.setBaseCurrency("USD");
        originalCharge.setAmount(BigDecimal.valueOf(50));
        originalCharge.setAmountInBaseCurrency(BigDecimal.valueOf(50));
        when(ledgerEntryRepository.findById(originalEntryId))
                .thenReturn(Optional.of(originalCharge));
        when(ledgerEntryRepository.existsByOriginalEntryIdAndType(originalEntryId, EntryType.REFUND))
                .thenReturn(false);
        RefundRequestDto refundRequestDto = new RefundRequestDto();
        refundRequestDto.setAmount(BigDecimal.valueOf(50));
        refundRequestDto.setAmountInBaseCurrency(BigDecimal.valueOf(50));
        refundRequestDto.setExchangeRate(BigDecimal.ONE);
        refundRequestDto.setOriginalEntryId(originalEntryId);
        LedgerEntry expectedEntry = new LedgerEntry();
        expectedEntry.setSubscriberId(user.getId());
        expectedEntry.setType(EntryType.REFUND);
        when(ledgerMapper.createLedgerEntryFromRefundRequestDto(any(), any(), any())).thenReturn(expectedEntry);
        ledgerService.applyRefund(refundRequestDto, user, currentAdmin);
        assertEquals(0, BigDecimal.valueOf(150).compareTo(user.getBalance()));
        verify(ledgerEntryRepository).save(argThat(entry -> EntryType.REFUND.equals(entry.getType())));
    }

    @Test
    void applyRefund_originalEntryNotFound_throwsInvalidRefundTarget() {
        UUID originalEntryId = UUID.fromString("890e8403-e29b-4fd0-a726-442655aa0493");

        when(ledgerEntryRepository.findById(originalEntryId))
                .thenReturn(Optional.empty());

        RefundRequestDto refundRequestDto = new RefundRequestDto();
        refundRequestDto.setAmount(BigDecimal.valueOf(50));
        refundRequestDto.setOriginalEntryId(originalEntryId);

        assertThatThrownBy(() -> ledgerService.applyRefund(refundRequestDto, user, currentAdmin))
                .isInstanceOf(InvalidRefundTargetException.class);

        verifyNoInteractions(applicationUserRepository);
        verify(ledgerEntryRepository, never()).save(any());
    }

    @Test
    void applyRefund_originalEntryNotACharge_throwsInvalidRefundTarget() {
        UUID originalEntryId = UUID.fromString("890e8403-e29b-4fd0-a726-442655aa0493");

        LedgerEntry originalEntry = new LedgerEntry();
        originalEntry.setId(originalEntryId);
        originalEntry.setType(EntryType.PAYMENT);
        originalEntry.setSubscriberId(user.getId());
        originalEntry.setAmount(BigDecimal.valueOf(50));

        when(ledgerEntryRepository.findById(originalEntryId))
                .thenReturn(Optional.of(originalEntry));

        RefundRequestDto refundRequestDto = new RefundRequestDto();
        refundRequestDto.setAmount(BigDecimal.valueOf(50));
        refundRequestDto.setOriginalEntryId(originalEntryId);

        assertThatThrownBy(() -> ledgerService.applyRefund(refundRequestDto, user, currentAdmin))
                .isInstanceOf(InvalidRefundTargetException.class);

        verifyNoInteractions(applicationUserRepository);
        verify(ledgerEntryRepository, never()).save(any());
    }

    @Test
    void applyRefund_originalEntryBelongsToDifferentSubscriber_throwsInvalidRefundTarget() {
        UUID originalEntryId = UUID.fromString("890e8403-e29b-4fd0-a726-442655aa0493");
        UUID otherSubscriberId = UUID.fromString("550e8400-e29b-41d4-a716-446655440099");

        LedgerEntry originalCharge = new LedgerEntry();
        originalCharge.setId(originalEntryId);
        originalCharge.setType(EntryType.CHARGE);
        originalCharge.setSubscriberId(otherSubscriberId);
        originalCharge.setAmount(BigDecimal.valueOf(50));

        when(ledgerEntryRepository.findById(originalEntryId))
                .thenReturn(Optional.of(originalCharge));

        RefundRequestDto refundRequestDto = new RefundRequestDto();
        refundRequestDto.setAmount(BigDecimal.valueOf(50));
        refundRequestDto.setOriginalEntryId(originalEntryId);

        assertThatThrownBy(() -> ledgerService.applyRefund(refundRequestDto, user, currentAdmin))
                .isInstanceOf(InvalidRefundTargetException.class);

        verifyNoInteractions(applicationUserRepository);
        verify(ledgerEntryRepository, never()).save(any());
    }

    @Test
    void applyRefund_alreadyRefunded_throwsInvalidRefundTarget() {
        UUID originalEntryId = UUID.fromString("890e8403-e29b-4fd0-a726-442655aa0493");

        LedgerEntry originalCharge = new LedgerEntry();
        originalCharge.setId(originalEntryId);
        originalCharge.setType(EntryType.CHARGE);
        originalCharge.setSubscriberId(user.getId());
        originalCharge.setAmount(BigDecimal.valueOf(50));

        when(ledgerEntryRepository.findById(originalEntryId))
                .thenReturn(Optional.of(originalCharge));
        when(ledgerEntryRepository.existsByOriginalEntryIdAndType(originalEntryId, EntryType.REFUND))
                .thenReturn(true);

        RefundRequestDto refundRequestDto = new RefundRequestDto();
        refundRequestDto.setAmount(BigDecimal.valueOf(50));
        refundRequestDto.setOriginalEntryId(originalEntryId);

        assertThatThrownBy(() -> ledgerService.applyRefund(refundRequestDto, user, currentAdmin))
                .isInstanceOf(InvalidRefundTargetException.class);

        verifyNoInteractions(applicationUserRepository);
        verify(ledgerEntryRepository, never()).save(any());
    }

    @Test
    void applyRefund_mismatchedExchangeRate_throwsExchangeRateMismatchException() {
        UUID originalEntryId = UUID.fromString("890e8403-e29b-4fd0-a726-442655aa0493");

        LedgerEntry originalCharge = new LedgerEntry();
        originalCharge.setId(originalEntryId);
        originalCharge.setType(EntryType.CHARGE);
        originalCharge.setSubscriberId(user.getId());
        originalCharge.setAmount(BigDecimal.valueOf(50));
        originalCharge.setAmountInBaseCurrency(BigDecimal.valueOf(50));

        when(ledgerEntryRepository.findById(originalEntryId))
                .thenReturn(Optional.of(originalCharge));
        when(ledgerEntryRepository.existsByOriginalEntryIdAndType(originalEntryId, EntryType.REFUND))
                .thenReturn(false);

        RefundRequestDto refundRequestDto = new RefundRequestDto();
        refundRequestDto.setAmount(BigDecimal.valueOf(50));
        refundRequestDto.setAmountInBaseCurrency(BigDecimal.valueOf(50));
        refundRequestDto.setExchangeRate(BigDecimal.valueOf(4));
        refundRequestDto.setOriginalEntryId(originalEntryId);

        assertThatThrownBy(() -> ledgerService.applyRefund(refundRequestDto, user, currentAdmin))
                .isInstanceOf(ExchangeRateMismatchException.class);

        verifyNoInteractions(applicationUserRepository);
        verify(ledgerEntryRepository, never()).save(any());
    }

    @Test
    void applyRefund_baseAmountExceedsOriginalCharge_throwsRefundExceedsOriginalCharge() {
        UUID originalEntryId = UUID.fromString("890e8403-e29b-4fd0-a726-442655aa0493");
        LedgerEntry originalCharge = new LedgerEntry();
        originalCharge.setId(originalEntryId);
        originalCharge.setType(EntryType.CHARGE);
        originalCharge.setSubscriberId(user.getId());
        originalCharge.setAmount(BigDecimal.valueOf(50));
        originalCharge.setAmountInBaseCurrency(BigDecimal.valueOf(50));
        when(ledgerEntryRepository.findById(originalEntryId)).thenReturn(Optional.of(originalCharge));
        when(ledgerEntryRepository.existsByOriginalEntryIdAndType(originalEntryId, EntryType.REFUND)).thenReturn(false);
        RefundRequestDto refundRequestDto = new RefundRequestDto();
        refundRequestDto.setAmount(BigDecimal.valueOf(75));
        refundRequestDto.setAmountInBaseCurrency(BigDecimal.valueOf(75));
        refundRequestDto.setExchangeRate(BigDecimal.ONE);
        refundRequestDto.setOriginalEntryId(originalEntryId);

        assertThatThrownBy(() -> ledgerService.applyRefund(refundRequestDto, user, currentAdmin))
                .isInstanceOf(RefundExceedsOriginalChargeException.class);

        verifyNoInteractions(applicationUserRepository);
        verify(ledgerEntryRepository, never()).save(any());
    }

    @Test
    void applyRefund_sameBaseAmountAtDifferentRate_isAllowed() {
        UUID originalEntryId = UUID.fromString("890e8403-e29b-4fd0-a726-442655aa0493");
        LedgerEntry originalCharge = new LedgerEntry();
        originalCharge.setId(originalEntryId);
        originalCharge.setType(EntryType.CHARGE);
        originalCharge.setSubscriberId(user.getId());
        originalCharge.setAmount(new BigDecimal("4486.00"));
        originalCharge.setAmountInBaseCurrency(new BigDecimal("100.00"));
        when(ledgerEntryRepository.findById(originalEntryId)).thenReturn(Optional.of(originalCharge));
        when(ledgerEntryRepository.existsByOriginalEntryIdAndType(originalEntryId, EntryType.REFUND)).thenReturn(false);
        RefundRequestDto refundRequestDto = new RefundRequestDto();
        refundRequestDto.setAmount(new BigDecimal("4550.00"));
        refundRequestDto.setAmountInBaseCurrency(new BigDecimal("100.00"));
        refundRequestDto.setExchangeRate(new BigDecimal("45.50"));
        refundRequestDto.setOriginalEntryId(originalEntryId);
        LedgerEntry expectedEntry = new LedgerEntry();
        expectedEntry.setSubscriberId(user.getId());
        expectedEntry.setType(EntryType.REFUND);
        when(ledgerMapper.createLedgerEntryFromRefundRequestDto(any(), any(), any())).thenReturn(expectedEntry);

        ledgerService.applyRefund(refundRequestDto, user, currentAdmin);

        assertEquals(0, new BigDecimal("4650.00").compareTo(user.getBalance()));
        verify(ledgerEntryRepository).save(argThat(entry -> EntryType.REFUND.equals(entry.getType())));
    }

    @Test
    void search_alwaysScopesQueryToGivenSubscriberId() {
        UUID subscriberId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 10);
        Page<LedgerEntry> emptyPage = new PageImpl<>(List.of());
        when(ledgerEntryRepository.search(eq(subscriberId), any(), isNull(), isNull(), isNull(), eq(pageable)))
                .thenReturn(emptyPage);

        ledgerService.search(subscriberId, "some search", null, null, null, pageable);

        ArgumentCaptor<UUID> subscriberIdCaptor = ArgumentCaptor.forClass(UUID.class);
        verify(ledgerEntryRepository).search(subscriberIdCaptor.capture(), any(), isNull(), isNull(), isNull(), eq(pageable));
        assertThat(subscriberIdCaptor.getValue()).isEqualTo(subscriberId);
    }
}