package com.minecraftmc22.expenses.data.backup

import android.content.Context
import com.minecraftmc22.expenses.common.presentation.Language
import com.minecraftmc22.expenses.common.presentation.Theme
import com.minecraftmc22.expenses.data.model.Currency
import com.minecraftmc22.expenses.data.model.Expense
import com.minecraftmc22.expenses.data.model.Tag
import com.minecraftmc22.expenses.data.preference.PreferenceDataSource
import com.minecraftmc22.expenses.data.store.DataStore
import com.minecraftmc22.expenses.home.presentation.DateRange
import com.minecraftmc22.expenses.util.getCurrentTimestamp
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers.io
import org.json.JSONArray
import org.json.JSONObject
import org.threeten.bp.LocalDate
import org.threeten.bp.format.DateTimeParseException

/** What a backup document holds: the settings live outside of this. */
data class BackupContent(
    val tags: List<Tag>,
    val expenses: List<Expense>
)

/**
 * Turns the user data into a JSON document and back. The same document is used by the backup
 * file and by the WebDAV sync, which is why reading and writing are separated from applying:
 * the sync merges two documents before anything touches the database.
 *
 * Attachments are deliberately not part of it — they are files that live on this device only.
 * It uses `org.json` from the platform, so no JSON library had to be added.
 */
