package com.minecraftmc22.expenses.home.domain

import com.minecraftmc22.expenses.data.model.Expense

class SortExpensesUseCase {

    private val comparator by lazy {
        compareByDescending(Expense::date).then(timestampComparator)
    }

    private val timestampComparator: Comparator<Expense> by lazy {
        Comparator<Expense> { first, second ->
            when {
                first.timestamp == null -> -1
                second.timestamp == null -> 1
                first.timestamp == second.timestamp -> 0
                first.timestamp > second.timestamp -> -1
                first.timestamp < second.timestamp -> 1
                else -> 0
            }
        }
    }

    operator fun invoke(expenses: List<Expense>) = expenses.sortedWith(comparator)
}