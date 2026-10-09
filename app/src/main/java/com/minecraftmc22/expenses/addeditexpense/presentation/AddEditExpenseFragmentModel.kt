package com.minecraftmc22.expenses.addeditexpense.presentation

import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.minecraftmc22.expenses.Application
import com.minecraftmc22.expenses.data.attachment.AttachmentStore
import com.minecraftmc22.expenses.data.model.Attachment
import com.minecraftmc22.expenses.data.model.Currency
import com.minecraftmc22.expenses.data.model.Expense
import com.minecraftmc22.expenses.data.model.Tag
import com.minecraftmc22.expenses.data.preference.PreferenceDataSource
import com.minecraftmc22.expenses.data.store.DataStore
import com.minecraftmc22.expenses.util.extensions.plusAssign
import com.minecraftmc22.expenses.util.reactive.Event
import com.minecraftmc22.expenses.util.reactive.Variable
import io.reactivex.android.schedulers.AndroidSchedulers.mainThread
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers.io
import org.threeten.bp.LocalDate

class AddEditExpenseFragmentModel(
    application: Application,
    private val dataStore: DataStore,
    private val attachmentStore: AttachmentStore,
    private val preferenceDataSource: PreferenceDataSource,
    private val expense: Expense?
) : AndroidViewModel(application) {

    var amount: Double? = null
    var title: String = ""
    var notes: String = ""

    val selectedCurrency = Variable(Currency.USD)
    val selectedDate = Variable(LocalDate.now())
    val selectedTags = Variable(emptyList<Tag>())
    val attachments = Variable(emptyList<Attachment>())

    /** Empty string means "not set", in which case the first attached picture is used. */
    val background = Variable("")

    val finish = Event()

    private val disposables = CompositeDisposable()

    // Lifecycle start

    init {
        if (expense == null) {
            setDefaultCurrency()
        } else {
            populateData(expense)
            loadAttachments(expense.id)
        }
    }

    private fun setDefaultCurrency() {
        getApplication<Application>().let {
            selectedCurrency.value = preferenceDataSource.getDefaultCurrency(it)
        }
    }

    private fun populateData(expense: Expense) {
        amount = expense.amount
        title = expense.title
        notes = expense.notes
        background.value = expense.background.orEmpty()

        selectedCurrency.value = expense.currency
        selectedDate.value = expense.date
        selectedTags.value = expense.tags
    }

    // Attachments and background

    private fun loadAttachments(expenseId: String) {
        disposables += attachmentStore.getAttachments(expenseId)
            .observeOn(mainThread())
            .subscribe({ attachments.value = it }, { error ->
                Log.w(TAG, "Failed to load the attachments (${error.message}).")
            })
    }

    fun addAttachments(uris: List<Uri>) {
        uris.forEach { uri ->
            disposables += attachmentStore.importAttachment(uri)
                .observeOn(mainThread())
                .subscribe({ attachment ->
                    attachments.value = attachments.value + attachment
                }, { error ->
                    Log.w(TAG, "Failed to import an attachment (${error.message}).")
                })
        }
    }

    fun removeAttachment(attachment: Attachment) {
        attachments.value = attachments.value - attachment

        if (background.value == attachment.path) {
            background.value = ""
        }
    }

    fun chooseBackground(uri: Uri) {
        disposables += attachmentStore.importBackground(uri)
            .observeOn(mainThread())
            .subscribe({ path ->
                background.value = path
            }, { error ->
                Log.w(TAG, "Failed to import the background picture (${error.message}).")
            })
    }

    fun clearBackground() {
        background.value = ""
    }

    /** The picture this expense shows: the chosen one, or else the first attached picture. */
    val effectiveBackgroundPath: String
        get() = background.value.ifEmpty {
            attachments.value.firstOrNull { it.isImage }?.path.orEmpty()
        }

    // Lifecycle end

    override fun onCleared() {
        super.onCleared()
        disposables.clear()
    }

    // Selection

    fun selectCurrency(currency: Currency) {
        selectedCurrency.value = currency
    }

    fun selectTags(tags: List<Tag>) {
        selectedTags.value = tags
    }

    fun selectDate(year: Int, month: Int, day: Int) {
        selectedDate.value = LocalDate.of(year, month, day)
    }

    // Updating

    fun updateAmount(amount: Double) {
        this.amount = amount
    }

    fun updateTitle(title: String) {
        this.title = title
    }

    fun updateNotes(notes: String) {
        this.notes = notes
    }

    // Saving

    fun saveExpense() {
        if (expense == null) {
            createExpense()
        } else {
            updateExpense(expense)
        }
    }

    private fun createExpense() {
        val expenseForInsertion = prepareExpenseForInsertion()

        disposables += dataStore.insertExpense(expenseForInsertion)
            .flatMapCompletable { id ->
                attachmentStore.saveAttachments(id, attachments.value)
            }
            .subscribeOn(io())
            .observeOn(mainThread())
            .doOnTerminate { finish.next() }
            .subscribe({
                Log.d(TAG, "Expense insertion succeeded.")
            }, { error ->
                Log.e(TAG, "Expense insertion failed (${error.message}).")
            })
    }

    private fun prepareExpenseForInsertion(): Expense {
        return Expense(
            "",
            amount ?: 0.0,
            selectedCurrency.value,
            title,
            selectedTags.value,
            selectedDate.value,
            notes,
            null,
            background.value.ifEmpty { null }
        )
    }

    private fun updateExpense(expense: Expense) {
        val expenseForUpdate = prepareExpenseForUpdate(expense)

        disposables += dataStore.updateExpense(expenseForUpdate)
            .andThen(attachmentStore.saveAttachments(expense.id, attachments.value))
            .subscribeOn(io())
            .observeOn(mainThread())
            .doOnTerminate { finish.next() }
            .subscribe({
                Log.d(TAG, "Expense update succeeded.")
            }, { error ->
                Log.d(TAG, "Expense update failed (${error.message}.")
            })
    }

    private fun prepareExpenseForUpdate(expense: Expense): Expense {
        return expense.copy(
            amount = amount ?: 0.0,
            currency = selectedCurrency.value,
            title = title,
            tags = selectedTags.value,
            date = selectedDate.value,
            notes = notes,
            background = background.value.ifEmpty { null }
        )
    }

    @Suppress("UNCHECKED_CAST")
    class Factory(
        private val application: Application,
        private val expense: Expense?
    ) : ViewModelProvider.NewInstanceFactory() {

        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return AddEditExpenseFragmentModel(
                application,
                application.defaultDataStore,
                application.attachmentStore,
                application.preferenceDataSource,
                expense
            ) as T
        }
    }

    companion object {
        private val TAG = AddEditExpenseFragmentModel::class.java.simpleName
    }
}