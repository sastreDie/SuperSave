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

### D4. Money received mid-week counts starting next week (Diego)
This week's budget never changes because money arrived. New income only enters
the calculation at the **next** week start.

**Replaced by D36 (Diego):** money coming in now counts the day it arrives, spread over the next 30 days.

### D5. Surplus and overspending are spread across all remaining weeks of the month (Diego)
Underspending → every remaining week gets a bit more.
Overspending → every remaining week gets a bit less (a gentler hit than taking it all from next week).

### D6. Feedback messages are informative and never negative (Diego)
Compare this week's budget to last week's:
- Up: *"Good job! Your budget increased this week thanks to your spending habits!"*
- Down: *"We took a bit of a hit last week, so this week's budget is a little lower."*
- Tone: rewarding when things go well, neutral and informative otherwise. Never scolding.

**Update:** taken out of the first version (Diego). Seeing the message every time
felt bad. Idea for later: show it once per week, as a speech bubble that fades
after a couple of seconds the first time home is opened in a new segment.
The tone rule still applies to every other message.

### D7. Many income sources (Diego)
- **Recurring incomes**, each with its own schedule (see D8).
- **One-off extra income** ("I made some unexpected money"), added on any day.

### D8. Schedule presets for incomes and fixed payments (Diego)
- End of month
- Start of month
- Biweekly / quincena (15th and end of month)
- Every N days (from a start date)
- Day of month, e.g. "the 10th" (added in D23)

### D9. Monthly review of savings performance (Diego)
- During the month, surplus flows into the remaining weeks (D5).
- At month end, the user reviews how they did and picks:
  - **Keep it**: the extra goes to savings, or
  - **Spend it**: the extra grows next month's budget.
- Design goal: the important numbers go up, so the user feels good.

**Update (built):** months close automatically with the default (D21). Home
then shows "November is over. See how it went", which opens the review: "You saved"
(big), the goal line (and how far short, D25, in the D6 tone), and Keep it / Spend it
for the leftover, set to what the close did. Changing it moves the leftover into or
out of savings that day, and like a correction (D18) the current week feels it.

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

**Update (Nov 22):** after writing the first tables and engine functions by hand,
Diego asked Claude to write the remaining repetitive pieces: the table classes and
the calendar helpers (`Calendar.kt`: week splitting; `ScheduleRules.kt`: schedule
dates). The **money logic** (free money, segment budget, left this week,
corrections, Weigh the payment) stays Diego's: it's the core of the app.

**Update 2 (Nov 22):** after writing `freeMoney` by hand, Diego found the rest of
the money formulas simple and asked Claude to write them (`segmentBudget`,
`weekBudget`, `leftThisWeek`, `correctionForSegment`, `paymentsDueIn`,
`budgetTrend`). Diego designed every formula (D3, D5, D17, D18, D25). He wants to
spend his hands-on time on the technical part, starting with the **widget**,
which he will write himself with Claude explaining.

### D12. The budget is always weekly; the month is the goal (Diego)
The calendar month is the planning horizon: savings goal, fixed payments and
the review (D9) are monthly. But the user only ever sees a **weekly** budget,
and every weekly budget is calculated to protect the monthly goal.

### D13. Heads-up badges for the week (Diego)
- **"Pay day this week!"** when an income is expected this week.
- **"Payment due this week"** when a fixed payment is expected this week
  (e.g. "Claude subscription, the 10th").

### D14. The app tracks the balance; "Correct my balance" is essential (Diego)
The user enters the balance once at setup. From then on:
`balance = starting balance + incomes − expenses − fixed payments`.
**Correct my balance** lets the user type the real bank balance at any time.
It must be easy to find.

### D15. Fixed payments are auto-deducted, and the user can cancel them (Diego)
- Each fixed payment is reserved in the budget and deducted on its date automatically.
- The user can edit or **cancel** one (e.g. cancelled a subscription). From then
  on it is no longer expected; past deductions stay in the history.

