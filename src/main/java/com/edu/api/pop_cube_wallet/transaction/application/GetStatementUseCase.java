package com.edu.api.pop_cube_wallet.transaction.application;

import com.edu.api.pop_cube_wallet.transaction.domain.StatementEntry;
import org.jmolecules.architecture.hexagonal.PrimaryPort;

import java.util.List;

/**
 * Input port for querying the account statement (CQRS query side).
 */
@PrimaryPort
public interface GetStatementUseCase {
    List<StatementEntry> execute(GetStatementQuery query);
}
