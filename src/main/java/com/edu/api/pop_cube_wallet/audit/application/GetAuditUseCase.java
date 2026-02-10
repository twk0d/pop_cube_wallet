package com.edu.api.pop_cube_wallet.audit.application;

import com.edu.api.pop_cube_wallet.audit.domain.AuditEntry;
import org.jmolecules.architecture.hexagonal.PrimaryPort;

import java.util.List;

/**
 * Input port for querying audit trail entries.
 */
@PrimaryPort
public interface GetAuditUseCase {
    List<AuditEntry> execute(GetAuditQuery query);
}
