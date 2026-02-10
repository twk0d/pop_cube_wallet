package com.edu.api.pop_cube_wallet.audit.application;

import com.edu.api.pop_cube_wallet.audit.domain.AuditEntry;
import com.edu.api.pop_cube_wallet.audit.domain.AuditEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Query handler for audit read operations.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class AuditQueryHandler implements GetAuditUseCase {

    private final AuditEntryRepository auditEntryRepository;

    @Override
    public List<AuditEntry> execute(GetAuditQuery query) {
        return auditEntryRepository.findByAccountIdAndOccurredAtBetweenOrderByOccurredAtDesc(
                query.accountId(), query.from(), query.to());
    }
}
