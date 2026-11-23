package com.diegodavid.supersave.data

/**
 * What kind of money movement a LedgerEntry is.
 * STARTING_MONEY (D36): the money set up as "for spending" on day one; like an
 * income, it's spread over the next 30 days. Stored as text, so adding it needed
 * no database change.
 */
enum class EntryType { EXPENSE, INCOME, FIXED_PAYMENT, BALANCE_CORRECTION, SAVINGS_TRANSFER, STARTING_MONEY }
