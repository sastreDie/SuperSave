package com.diegodavid.supersave.ui.setup

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.diegodavid.supersave.AppClock
import com.diegodavid.supersave.data.BudgetRepository
import com.diegodavid.supersave.data.FixedPayment
import com.diegodavid.supersave.data.RecurringIncome
import com.diegodavid.supersave.data.ReviewChoice
import com.diegodavid.supersave.data.Schedule
import com.diegodavid.supersave.data.SetupData
import com.diegodavid.supersave.data.SuperSaveDatabase
import com.diegodavid.supersave.data.TestData
import com.diegodavid.supersave.ui.parseMoney
import kotlinx.coroutines.launch
import java.time.DayOfWeek

/** The setup screens, in order. */
enum class SetupStep { WELCOME, BALANCE, INCOMES, PAYMENTS, SAVINGS }

/**
 * Holds what the user has typed during setup. A ViewModel survives screen rotation,
 * so nothing is lost if the phone turns. (UI plumbing by Claude, Nivel 2.)
 */
class SetupViewModel(app: Application) : AndroidViewModel(app) {
    private val db = SuperSaveDatabase.get(app)
    private val repository = BudgetRepository(db)

    var step by mutableStateOf(SetupStep.WELCOME)
        private set

    var weekStart by mutableStateOf(DayOfWeek.MONDAY)
    var balanceText by mutableStateOf("")
    /** D36: how much of the balance is savings. Empty = none. */
    var savingsText by mutableStateOf("")
    val incomes = mutableStateListOf<RecurringIncome>()
    val payments = mutableStateListOf<FixedPayment>()
    var goalText by mutableStateOf("")
    var reviewChoice by mutableStateOf(ReviewChoice.KEEP)

    /** True while saving, so a double tap on Finish can't save twice. */
    var saving by mutableStateOf(false)
        private set

    val stepNumber: Int get() = step.ordinal + 1
    val stepCount: Int = SetupStep.entries.size
    val isLastStep: Boolean get() = step == SetupStep.entries.last()

    /** The balance can be zero or negative (D25); it just has to be a number. */
    val balance: Long? get() = parseMoney(balanceText)

    /** Savings can't be negative or more than the money you have. */
    val savings: Long?
        get() = if (savingsText.isBlank()) 0L
        else parseMoney(savingsText)?.takeIf { it >= 0 && it <= (balance ?: 0L).coerceAtLeast(0L) }

    /** An empty goal means "no goal" (₱0). A goal can't be negative. */
    val goal: Long? get() = if (goalText.isBlank()) 0L else parseMoney(goalText)?.takeIf { it >= 0 }

    val canContinue: Boolean
        get() = !saving && when (step) {
            SetupStep.BALANCE -> balance != null && savings != null
            SetupStep.SAVINGS -> goal != null
            else -> true
        }

    fun next() {
        if (!canContinue) return
        if (isLastStep) finish() else step = SetupStep.entries[step.ordinal + 1]
    }

    /** Returns false on the first screen, so the system back button closes the app. */
    fun back(): Boolean {
        if (step == SetupStep.WELCOME) return false
        step = SetupStep.entries[step.ordinal - 1]
        return true
    }

    fun addIncome(name: String, amount: Long, schedule: Schedule) {
        incomes.add(RecurringIncome(name = name, amount = amount, schedule = schedule))
    }

    fun addPayment(name: String, amount: Long, schedule: Schedule) {
        payments.add(FixedPayment(name = name, amount = amount, schedule = schedule))
    }

    private fun finish() {
        val balance = balance ?: return
        val goal = goal ?: return
        saving = true
        viewModelScope.launch {
            try {
                repository.completeSetup(
                    SetupData(
                        weekStart = weekStart,
                        startingBalance = balance,
                        startingSavings = savings ?: 0L,
                        incomes = incomes.toList(),
                        payments = payments.toList(),
                        monthlySavingsGoal = goal,
                        defaultReviewChoice = reviewChoice,
                    ),
                    today = AppClock.today(),
                )
                // No navigation needed: MainActivity watches the settings row and
                // switches to the home screen once it exists.
            } finally {
                saving = false
            }
        }
    }

    /** TEMPORARY: skip setup with the fake November data. */
    fun loadTestData() {
        viewModelScope.launch { TestData.load(db) }
    }
}
