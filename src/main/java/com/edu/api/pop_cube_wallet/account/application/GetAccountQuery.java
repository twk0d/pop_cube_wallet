package com.edu.api.pop_cube_wallet.account.application;

import java.util.UUID;

/**
 * Query to retrieve a single account by ID.
 */
public record GetAccountQuery(UUID accountId) {}