### D16. Languages (Diego)
- **App (UI, code, commits): English.** It's how Diego will use it.
- **Assignment documentation (report, AI appendix): Spanish.**
- `DESIGN.md` is a working document in English. Its content goes into the Spanish
  report later.

### D17. A week that crosses a month boundary is split by days (Diego)
Mon Mar 30 to Sun Apr 5: 2 days are paid from March's money and 5 days from
April's. This also means 5-week months don't get thinner weeks: money is
spread over **days left in the month**, not "weeks left".

### D18. Balance corrections count immediately (Diego)
The difference (up or down) is spread right away across the rest of the
month, **including the current week**. This is the one exception to the frozen
weekly budget (D3) and to "income counts next week" (D4): a correction fixes a
mistake, it isn't new money.

### D19. The conservative quincena effect is accepted (Diego)
Only money already received counts (D4), so the first half of the month is
tighter and the second half more generous. Diego chose this as the most
effective way to save. It's a prototype Diego will keep using, so the model can
be changed later if it doesn't work in practice.

**Replaced by D36 (Diego):** money coming in now counts the day it arrives, spread over the next 30 days.

### D20. Each part of a split week freezes at its own start (Diego)
Mon Mar 30 to Sun Apr 5 with salary on Mar 31: March's 2 days freeze on Mar 30,
and April's 5 days freeze on **Apr 1**, after the salary has arrived. The user
sees one week whose "left this week" jumps up on the 1st. The month review (D9)
happens at the end of the **month**, not the end of the week.

**Update (D36):** the split by month still holds for the base budget, but a payday now shows up the day it arrives: the salary on the 30th gives its first day to the 30th and the rest to the next 29 days.

### D21. Month review default is "Keep it", and the user can change it (Diego)
If the month hasn't been reviewed when the new month starts, the surplus is
**kept** as savings. When setting up the savings plan, the user can pick their
default (Keep it / Spend it). Choosing "Spend it" later adds the money back like
a correction (D18).

### D22. English and Spanish from day one (Diego)
All on-screen text lives in `res/values/strings.xml` (English) and
`res/values-es/strings.xml` (Spanish). No hardcoded text in the code. Every new
string gets both versions in the same commit.

### D23. Data model decisions (Diego)
- 5th schedule preset: **day of month** (e.g. "the 10th").
- **No expense categories** in the prototype.
- "Keep it" is stored as a `SAVINGS_TRANSFER` transaction.
- Recurring incomes and fixed payments are **separate tables**.

