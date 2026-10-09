package com.minecraftmc22.expenses.expensedetail.presentation

import android.annotation.SuppressLint
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.minecraftmc22.expenses.Application
import com.minecraftmc22.expenses.R
import com.minecraftmc22.expenses.data.attachment.AttachmentStore
import com.minecraftmc22.expenses.data.model.Attachment
import com.minecraftmc22.expenses.data.model.Expense
import com.minecraftmc22.expenses.data.model.Tag
import com.minecraftmc22.expenses.expensedetail.domain.DeleteExpenseUseCase
import com.minecraftmc22.expenses.expensedetail.domain.ObserveExpenseUseCase
import com.minecraftmc22.expenses.util.READABLE_DATE_FORMAT
import com.minecraftmc22.expenses.util.extensions.plusAssign
import com.minecraftmc22.expenses.util.extensions.toString
import com.minecraftmc22.expenses.util.reactive.DataEvent
import com.minecraftmc22.expenses.util.reactive.Event
import com.minecraftmc22.expenses.util.reactive.Variable
import io.reactivex.android.schedulers.AndroidSchedulers.mainThread
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers.io

@SuppressLint("LongLogTag")
class ExpenseDetailFragmentModel(
    application: Application,
    private val observeExpenseUseCase: ObserveExpenseUseCase,
    private val deleteExpenseUseCase: DeleteExpenseUseCase,
    private val attachmentStore: AttachmentStore,
    private var expense: Expense
) : AndroidViewModel(application) {

    val amount = Variable("")
    val currency = Variable("")
    val title = Variable("")
    val tags = Variable(emptyList<Tag>())
    val date = Variable("")
    val notes = Variable("")
    val attachments = Variable(emptyList<Attachment>())

    /** Picture drawn behind this screen; empty means "use the global background". */
    val background = Variable("")

    val showEdit = DataEvent<Expense>()
    val finish = Event()

    private val disposables = CompositeDisposable()

    // Lifecycle start

    init {
        observeExpense()
        observeAttachments()
    }

    private fun observeExpense() {
        disposables += observeExpenseUseCase(expense.id)
            .subscribeOn(io())
            .observeOn(mainThread())
            .subscribe({
                expense = it; populateExpenseValues()
            }, { error ->
                Log.w(TAG, "Failed to observe expense: ($error).")
            })
    }

    private fun observeAttachments() {
        disposables += attachmentStore.observeAttachments(expense.id)
            .subscribeOn(io())
            .observeOn(mainThread())
            .subscribe({ attachmentList ->
                attachments.value = attachmentList
                updateBackground()
            }, { error ->
                Log.w(TAG, "Failed to observe attachments: ($error).")
            })
    }

    private fun populateExpenseValues() {
        amount.value = "${"%.2f".format(expense.amount)} ${expense.currency.symbol}"
        currency.value = "(${expense.currency.title} • ${expense.currency.code})"
        title.value = expense.title
        tags.value = expense.tags
        date.value = expense.date.toString(READABLE_DATE_FORMAT)
        notes.value = makeNotes(expense)

        updateBackground()
    }

    /** The picture chosen for this expense, or else its first attached picture. */
    private fun updateBackground() {
        background.value = expense.background
            ?: attachments.value.firstOrNull { it.isImage }?.path
            ?: ""
    }

    private fun makeNotes(expense: Expense): String {
        val notes = expense.notes
        return if (notes.isNotEmpty()) notes
        else getApplication<Application>().getString(R.string.no_notes)
    }

    // Lifecycle end

    override fun onCleared() {
        super.onCleared()
        disposables.clear()
    }

    // Public

    fun edit() {
        showEdit.next(expense)
    }

    fun delete() {
        disposables += deleteExpenseUseCase(expense)
            .andThen(attachmentStore.deleteAttachments(expense.id))
            .subscribeOn(io())
            .observeOn(mainThread())
            .subscribe({
                finish.next()
            }, { error ->
                Log.w(TAG, "Failed to delete expense: ($error).")
            })
    }

    @Suppress("UNCHECKED_CAST")
    class Factory(private val application: Application, private val expense: Expense) :
        ViewModelProvider.NewInstanceFactory() {

        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            val dataStore = application.defaultDataStore

            val observeExpenseUseCase = ObserveExpenseUseCase(dataStore)
            val deleteExpenseUseCase = DeleteExpenseUseCase(dataStore)

            return ExpenseDetailFragmentModel(
                application,
                observeExpenseUseCase,
                deleteExpenseUseCase,
                application.attachmentStore,
                expense
            ) as T
        }
    }

    companion object {
        private const val TAG = "ExpenseDetailFragmentModel"
    }
}