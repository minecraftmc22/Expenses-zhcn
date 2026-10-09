package com.minecraftmc22.expenses.home.domain

import com.minecraftmc22.expenses.data.model.Expense
import com.minecraftmc22.expenses.home.presentation.DateRange
import com.minecraftmc22.expenses.home.presentation.TagFilter

class FilterExpensesUseCase {

    operator fun invoke(
        expenses: List<Expense>,
        dateRange: DateRange,
        tagFilter: TagFilter?
    ): List<Expense> {
        return expenses.filter { expense ->
            dateRange.contains(expense.date) &&
                    tagFilter?.let { expense.tags.containsAll(it.tags) } ?: true
        }
    }
}