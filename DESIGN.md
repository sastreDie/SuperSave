# SuperSave: Design Decisions

Living document. Each decision records **who decided** and **why**, so the
final report can explain the design. Open questions are at the bottom.

Stack (from the proposal): Kotlin, Jetpack Compose, Room, Jetpack Glance (widget),
WorkManager. Offline only, data stays on the phone. Test device: Pixel 10.

---

## Decided

### D1. Weekly budget instead of daily (Diego)
A daily limit punishes normal life (one big groceries day = "failure").
A week evens out spending.

### D2. Week start is user-selectable: Monday or Sunday (Diego)
Only those two options. Default: Monday.

### D3. The weekly budget is fixed at week start; "left this week" updates live (Diego)
- At the start of each week the app calculates **this week's budget** once.
- Every expense lowers **left this week** immediately.
- The main number is *how much you have left*, not *how much you should spend*.

### D4. Payday mid-week does not change this week's budget (Diego)
The new income counts at the **next** weekly recalculation.
The UI shows a badge: **"Pay day this week!"**

### D5. Surplus and overspending are spread across all remaining weeks (Diego)
Underspending → every remaining week gets a bit more.
Overspending → every remaining week gets a bit less (a gentler hit than taking it all from next week).

### D6. Feedback messages are informative and never negative (Diego)
Compare this week's budget to last week's:
- Up: *"Good job! Your budget increased this week thanks to your spending habits!"*
- Down: *"We took a bit of a hit last week, so this week's budget is a little lower."*
- Tone: rewarding when things go well, neutral and informative otherwise. Never scolding.

### D7. Many income sources (Diego)
- **Recurring incomes**, each with its own schedule (see D8).
- **One-off extra income** ("I made some unexpected money"), added on any day.

### D8. Schedule presets for incomes and fixed payments (Diego)
- End of month
- Start of month
- Biweekly / quincena (15th and end of month)
- Every N days (from a start date)

### D9. Monthly review of savings performance (Diego)
- During the month, surplus flows into the remaining weeks (D5).
- At month end, the user reviews how they did and picks:
  - **Keep it**: the extra goes to savings, or
  - **Spend it**: the extra grows next month's budget.
- Design goal: the important numbers go up, so the user feels good.

### D10. Implementation order (Diego, proposed by Claude)
1. Data model
2. Empty app scaffold that builds (Claude)
3. Budget engine plus tests (Diego and David write the engine)
4. Setup and expense screens
5. Weigh the payment
6. Widget

### D11. AI use stays at Nivel 2 (course rule)
Claude writes boilerplate: project setup, Room plumbing, UI scaffolding, widget
and WorkManager plumbing, test harness. Diego and David write the core: the
budget engine, Weigh the payment logic, schedule rules, data model decisions.
Every AI contribution goes in `AI_USAGE.md`.

---

## Draft formula (to be confirmed, see Q1)

```
freeMoney  = balance
           + expected income before horizon end
           − fixed payments due before horizon end
           − savings goal still to set aside

weekBudget = freeMoney × (days of this week inside horizon ÷ days left in horizon)
leftThisWeek = weekBudget − spent this week
```

Recalculating `weekBudget` at each week start from the *real* balance is what
spreads surplus/overspending across the remaining weeks (D5): no extra
bookkeeping is needed.

---

## Open questions

- **Q1. Planning horizon.** With many incomes, "days until payday" is ambiguous.
  Proposal: use the **calendar month** as the horizon, since the savings goal and
  the review (D9) are already monthly.
- **Q2. Counting income before it arrives.** With a month horizon, does the
  quincena on the 15th count toward the budget on the 1st? Counting it gives
  smoother weeks, but you could spend money you don't have yet if big fixed
  payments come early.
- **Q3. Weeks that cross a month boundary** (e.g. Mon Oct 27 to Sun Nov 2).
  Which month's budget pays for which days?
- **Q4. Where does the balance come from after setup?** Proposal: the user enters
  it once, then the app tracks it (balance = start + incomes − expenses −
  payments), with a "correct my balance" button for when it drifts from the bank.
- **Q5. Are fixed payments auto-deducted on their date,** or does the user mark
  them as paid?
- **Q6. Language of the app UI and the code/docs:** Spanish or English?
