package com.m000gg.billing.ledger;

import com.m000gg.billing.ledger.exception.ApplicationUserNotFoundException;
import com.m000gg.billing.ledger.exception.InsufficientBalanceException;
import com.m000gg.billing.subscribers.ApplicationUser;
import com.m000gg.billing.subscribers.ApplicationUserRepository;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
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
    public void applyTopUp(TopUpRequestDto topUpRequestDto, UUID id){
        ApplicationUser user = applicationUserRepository.findById(id)
                .orElseThrow(() -> new ApplicationUserNotFoundException(id));
        LedgerEntry entry = ledgerMapper.createLedgerEntryFromTopUpRequestDto(topUpRequestDto, id);
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
}
