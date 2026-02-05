package com.edu.api.pop_cube_wallet.transaction.application;

import com.edu.api.pop_cube_wallet.transaction.domain.StatementEntry;
import com.edu.api.pop_cube_wallet.transaction.domain.StatementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Query handler for the CQRS read model (statement entries).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class TransactionQueryHandler implements GetStatementUseCase {

    private final StatementRepository statementRepository;

    @Override
    public List<StatementEntry> execute(GetStatementQuery query) {
        return statementRepository.findByAccountIdOrderByCreatedAtDesc(query.accountId());
    }
}
