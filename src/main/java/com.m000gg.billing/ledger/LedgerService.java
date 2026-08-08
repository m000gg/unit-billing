package com.m000gg.billing.ledger;

import com.m000gg.billing.ledger.exception.InsufficientBalanceException;
import com.m000gg.billing.ledger.exception.InvalidRefundTargetException;
import com.m000gg.billing.ledger.exception.RefundExceedsOriginalChargeException;
import com.m000gg.billing.subscribers.ApplicationUser;
import com.m000gg.billing.subscribers.ApplicationUserRepository;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;

@Service
public class LedgerService {

    @Autowired
    private LedgerMapper ledgerMapper;

    @Autowired
    private LedgerEntryRepository ledgerEntryRepository;

    @Autowired
    private ApplicationUserRepository applicationUserRepository;

    @Transactional
    public void applyTopUp(TopUpRequestDto topUpRequestDto, ApplicationUser user){
        LedgerEntry entry = ledgerMapper.createLedgerEntryFromTopUpRequestDto(topUpRequestDto, user.getId());
        user.setBalance(user.getBalance().add(topUpRequestDto.getAmount()));
        ledgerEntryRepository.save(entry);
        applicationUserRepository.save(user);
    }

    @Transactional
    public void issueBill(@Valid BillRequestDto billRequestDto, ApplicationUser user) {
        if (billRequestDto.getAmount().compareTo(user.getBalance()) > 0) {
            throw new InsufficientBalanceException(user.getId(), billRequestDto.getAmount(), user.getBalance());
        }
        LedgerEntry entry = ledgerMapper.createLedgerEntryFromBillRequestDto(billRequestDto, user.getId());
        user.setBalance(user.getBalance().subtract(billRequestDto.getAmount()));
        ledgerEntryRepository.save(entry);
        applicationUserRepository.save(user);
    }

    @Transactional
    public void applyCorrection(CorrectionRequestDto correctionRequestDto, ApplicationUser user){
        CorrectionDirection correctionDirection = correctionRequestDto.getDirection();
        if (correctionRequestDto.getAmount().compareTo(user.getBalance()) > 0 && correctionDirection == CorrectionDirection.DECREASE) {
            throw new InsufficientBalanceException(user.getId(), correctionRequestDto.getAmount(), user.getBalance());
        }
        LedgerEntry entry = ledgerMapper.createLedgerEntryFromCorrectionRequestDto(correctionRequestDto, user.getId());
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
    public void applyRefund(RefundRequestDto refundRequestDto, ApplicationUser user){
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


        LedgerEntry entry = ledgerMapper.createLedgerEntryFromRefundRequestDto(refundRequestDto, user.getId());
        user.setBalance(user.getBalance().add(refundRequestDto.getAmount()));
        ledgerEntryRepository.save(entry);
        applicationUserRepository.save(user);

    }
}
