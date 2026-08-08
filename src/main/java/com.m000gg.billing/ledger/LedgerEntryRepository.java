package com.m000gg.billing.ledger;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.UUID;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, UUID> {

    @Query("""
        SELECT c FROM LedgerEntry c
        WHERE c.subscriberId = :subscriberId
          AND c.type = com.m000gg.billing.ledger.EntryType.CHARGE
          AND NOT EXISTS (
              SELECT r FROM LedgerEntry r
              WHERE r.type = com.m000gg.billing.ledger.EntryType.REFUND
                AND r.originalEntryId = c.id
          )
        ORDER BY c.createdAt DESC
        """)
    List<LedgerEntry> findRefundableCharges(@Param("subscriberId") UUID subscriberId);

    boolean existsByOriginalEntryIdAndType(UUID originalEntryId, EntryType type);
}
