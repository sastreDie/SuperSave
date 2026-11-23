package com.diegodavid.supersave.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * The database: lists every table. Room generates the real implementation at build time.
 * Bump `version` whenever a table changes, and add a migration.
 */
@Database(
    entities = [
        LedgerEntry::class,
        RecurringIncome::class,
        FixedPayment::class,
        Segment::class,
        MonthReview::class,
        Settings::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class SuperSaveDatabase : RoomDatabase() {
    abstract fun ledger(): LedgerDao
    abstract fun fixedPayments(): FixedPaymentDao
    abstract fun recurringIncomes(): RecurringIncomeDao
    abstract fun segments(): SegmentDao
    abstract fun settings(): SettingsDao

    companion object {
        // One database for the whole app. The app screen and the widget both call get().
        // @Volatile + synchronized: if two threads ask at the same moment, only one opens it.
        @Volatile
        private var instance: SuperSaveDatabase? = null

        fun get(context: Context): SuperSaveDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    SuperSaveDatabase::class.java,
                    "supersave.db",
                ).build().also { instance = it }
            }
    }
}
