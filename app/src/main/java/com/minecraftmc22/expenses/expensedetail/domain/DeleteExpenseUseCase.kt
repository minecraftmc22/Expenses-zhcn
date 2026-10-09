package com.minecraftmc22.expenses.expensedetail.domain

import com.minecraftmc22.expenses.data.model.Expense
import com.minecraftmc22.expenses.data.store.DataStore
import io.reactivex.Completable

class DeleteExpenseUseCase(private val dataStore: DataStore) {

    operator fun invoke(expense: Expense): Completable {
        return dataStore.deleteExpense(expense)
    }
}