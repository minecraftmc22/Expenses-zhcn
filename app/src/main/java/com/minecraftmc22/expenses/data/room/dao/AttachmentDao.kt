package com.minecraftmc22.expenses.data.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.minecraftmc22.expenses.data.room.entities.AttachmentEntity
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single

@Dao
interface AttachmentDao {

    @Query("SELECT * FROM attachments WHERE expense_id = :expenseId ORDER BY id")
    fun observeByExpenseId(expenseId: String): Observable<List<AttachmentEntity>>

    @Query("SELECT * FROM attachments WHERE expense_id = :expenseId ORDER BY id")
    fun getByExpenseId(expenseId: String): Single<List<AttachmentEntity>>

    @Query("SELECT * FROM attachments")
    fun getAll(): Single<List<AttachmentEntity>>

    @Insert
    fun insertAll(attachments: List<AttachmentEntity>)

    @Query("DELETE FROM attachments WHERE expense_id = :expenseId")
    fun deleteByExpenseId(expenseId: String)

    @Query("DELETE FROM attachments")
    fun deleteAll(): Completable

    /** Makes the rows of one expense match [attachments] exactly. */
    @Transaction
    fun replaceForExpense(expenseId: String, attachments: List<AttachmentEntity>) {
        deleteByExpenseId(expenseId)

        if (attachments.isNotEmpty()) {
            insertAll(attachments)
        }
    }
}
