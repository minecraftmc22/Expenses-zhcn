package com.minecraftmc22.expenses.data.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import org.threeten.bp.LocalDate

@Parcelize
data class Expense(
    val id: String,
    val amount: Double,
    val currency: Currency,
    val title: String,
    val tags: List<Tag>,
    val date: LocalDate,
    val notes: String,
    val timestamp: Long?,
    /**
     * Path of the picture drawn behind this expense. Null means "not set", in which case the
     * detail screen falls back to the first image attachment.
     */
    val background: String? = null,
    /**
     * Identity that survives a trip through a backup or a WebDAV file. Row ids are per device
     * (and per data store), so merging two devices by id would duplicate everything.
     */
    val uuid: String? = null,
    /** When this expense was last written, used to pick the winner while merging. */
    val modifiedAt: Long? = null
) : Parcelable, Comparable<Expense> {

    /**
     * Compares expenses based on date and timestamp.
     */
    override fun compareTo(other: Expense): Int {
        return if (date == other.date) {
            compareByTimestamp(other)
        } else {
            compareByDate(other)
        }
    }

    private fun compareByTimestamp(other: Expense) = when {
        // They are both the same.
        timestamp == other.timestamp -> 0
        // One has not been confirmed yet, so it's newer.
        timestamp == null -> 1
        // Other has not been confirmed yet, so it's newer.
        other.timestamp == null -> -1
        // Just compare values.
        else -> timestamp.compareTo(other.timestamp)
    }

    private fun compareByDate(other: Expense) = date.compareTo(other.date)
}