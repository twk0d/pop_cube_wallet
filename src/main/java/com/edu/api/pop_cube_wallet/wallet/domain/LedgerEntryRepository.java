package com.edu.api.pop_cube_wallet.wallet.domain;

import org.jmolecules.architecture.hexagonal.SecondaryPort;
import org.jmolecules.ddd.annotation.Repository;

/**
 * Output port for LedgerEntry persistence.
 */
@Repository
@SecondaryPort
public interface LedgerEntryRepository {

    LedgerEntry save(LedgerEntry entry);
}
