package com.m000gg.billing.ledger;

import com.m000gg.billing.identity.Admin;
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

import java.time.Instant;
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

    @Transactional
    public void applyTopUp(TopUpRequestDto topUpRequestDto, ApplicationUser user, Admin currentAdmin){
        LedgerEntry entry = ledgerMapper.createLedgerEntryFromTopUpRequestDto(topUpRequestDto, user.getId(), currentAdmin.getId());
        user.setBalance(user.getBalance().add(topUpRequestDto.getAmount()));
        ledgerEntryRepository.save(entry);
        applicationUserRepository.save(user);
    }

    @Transactional
    public void issueBill(@Valid BillRequestDto billRequestDto, ApplicationUser user, Admin currentAdmin) {
        if (billRequestDto.getAmount().compareTo(user.getBalance()) > 0) {
            throw new InsufficientBalanceException(user.getId(), billRequestDto.getAmount(), user.getBalance());
        }
        LedgerEntry entry = ledgerMapper.createLedgerEntryFromBillRequestDto(billRequestDto, user.getId(), currentAdmin.getId());
        user.setBalance(user.getBalance().subtract(billRequestDto.getAmount()));
        ledgerEntryRepository.save(entry);
        applicationUserRepository.save(user);
    }

    @Transactional
    public void applyCorrection(CorrectionRequestDto correctionRequestDto, ApplicationUser user, Admin currentAdmin){
        CorrectionDirection correctionDirection = correctionRequestDto.getDirection();
        if (correctionRequestDto.getAmount().compareTo(user.getBalance()) > 0 && correctionDirection == CorrectionDirection.DECREASE) {
            throw new InsufficientBalanceException(user.getId(), correctionRequestDto.getAmount(), user.getBalance());
        }
        LedgerEntry entry = ledgerMapper.createLedgerEntryFromCorrectionRequestDto(correctionRequestDto, user.getId(), currentAdmin.getId());
        if (correctionDirection == CorrectionDirection.DECREASE){
            user.setBalance(user.getBalance().subtract(correctionRequestDto.getAmount()));
        } else {
            user.setBalance(user.getBalance().add(correctionRequestDto.getAmount()));
        }

        ledgerEntryRepository.save(entry);
        applicationUserRepository.save(user);
    }

    public List<LedgerEntry> findRefundableCharges(UUID userId){
        return ledgerEntryRepository.findRefundableCharges(userId);
    }

    @Transactional
    public void applyRefund(RefundRequestDto refundRequestDto, ApplicationUser user, Admin currentAdmin){
        LedgerEntry originalChargeLedger = ledgerEntryRepository.findById(refundRequestDto.getOriginalEntryId())
                .orElseThrow(() -> new InvalidRefundTargetException(refundRequestDto.getOriginalEntryId()));
        boolean alreadyRefunded = ledgerEntryRepository.existsByOriginalEntryIdAndType(
                originalChargeLedger.getId(), EntryType.REFUND);
        if (originalChargeLedger.getType() != EntryType.CHARGE
                || !originalChargeLedger.getSubscriberId().equals(user.getId()) || alreadyRefunded) {
            throw new InvalidRefundTargetException(refundRequestDto.getOriginalEntryId());
        }
        if (refundRequestDto.getAmount().compareTo(originalChargeLedger.getAmount()) > 0){
            throw new RefundExceedsOriginalChargeException(originalChargeLedger.getId(),refundRequestDto.getAmount(),originalChargeLedger.getAmount());

        }

        LedgerEntry entry = ledgerMapper.createLedgerEntryFromRefundRequestDto(refundRequestDto, user.getId(), currentAdmin.getId());
        user.setBalance(user.getBalance().add(refundRequestDto.getAmount()));
        ledgerEntryRepository.save(entry);
        applicationUserRepository.save(user);

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
