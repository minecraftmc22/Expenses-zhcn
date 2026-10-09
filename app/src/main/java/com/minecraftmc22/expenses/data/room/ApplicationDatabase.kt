package com.minecraftmc22.expenses.data.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.minecraftmc22.expenses.data.room.converter.CurrencyConverter
import com.minecraftmc22.expenses.data.room.converter.LocalDateConverter
import com.minecraftmc22.expenses.data.room.dao.AttachmentDao
import com.minecraftmc22.expenses.data.room.dao.ExpenseDao
import com.minecraftmc22.expenses.data.room.dao.ExpenseTagJoinDao
import com.minecraftmc22.expenses.data.room.dao.TagDao
import com.minecraftmc22.expenses.data.room.entities.AttachmentEntity
import com.minecraftmc22.expenses.data.room.entities.ExpenseEntity
import com.minecraftmc22.expenses.data.room.entities.ExpenseTagJoinEntity
import com.minecraftmc22.expenses.data.room.entities.TagEntity

@Database(
    entities = [
        ExpenseEntity::class,
        ExpenseTagJoinEntity::class,
        TagEntity::class,
        AttachmentEntity::class
    ],
    version = 3,
    exportSchema = true
)
@TypeConverters(
    value = [
        CurrencyConverter::class,
        LocalDateConverter::class
    ]
)
abstract class ApplicationDatabase : RoomDatabase() {

    abstract fun expenseDao(): ExpenseDao

    abstract fun tagDao(): TagDao

    abstract fun expenseTagJoinDao(): ExpenseTagJoinDao

    abstract fun attachmentDao(): AttachmentDao

    companion object {

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE expenses ADD COLUMN created_at INTEGER NOT NULL DEFAULT 0"
                )
                database.execSQL(
                    "ALTER TABLE expenses ADD COLUMN modified_at INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        /**
         * Adds the per expense background and the local attachment table. The column list, the
         * foreign key and the index have to match what Room generates for the entities above,
         * otherwise opening the database fails its schema validation.
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE expenses ADD COLUMN background TEXT")

                // Rows written before this column existed get a fresh identity, so a WebDAV
                // merge can match them against the same expense on another device.
                database.execSQL(
                    "ALTER TABLE expenses ADD COLUMN uuid TEXT NOT NULL DEFAULT ''"
                )
                database.execSQL(
                    "UPDATE expenses SET uuid = lower(hex(randomblob(16))) WHERE uuid = ''"
                )

                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS attachments (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "expense_id TEXT NOT NULL, " +
                        "path TEXT NOT NULL, " +
                        "name TEXT NOT NULL, " +
                        "mime_type TEXT NOT NULL, " +
                        "created_at INTEGER NOT NULL)"
                )

                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_attachments_expense_id " +
                        "ON attachments (expense_id)"
                )
            }
        }

        private const val DATABASE_NAME = "database"

        fun build(context: Context) =
            Room.databaseBuilder(context, ApplicationDatabase::class.java,
                DATABASE_NAME
            )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .fallbackToDestructiveMigration()
                .build()
    }
}