### D24. Savings bucket, plus a tip to keep savings separate (Diego)
The app tracks saved money as a bucket inside the balance
(`spendable = balance − saved`), so it works whether or not savings leave the
account. During setup, the app **recommends** keeping savings apart, either in
an *apartado*/*cajita* or in a different savings account.

### D25. Free money can be negative (Diego)
If bills plus the savings goal are more than the money you have, `freeMoney`
returns a negative number instead of stopping at zero. People who are in the
red need a saving tool too. The app can then say *how far* from the goal you
are, in an informative tone (D6).

### D26. Money divisions round down (Claude, accepted by Diego)
Splitting money across days leaves fractions of a cent. They always round
**down**: ₱760.869 → ₱760.86, and −₱233.333 → −₱233.34. The leftover cents stay
in free money, so the app never shows more money than there really is.

### D27. Money dated on a segment's first day counts in the next segment (Claude, to confirm)
A segment freezes using only ledger entries dated **before** its start. A salary
that lands on Monday (the week start) or on the 1st waits until the next
segment, which is D4 taken literally. Segments freeze lazily: the first time the
app or the widget asks about the week, using the balance as of the segment's
start, so opening the app on Wednesday gives the same budget as on Monday.

**Replaced by D36 (Diego):** money coming in now counts the day it arrives, spread over the next 30 days.

### D28. Setup dates the starting balance so the first week isn't empty (Claude, to confirm)
Because of D27, a balance dated today wouldn't count until the next segment, so
setting up on a Wednesday would give a ₱0 week. Setup instead dates the starting
balance the day **before** the current segment starts, so this week freezes with
it. Incomes and payments added during setup start counting **from tomorrow**
(`anchorDate` = tomorrow): anything due today or earlier is assumed to be already
part of the balance you typed, so it's never counted twice. Incomes and payments
added later in Settings count from **today**, because the app's balance doesn't
include them yet.

**Replaced by D36/D37:** setup now asks how much of the balance is savings; the rest is starting money, spread over 30 days from the setup day.

### D29. Weigh the payment (Claude, to confirm; D11 lists it as Diego's logic)
Before paying for something, the user types the amount and sees, live:
- **It fits:** how much would be left this week.
- **It doesn't fit:** how much over, and how much lower each later week of the
  month gets (the overspend spread over the days left, D5). In the last week of
  the month, the overspend comes out of this month's savings.
- **It would use savings:** if the payment is more than the free money for the
  rest of the month, how much of the savings goal it uses (D25).

Nothing is saved until **Register payment**, which records it as an expense.
The payment is **never rounded**: it's taken to the exact cent. The only
division is the "each week gets about … less" estimate, which rounds **down**
like every other division in the app (D26, Diego). Messages are informative,
never scolding (D6).

On screen it's called **"Check & record an expense"** (Diego asked for a name that
says it does both), and there's a quick-expense widget that opens it directly.

### D30. Fixed payments and incomes are recorded when the app calculates (Claude, to confirm)
Instead of a WorkManager job, every time the app or the widget calculates, each
fixed payment and recurring income that came due up to today and has no ledger
row yet gets one, dated its due day (D15). Nothing is missed if the phone was off,
and nothing is written twice. Cancelling a payment or ending an income stops it
from that day on. Editing one restarts its schedule from today. The real amount
can still differ (e.g. a smaller paycheck); Correct my balance fixes that (D14).

### D31. Extra money: save it or spend it (Diego)
A button on the home screen adds one-off money that isn't on any schedule (D7).
The user picks, with the Keep it / Spend it setting as the default (D21):
- **Save it:** an `INCOME` row plus a `SAVINGS_TRANSFER` of the same amount. It's
  in the balance but in the savings bucket (D24), so the budget doesn't change.
- **Spend it:** just the `INCOME` row. It becomes spendable money for the month and,
  like any money received mid-week, counts from the **next week** (D4), spread over
  the rest of the month. In the last week of a month it goes into the next month.

### D32. The home screen is a dashboard (Diego)
Biggest: **left this week**. Then three tiles: **left this month** (free money for
the rest of the month, as of today, this week included; D25), **balance** (what
the app thinks is in the account, D14) and **saved** (the savings bucket, D24).
The expense screen shows a smaller version (this week, this month, balance) that
updates as the amount is typed. The summary widget shows the same numbers and
opens the app when tapped. It adapts to its size, from 2×1 (only this week) up to
4×3 (everything, plus the D13 badge).

### D33. Month close moves savings automatically (Claude, to confirm; follows D9, D21, D24)
Found by Diego: the savings goal was set aside during the month but never saved,
so every new month spent it again. Now, when a month ends and nobody reviewed it,
it closes with the default choice (D21), dated its last day:
- The **goal** goes into the savings bucket, minus any overspend in the last week
  (there are no weeks left to spread it over, D29). Closing never takes money
  out of savings.
- The **surplus** is what's left of the month's last segment budget. With Keep it
  it's saved too; with Spend it, it stays spendable and grows next month.
- Money that arrived during that last segment (e.g. salary on the 30th) was never
  part of this month's budget, so it carries over to next month (D20).

With Spend it, unspent money keeps rolling into the next month until it's spent;
only the goal is saved each month. Capping it (e.g. one month only, then saved)
was considered and rejected for now (Diego).

If the app wasn't opened for a while, each missed month closes in order the next
time it calculates. The screen to review a month and change the choice (D9) is
still to be built.

### D34. Activity log, adjustable (Diego asked; details by Claude)
Home lists everything from the last month, newest first: expenses, recurring
incomes, extra money (marked "saved" when it went to savings), fixed payments,
balance corrections and money moved to savings. "Show older" goes back another month.
Tap a line to adjust it:
- **Expenses and extra money:** change the amount or note, or remove them. Saved
  extra money changes or disappears together with its savings half.
- **Recurring incomes and fixed payments:** change that one time's amount (e.g. a
  smaller paycheck). They can't be removed, because the app would record them
  again (D30); set them to $0 if they didn't happen. Settings changes every time.
- **Corrections and moves to savings** are shown but can't be changed: they change
  frozen budgets and savings (D18, D33).

The widgets also redraw once a day just after midnight (WorkManager), so a new
week shows up on the home screen without opening the app.

### D35. Expenses can be dated earlier in the month (Diego asked; limit by Claude)
The expense screen has a date, today by default. Any earlier day of the **current
month** can be picked. Earlier months can't: they're closed and their savings were
already moved (D33); Correct my balance covers a forgotten expense there.
An expense in an earlier week doesn't change this week's money; it comes out of
what's left this month, so the weighing (D29) isn't shown for it.
Home also shows the D25 line when "left this month" is negative: how much short,
and that spending a little less closes the gap.

### D36. Money coming in is spread evenly over the next 30 days (Diego)
Waiting until next week for a payday felt unintuitive. Now every bit of money coming
in counts **the day the app records it**, split into 30 equal daily shares starting
that day, whatever month those days fall in:
- recurring paydays (D30), extra money you spend (D31), money taken back out of
  savings by a month review (D21), and the starting money at setup.
- ₱10,000 on a Sunday: that Sunday gets ₱333.33, each of the next four weeks
  ₱2,333.33, and the 30th day the rest (₱333.34). Rounded down on the running total,
  so the 30 days always add up exactly (D26).
- A week's budget = its frozen base (D3: bills and the goal set aside, D5 leftovers
  and overspending spread over the rest of the month) + its days' share of money
  that has come in. A payday mid-week raises "left this week" right away.
- Shares are worked out each time, not stored, so editing a paycheck in the
  activity log (D34) fixes every week that depends on it.
- Month close (D33) only saves this month's days: the part of a late payday that
  belongs to next month stays spendable. "Left this month" doesn't count it either.
- Corrections (D18) and moving the leftover into savings in a review stay immediate.
- Setup asks **how much of the balance is savings**; it goes straight into the savings
  bucket (D24) and is never budgeted. A tip suggests setting up right after payday,
  at the start of the month.
Replaces D4, D19, D27 and D28. Spreading over the rest of the month instead was
considered: a payday on the 28th would get squeezed into 3 days.

### D37. In the setup month the savings goal counts only from setup (Claude, to confirm)
Setting up on Nov 22 with a ₱1,000 goal asks for ₱1,000 × 9/30 = ₱300 in November,
then the full goal from December. Without this, the whole month's goal came out of
the few days left and the first week started negative.

---

## Formula draft (with D17 and D18)

The unit of calculation is a **segment**: the part of a week that falls inside
one month. A normal week is one segment. A week that crosses a month boundary
is two segments (D17).

When a segment starts:
```
freeMoney     = balance (money already received, D4)
              − fixed payments still due this month (D15)
              − savings goal still to set aside this month

segmentBudget = freeMoney × (days in this segment ÷ days left in the month)
```
The number the user sees:
```
weekBudget   = sum of the week's segment budgets
leftThisWeek = weekBudget − expenses this week
```
On a balance correction (D18):
```
segmentBudget += difference × (days left in this segment ÷ days left in the month)
```

Example: ₱3,000 free on Nov 1 (30 days). A full 7-day week gets
3,000 × 7 ÷ 30 = **₱700**. Spend only ₱500, and next week the free money is
₱2,500 over 23 days → 2,500 × 7 ÷ 23 = **₱761**, which shows "Good job!" (D6).
That is D5 working by itself.

---

## Open questions

None right now.
