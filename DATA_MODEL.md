# Data Model

Status: **v2, approved (D23, D24).**
Under Nivel 2 (D11) the data model is the team's decision; Claude drafted it.

No code yet: these are the tables Room will store on the phone.

---

## Core idea: a ledger

Instead of storing "the balance" as one number we keep overwriting, we store
**every money movement** as a row in one table: `Transaction`.

```
balance = sum of all transactions (except savings transfers, see below)
```

Why:
- **Correct my balance (D14, D18)** is one row: if the app says ₱2,000 and the
  bank says ₱1,700, we add `BALANCE_CORRECTION −300`. Nothing is overwritten, and
  the history shows when and by how much you corrected.
- **Starting balance** at setup is just the first correction row.
- **History, the monthly review (D9) and future charts** come from the same table.
- **Weigh the payment** = "what if we added this row?", computed but not saved.

---

## Savings bucket (D24)

If the money you save **stays in the same bank account**, "Keep it" can't
remove money from the balance. Otherwise the balance would stop matching the
bank, and "Correct my balance" would undo your savings.

So we track two numbers:
```
balance    = everything in the account          (matches the bank)
saved      = sum of SAVINGS_TRANSFER rows       (money set aside)
spendable  = balance − saved                    (what the budget formula uses)
```

**A bug this prevents:** in March we reserve the ₱2,000 savings goal. On Apr 1
that ₱2,000 is still in the account. If nothing marks it as saved, April's
formula sees it as free money and spreads it over April's weeks, so your
savings get spent. So at **month close**, the goal plus the surplus (if Keep
it) becomes a `SAVINGS_TRANSFER` row.

Decided (D24): keep the bucket so the app works either way, and recommend during
setup that users keep savings apart (an *apartado*/*cajita* or a separate
account).

---

## Tables

### `Transaction`: every movement of money
| Field | Type | Notes |
|---|---|---|
| id | Long | auto |
| date | LocalDate | when it happened |
| amount | Long (cents) | **positive = money in, negative = money out** |
| type | enum | `EXPENSE`, `INCOME`, `FIXED_PAYMENT`, `BALANCE_CORRECTION`, `SAVINGS_TRANSFER` |
| note | String? | "Tacos", "Freelance gig" |
| incomeId | Long? | which `RecurringIncome` created it, null otherwise |
| paymentId | Long? | which `FixedPayment` created it (auto-deducted, D15), null otherwise |

One-off extra income (D7) = an `INCOME` row with no `incomeId`.
No categories (D23).

### `RecurringIncome`
| Field | Type | Notes |
|---|---|---|
| id | Long | auto |
| name | String | "Beca", "Salary" |
| amount | Long (cents) | |
| schedule | Schedule | embedded, see below |
| endedOn | LocalDate? | null = active |

### `FixedPayment`
| Field | Type | Notes |
|---|---|---|
| id | Long | auto |
| name | String | "Rent", "Claude subscription" |
| amount | Long (cents) | |
| schedule | Schedule | embedded, see below |
| cancelledOn | LocalDate? | D15: null = active. A date instead of deleting, so past deductions keep their link |

### `Schedule`: a shared piece embedded in both tables
You chose separate tables (D23). They can still **share** the schedule: Room's
`@Embedded` puts these columns inside each table, and the "when is the next
date?" logic is written once for `Schedule`, not twice.
| Field | Type | Notes |
|---|---|---|
| type | enum | `END_OF_MONTH`, `START_OF_MONTH`, `BIWEEKLY`, `EVERY_N_DAYS`, `DAY_OF_MONTH` |
| everyNDays | Int? | only `EVERY_N_DAYS` |
| dayOfMonth | Int? | only `DAY_OF_MONTH`, 1–31. What about the 31st in a 30-day month? Proposal: use the last day |
| anchorDate | LocalDate | start date; for `EVERY_N_DAYS`, the first occurrence |

### `Segment`: a frozen piece of a week (D3, D17, D20)
| Field | Type | Notes |
|---|---|---|
| start | LocalDate | primary key; the week start, or the 1st of the month |
| end | LocalDate | inclusive |
| budget | Long (cents) | frozen when created; changed only by corrections (D18) |

`weekBudget` = sum of the week's segments. We store them because they're frozen
(D3), and to compare this week to last week for the messages (D6).

### `MonthReview` (D9, D21)
| Field | Type | Notes |
|---|---|---|
| month | YearMonth | primary key |
| savingsGoal | Long (cents) | the goal that month |
| surplus | Long (cents) | extra beyond the goal |
| choice | enum? | `KEEP` / `SPEND`; null = not reviewed (the default from Settings applies) |

### `Settings`: one row
| Field | Type | Notes |
|---|---|---|
| weekStartDay | enum | `MONDAY` / `SUNDAY` (D2) |
| monthlySavingsGoal | Long (cents) | |
| defaultReviewChoice | enum | `KEEP` (default) / `SPEND` (D21) |

---

## Why money is `Long` cents, not `Double`
`0.1 + 0.2` in a `Double` is `0.30000000000000004`. For money that's a real bug.
₱123.45 is stored as `12345`. We only format it as pesos when displaying it.
