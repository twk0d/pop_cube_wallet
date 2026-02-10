package com.edu.api.pop_cube_wallet.audit.application;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Query to retrieve audit entries for a given account within a date range.
 */
public record GetAuditQuery(
        UUID accountId,
        LocalDateTime from,
        LocalDateTime to
) {}