class BackupManager(
    private val context: Context,
    private val dataStore: DataStore,
    private val preferenceDataSource: PreferenceDataSource
) {

    // Document

    fun write(tags: List<Tag>, expenses: List<Expense>): String {
        val backup = JSONObject()
            .put(KEY_VERSION, BACKUP_VERSION)
            .put(KEY_EXPORTED_AT, getCurrentTimestamp())
            .put(KEY_SETTINGS, makeSettings())
            .put(KEY_TAGS, makeTags(tags))
            .put(KEY_EXPENSES, makeExpenses(expenses))

        return backup.toString(INDENT)
    }

    fun read(json: String): BackupContent {
        val backup = JSONObject(json)

        return BackupContent(
            tags = readTags(backup.optJSONArray(KEY_TAGS)),
            expenses = readExpenses(backup.optJSONArray(KEY_EXPENSES))
        )
    }

    // Backup file

    fun export(): Single<String> {
        return Single.zip(
            dataStore.getTags(),
            dataStore.getExpenses(),
            BiFunction<List<Tag>, List<Expense>, String> { tags, expenses ->
                write(tags, expenses)
            }
        ).subscribeOn(io())
    }

    /**
     * Replaces everything with the content of [json]. Deleting first is what makes this a
     * restore rather than a merge, so the caller has to confirm it.
     */
    fun import(json: String): Completable {
        return Completable.fromAction {
            val backup = JSONObject(json)

            restoreSettings(backup.optJSONObject(KEY_SETTINGS))

            dataStore.deleteAllExpenses().blockingAwait()
            dataStore.deleteAllTags().blockingAwait()

            val tagsByName = restoreTags(readTags(backup.optJSONArray(KEY_TAGS)))

            readExpenses(backup.optJSONArray(KEY_EXPENSES)).forEach { expense ->
                dataStore.insertExpense(
                    expense.copy(tags = expense.tags.mapNotNull { tagsByName[it.name] })
                ).blockingGet()
            }
        }.subscribeOn(io())
    }

    // Writing

    private fun makeSettings(): JSONObject {
        val theme = preferenceDataSource.getTheme(context)
        val language = preferenceDataSource.getLanguage(context)
        val currency = preferenceDataSource.getDefaultCurrency(context)
        val dateRange = preferenceDataSource.getDateRange(context)

        return JSONObject()
            .put(KEY_THEME, theme.name)
            .put(KEY_LANGUAGE, language.name)
            .put(KEY_DEFAULT_CURRENCY, currency.code)
            .put(KEY_DATE_RANGE, dateRange.name)
    }

    private fun makeTags(tags: List<Tag>): JSONArray {
        val array = JSONArray()

        tags.forEach { tag ->
            array.put(JSONObject().put(KEY_NAME, tag.name))
        }

        return array
    }

    private fun makeExpenses(expenses: List<Expense>): JSONArray {
        val array = JSONArray()

        expenses.forEach { expense ->
            array.put(
                JSONObject()
                    .put(KEY_AMOUNT, expense.amount)
                    .put(KEY_CURRENCY, expense.currency.code)
                    .put(KEY_TITLE, expense.title)
                    .put(KEY_TAGS, JSONArray(expense.tags.map { it.name }))
                    .put(KEY_DATE, expense.date.toString())
                    .put(KEY_NOTES, expense.notes)
                    .put(KEY_TIMESTAMP, expense.timestamp ?: JSONObject.NULL)
                    .put(KEY_BACKGROUND, expense.background ?: JSONObject.NULL)
                    .put(KEY_UUID, expense.uuid ?: JSONObject.NULL)
                    .put(KEY_MODIFIED_AT, expense.modifiedAt ?: JSONObject.NULL)
            )
        }

        return array
    }

    // Reading

    private fun readTags(array: JSONArray?): List<Tag> {
        if (array == null) return emptyList()

        return (0 until array.length()).mapNotNull { index ->
            array.optJSONObject(index)
                ?.optString(KEY_NAME)
                ?.takeIf { it.isNotEmpty() }
                ?.let { Tag("", it) }
        }
    }

    private fun readExpenses(array: JSONArray?): List<Expense> {
        if (array == null) return emptyList()

        return (0 until array.length()).mapNotNull { index ->
            array.optJSONObject(index)?.let { readExpense(it) }
        }
    }

    private fun readExpense(entry: JSONObject): Expense? {
        val currency = Currency.fromCode(entry.optString(KEY_CURRENCY)) ?: return null
        val date = parseDate(entry.optString(KEY_DATE)) ?: return null

        return Expense(
            id = "",
            amount = entry.optDouble(KEY_AMOUNT, 0.0),
            currency = currency,
            title = entry.optString(KEY_TITLE),
            tags = readTagNames(entry).map { Tag("", it) },
            date = date,
            notes = entry.optString(KEY_NOTES),
            timestamp = if (entry.isNull(KEY_TIMESTAMP)) null else entry.optLong(KEY_TIMESTAMP),
            background = if (entry.isNull(KEY_BACKGROUND)) null else entry.optString(KEY_BACKGROUND),
            uuid = if (entry.isNull(KEY_UUID)) null else entry.optString(KEY_UUID),
            modifiedAt = if (entry.isNull(KEY_MODIFIED_AT)) null else entry.optLong(KEY_MODIFIED_AT)
        )
    }

    private fun readTagNames(entry: JSONObject): List<String> {
        val names = entry.optJSONArray(KEY_TAGS) ?: return emptyList()

        return (0 until names.length())
            .map { names.optString(it) }
            .filter { it.isNotEmpty() }
    }

    private fun parseDate(value: String): LocalDate? {
        return try {
            LocalDate.parse(value)
        } catch (error: DateTimeParseException) {
            null
        }
    }

    // Applying

    private fun restoreSettings(settings: JSONObject?) {
        if (settings == null) return

        settings.optString(KEY_THEME)
            .takeIf { it.isNotEmpty() }
            ?.let { name -> valueOrNull<Theme>(name) }
            ?.let { preferenceDataSource.setTheme(context, it) }

        settings.optString(KEY_LANGUAGE)
            .takeIf { it.isNotEmpty() }
            ?.let { name -> valueOrNull<Language>(name) }
            ?.let { preferenceDataSource.setLanguage(context, it) }

        settings.optString(KEY_DEFAULT_CURRENCY)
            .takeIf { it.isNotEmpty() }
            ?.let { code -> Currency.fromCode(code) }
            ?.let { preferenceDataSource.setDefaultCurrency(context, it) }

        settings.optString(KEY_DATE_RANGE)
            .takeIf { it.isNotEmpty() }
            ?.let { name -> valueOrNull<DateRange>(name) }
            ?.let { preferenceDataSource.setDateRange(context, it) }
    }

    private fun restoreTags(tags: List<Tag>): Map<String, Tag> {
        val tagsByName = mutableMapOf<String, Tag>()

        tags.forEach { tag ->
            val id = dataStore.insertTag(Tag("", tag.name)).blockingGet()
            tagsByName[tag.name] = Tag(id, tag.name)
        }

        return tagsByName
    }

    /** Unknown names are ignored so a backup from a newer version does not break the import. */
    private inline fun <reified T : Enum<T>> valueOrNull(name: String): T? {
        return try {
            enumValueOf<T>(name)
        } catch (error: IllegalArgumentException) {
            null
        }
    }

    companion object {

        private const val BACKUP_VERSION = 1

        private const val INDENT = 2

        private const val KEY_VERSION = "version"
        private const val KEY_EXPORTED_AT = "exported_at"
        private const val KEY_SETTINGS = "settings"
        private const val KEY_TAGS = "tags"
        private const val KEY_EXPENSES = "expenses"

        private const val KEY_THEME = "theme"
        private const val KEY_LANGUAGE = "language"
        private const val KEY_DEFAULT_CURRENCY = "default_currency"
        private const val KEY_DATE_RANGE = "date_range"

        private const val KEY_NAME = "name"
        private const val KEY_AMOUNT = "amount"
        private const val KEY_CURRENCY = "currency"
        private const val KEY_TITLE = "title"
        private const val KEY_DATE = "date"
        private const val KEY_NOTES = "notes"
        private const val KEY_TIMESTAMP = "timestamp"
        private const val KEY_BACKGROUND = "background"
        private const val KEY_UUID = "uuid"
        private const val KEY_MODIFIED_AT = "modified_at"
    }
}
