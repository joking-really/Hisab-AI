package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        AccountEntity::class,
        CustomerEntity::class,
        ProductEntity::class,
        JournalEntryEntity::class,
        JournalLineEntity::class,
        SaleEntity::class,
        SaleItemEntity::class,
        PaymentEntity::class,
        StockMovementEntity::class,
        SequenceCounterEntity::class
    ],
    version = 2,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract val appDao: AppDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Create journal_lines table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS journal_lines (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        journalEntryId INTEGER NOT NULL,
                        accountCode TEXT NOT NULL,
                        debit REAL NOT NULL DEFAULT 0.0,
                        credit REAL NOT NULL DEFAULT 0.0,
                        description TEXT,
                        FOREIGN KEY(journalEntryId) REFERENCES journal_entries(id) ON DELETE CASCADE
                    )
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS index_journal_lines_journalEntryId ON journal_lines(journalEntryId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_journal_lines_accountCode ON journal_lines(accountCode)")

                // Create sales table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS sales (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        parchiNumber TEXT NOT NULL,
                        customerId INTEGER,
                        date INTEGER NOT NULL,
                        totalAmount REAL NOT NULL,
                        paymentType TEXT NOT NULL,
                        status TEXT NOT NULL DEFAULT 'current',
                        dueDate INTEGER,
                        journalEntryId INTEGER,
                        notes TEXT
                    )
                """)
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_sales_parchiNumber ON sales(parchiNumber)")

                // Create sale_items table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS sale_items (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        saleId INTEGER NOT NULL,
                        productSku TEXT NOT NULL,
                        quantity INTEGER NOT NULL,
                        unitPrice REAL NOT NULL,
                        totalPrice REAL NOT NULL,
                        location TEXT NOT NULL,
                        FOREIGN KEY(saleId) REFERENCES sales(id) ON DELETE CASCADE
                    )
                """)

                // Create payments table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS payments (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        paymentNumber TEXT NOT NULL UNIQUE,
                        customerId INTEGER NOT NULL,
                        saleId INTEGER,
                        amount REAL NOT NULL,
                        method TEXT NOT NULL,
                        date INTEGER NOT NULL,
                        notes TEXT,
                        journalEntryId INTEGER
                    )
                """)

                // Create sequence counter table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS sequence_counters (
                        prefix TEXT PRIMARY KEY NOT NULL,
                        next_value INTEGER NOT NULL DEFAULT 1
                    )
                """)
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_sequence_prefix ON sequence_counters(prefix)")

                // Migrate old journal_entries: convert single-entry to line-based
                // For each old journal entry, create 2 journal_lines (debit & credit)
                db.execSQL("""
                    INSERT INTO journal_lines (journalEntryId, accountCode, debit, credit, description)
                    SELECT 
                        id, 
                        COALESCE(debitAccountCode, 'UNKNOWN') as accountCode, 
                        COALESCE(amount, 0.0) as debit, 
                        0.0 as credit, 
                        'Migrated debit (fallback)' as description 
                    FROM journal_entries
                    WHERE debitAccountCode IS NOT NULL
                """)
                db.execSQL("""
                    INSERT INTO journal_lines (journalEntryId, accountCode, debit, credit, description)
                    SELECT 
                        id, 
                        COALESCE(creditAccountCode, 'UNKNOWN') as accountCode, 
                        0.0 as debit, 
                        COALESCE(amount, 0.0) as credit, 
                        'Migrated credit (fallback)' as description 
                    FROM journal_entries
                    WHERE creditAccountCode IS NOT NULL
                """)

                // Remove old columns from journal_entries
                // (SQLite doesn't support DROP COLUMN easily; create new table and swap)
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS journal_entries_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        entryNumber TEXT NOT NULL,
                        date INTEGER NOT NULL,
                        description TEXT NOT NULL,
                        refType TEXT NOT NULL,
                        refId INTEGER,
                        customerId INTEGER
                    )
                """)
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_journal_entries_new_entryNumber ON journal_entries_new(entryNumber)")
                
                db.execSQL("""
                    INSERT INTO journal_entries_new (id, entryNumber, date, description, refType, refId, customerId)
                    SELECT id, entryNumber, date, description, refType, NULL as refId, customerId FROM journal_entries
                """)
                
                db.execSQL("DROP TABLE IF EXISTS journal_entries")
                db.execSQL("ALTER TABLE journal_entries_new RENAME TO journal_entries")

                // Remove balance column from accounts (compute dynamically now)
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS accounts_new (
                        code TEXT PRIMARY KEY NOT NULL,
                        name TEXT NOT NULL,
                        type TEXT NOT NULL,
                        nameUrdu TEXT
                    )
                """)
                db.execSQL("""
                    INSERT INTO accounts_new (code, name, type, nameUrdu)
                    SELECT code, name, type, NULL as nameUrdu FROM accounts
                """)
                db.execSQL("DROP TABLE IF EXISTS accounts")
                db.execSQL("ALTER TABLE accounts_new RENAME TO accounts")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "hisab_ai_database"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
