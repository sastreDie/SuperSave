# Data Model

Status: **v2, approved (D23, D24).**
Under Nivel 2 (D11) the data model is the team's decision; Claude drafted it.

These are the tables Room stores on the phone. Database **version 4** (v2 added
`linkedEntryId`, v3 added `reviewed`, v4 added `startedOn`; Room migrates old data automatically).

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

### `LedgerEntry` (table `ledger`): every movement of money
Called `Transaction` in earlier drafts. Renamed because Room already has an
`@Transaction` annotation and `TRANSACTION` is an SQL keyword. Where this doc
says "transaction row", it means a `LedgerEntry`.
| Field | Type | Notes |
|---|---|---|
| id | Long | auto |
| date | LocalDate | when it happened |
| amount | Long (cents) | **positive = money in, negative = money out** |
| type | enum | `EXPENSE`, `INCOME`, `FIXED_PAYMENT`, `BALANCE_CORRECTION`, `SAVINGS_TRANSFER`, `STARTING_MONEY` (D36: the money for spending at setup, spread over 30 days like a payday) |
| note | String? | "Tacos", "Freelance gig" |
| incomeId | Long? | which `RecurringIncome` created it, null otherwise |
| paymentId | Long? | which `FixedPayment` created it (auto-deducted, D15), null otherwise |
| linkedEntryId | Long? | v2: a `SAVINGS_TRANSFER` that belongs to another row, e.g. the "save it" half of extra money (D31). Edited and removed together with it (D34) |

One-off extra money (D7, D31) = an `INCOME` row with no `incomeId`. If saved, plus a
linked `SAVINGS_TRANSFER` of the same amount.
Recurring incomes and fixed payments get their rows automatically when they come
due (D30).
No categories (D23).

### `RecurringIncome`
| Field | Type | Notes |
|---|---|---|
| id | Long | auto |
| name | String | "Beca", "Salary" |
| amount | Long (cents) | |
| schedule | Schedule | embedded, see below |
| note | String? | optional, e.g. "Deposited to Nu" |
| endedOn | LocalDate? | null = active |

### `FixedPayment`
| Field | Type | Notes |
|---|---|---|
| id | Long | auto |
| name | String | "Rent", "Claude subscription" |
| amount | Long (cents) | |
| schedule | Schedule | embedded, see below |
| note | String? | optional, e.g. "Paid with the BBVA card" |
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

### `MonthReview` (D9, D21, D33)
One row per closed month, written when the month closes.
| Field | Type | Notes |
|---|---|---|
| month | YearMonth | primary key |
| savingsGoal | Long (cents) | the goal that month |
| surplus | Long (cents) | what was left of the month's last segment budget; negative if that week was overspent (it came out of the goal) |
| choice | enum? | `KEEP` / `SPEND`: what was done with the surplus. Set to the Settings default at close; the review can change it. null only in rows from before v3 (= `KEEP`) |
| reviewed | Boolean | v3: false until the user has seen the review screen |

**Month close** (D33) writes one `SAVINGS_TRANSFER` dated the month's last day: the
goal (minus any last-week overspend) plus the surplus if `KEEP`. Changing the choice
in the review later writes another `SAVINGS_TRANSFER` dated that day: positive into
savings, negative back out.

### `Settings`: one row
| Field | Type | Notes |
|---|---|---|
| weekStartDay | enum | `MONDAY` / `SUNDAY` (D2) |
| monthlySavingsGoal | Long (cents) | |
| defaultReviewChoice | enum | `KEEP` (default) / `SPEND` (D21) |
| startedOn | LocalDate? | v4: the day setup was finished; in that month the goal counts only from then on (D37) |

### Money coming in (D36)
Not a table: incomes, starting money and money taken out of savings are spread over
30 days when the budget is worked out. Nothing extra is stored, so edits in the
activity log change it automatically.

---

## Why money is `Long` cents, not `Double`
`0.1 + 0.2` in a `Double` is `0.30000000000000004`. For money that's a real bug.
₱123.45 is stored as `12345`. We only format it as pesos when displaying it.
