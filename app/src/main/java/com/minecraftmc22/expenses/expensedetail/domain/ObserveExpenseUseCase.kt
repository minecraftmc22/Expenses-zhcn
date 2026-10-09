package com.minecraftmc22.expenses.expensedetail.domain

import com.minecraftmc22.expenses.data.model.Expense
import com.minecraftmc22.expenses.data.store.DataStore
import io.reactivex.Observable

class ObserveExpenseUseCase(private val dataStore: DataStore) {
    operator fun invoke(expenseId: String): Observable<Expense> {
        return dataStore.observeExpense(expenseId)
    }
}