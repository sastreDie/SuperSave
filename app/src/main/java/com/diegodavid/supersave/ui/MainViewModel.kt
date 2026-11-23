package com.diegodavid.supersave.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.diegodavid.supersave.data.BudgetRepository
import com.diegodavid.supersave.data.FixedPayment
import com.diegodavid.supersave.data.HomeData
import com.diegodavid.supersave.data.RecurringIncome
import com.diegodavid.supersave.data.Settings
import com.diegodavid.supersave.data.SuperSaveDatabase
import com.diegodavid.supersave.data.TestData
import com.diegodavid.supersave.engine.Weighing
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.diegodavid.supersave.AppClock

/**
 * Shared by the screens after setup: home, weigh, correct balance, settings.
 * Screens read the StateFlows and call the functions; the repository does the work.
 * The widget updates by itself after every write (SuperSaveApp). (Plumbing by Claude.)
 */
class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val db = SuperSaveDatabase.get(app)
    private val repository = BudgetRepository(db)

    // stateIn turns a Flow into a value the screen can read at any time.
    // WhileSubscribed(5000): keeps listening 5 s after the screen goes away (e.g. rotation).
    private fun <T> kotlinx.coroutines.flow.Flow<T>.asState(initial: T): StateFlow<T> =
        stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initial)

    val home: StateFlow<HomeData?> = repository.observeHome().asState(null)
    val settings: StateFlow<Settings?> = repository.observeSettings().asState(null)
    val incomes: StateFlow<List<RecurringIncome>> = repository.observeIncomes().asState(emptyList())
    val payments: StateFlow<List<FixedPayment>> = repository.observePayments().asState(emptyList())

    private fun today() = AppClock.today()
    private fun run(block: suspend () -> Unit) { viewModelScope.launch { block() } }

    suspend fun weigh(amount: Long): Weighing? = repository.weigh(amount, today())

    fun addExpense(amount: Long, note: String?) = run { repository.addExpense(today(), amount, note) }
    fun deleteExpense(id: Long) = run { repository.deleteExpense(id) }
    fun addSurpriseMoney(amount: Long, note: String?, save: Boolean) =
        run { repository.addSurpriseMoney(amount, note, save, today()) }
    fun correctBalance(real: Long) = run { repository.correctBalance(real, today()) }

    fun saveSettings(settings: Settings) = run { repository.saveSettings(settings) }
    fun addIncome(income: RecurringIncome) = run { repository.addIncome(income) }
    fun updateIncome(income: RecurringIncome) = run { repository.updateIncome(income) }
    fun endIncome(id: Long) = run { repository.endIncome(id, today()) }
    fun addPayment(payment: FixedPayment) = run { repository.addPayment(payment) }
    fun updatePayment(payment: FixedPayment) = run { repository.updatePayment(payment) }
    fun cancelPayment(id: Long) = run { repository.cancelPayment(id, today()) }

    // TEMPORARY debug tools.
    fun loadTestData() = run { TestData.load(db) }
    fun clearAll() = run { TestData.clear(db) }
}
