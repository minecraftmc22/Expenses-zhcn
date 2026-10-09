package com.minecraftmc22.expenses.addeditexpense.presentation

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.view.*
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.chip.Chip
import com.minecraftmc22.expenses.R
import com.minecraftmc22.expenses.addeditexpense.presentation.dateselection.DateSelectionDialogFragment
import com.minecraftmc22.expenses.currencyselection.CurrencySelectionActivity
import com.minecraftmc22.expenses.data.model.Attachment
import com.minecraftmc22.expenses.data.model.Currency
import com.minecraftmc22.expenses.data.model.Tag
import com.minecraftmc22.expenses.util.READABLE_DATE_FORMAT
import com.minecraftmc22.expenses.util.loadThumbnail
import com.minecraftmc22.expenses.util.extensions.*
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.fragment_add_edit_expense.*
import org.threeten.bp.LocalDate

class AddEditExpenseFragment : Fragment() {

    private lateinit var activityModel: AddEditExpenseActivityModel
    private lateinit var model: AddEditExpenseFragmentModel

    private val disposables = CompositeDisposable()

    // Lifecycle start

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_add_edit_expense, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupActionBar()
        watchEditTexts()
        addListeners()
        initializeModels()
        bindModels()
        showKeyboard()
    }

    private fun setupActionBar() {
        val actionBar = (requireActivity() as AppCompatActivity).supportActionBar ?: return
        actionBar.title = ""
        actionBar.setDisplayHomeAsUpEnabled(true)
        actionBar.setHomeAsUpIndicator(R.drawable.ic_clear_24dp)
        setHasOptionsMenu(true)
    }

    private fun watchEditTexts() {
        editTextAmount.afterTextChanged {
            model.updateAmount(it.toString().toDoubleOrNull() ?: 0.0)
        }
        editTextTitle.afterTextChanged { model.updateTitle(it.toString()) }
        editTextNotes.afterTextChanged { model.updateNotes(it.toString()) }
    }

    private fun addListeners() {
        textSymbol.setOnClickListener { showCurrencySelection() }
        containerTags.setOnClickListener { showTagSelection() }
        textDate.setOnClickListener { showDateSelection() }
        buttonAddAttachment.setOnClickListener { showAttachmentPicker() }
        buttonChooseExpenseBackground.setOnClickListener { showBackgroundPicker() }
        buttonClearExpenseBackground.setOnClickListener { model.clearBackground() }
    }

    private fun showCurrencySelection() {
        CurrencySelectionActivity.start(this, REQUEST_CODE_SELECT_CURRENCY)
    }

    private fun showTagSelection() {
        NavHostFragment.findNavController(this).navigate(R.id.action_tag_selection)
    }

    private fun showDateSelection() {
        DateSelectionDialogFragment.newInstance().apply {
            dateSelected = { y, m, d -> model.selectDate(y, m, d) }
        }.show(requireFragmentManager(), DateSelectionDialogFragment.TAG)
    }

    private fun showAttachmentPicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = ANY_MIME_TYPE
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }

        startActivityForResult(intent, REQUEST_CODE_ADD_ATTACHMENTS)
    }

    private fun showBackgroundPicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = IMAGE_MIME_TYPE
        }

        startActivityForResult(intent, REQUEST_CODE_CHOOSE_BACKGROUND)
    }

    private fun initializeModels() {
        activityModel = ViewModelProviders.of(requireActivity())
            .get(AddEditExpenseActivityModel::class.java)

        val args = arguments?.let { AddEditExpenseFragmentArgs.fromBundle(it) }
        val factory =
            AddEditExpenseFragmentModel.Factory(requireContext().application, args?.expense)
        model = ViewModelProviders.of(this, factory).get(AddEditExpenseFragmentModel::class.java)
    }

    private fun bindModels() {
        activityModel.selectedTags.observe(this, Observer { model.selectTags(it) })

        disposables += model.selectedCurrency
            .toObservable()
            .subscribe { updateCurrencyText(it) }
        disposables += model.selectedDate.toObservable().subscribe { updateDateText(it) }
        disposables += model.selectedTags.toObservable().subscribe { updateTagLayout(it) }
        disposables += model.finish.toObservable().subscribe { finish() }

        disposables += model.attachments.toObservable().subscribe {
            updateAttachments(it)
            updateBackgroundPreview()
        }
        disposables += model.background.toObservable().subscribe { updateBackgroundPreview() }

        editTextAmount.setText(makeEasilyEditableAmount(model.amount))
        editTextTitle.setText(model.title)
        editTextNotes.setText(model.notes)
    }

    // Attachments and background

    private fun updateAttachments(attachments: List<Attachment>) {
        containerAttachments.removeAllViews()
        attachments.forEach { containerAttachments.addView(createAttachmentRow(it)) }

        textNoAttachments.visibility = if (attachments.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun createAttachmentRow(attachment: Attachment): View {
        val density = resources.displayMetrics.density
        val thumbnailSize = (ATTACHMENT_THUMBNAIL_DP * density).toInt()
        val spacing = (ATTACHMENT_SPACING_DP * density).toInt()

        val row = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, spacing / 2, 0, spacing / 2)
        }

        val preview = AppCompatImageView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(thumbnailSize, thumbnailSize)

            val bitmap = if (attachment.isImage) {
                loadThumbnail(attachment.path, thumbnailSize)
            } else {
                null
            }

            if (bitmap != null) {
                scaleType = ImageView.ScaleType.CENTER_CROP
                setImageBitmap(bitmap)
            } else {
                scaleType = ImageView.ScaleType.CENTER
                setImageResource(R.drawable.ic_attachment_24dp)
            }
        }

        val name = TextView(requireContext()).apply {
            text = attachment.name
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.MIDDLE
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                .apply { marginStart = spacing }
        }

        val remove = AppCompatImageView(requireContext()).apply {
            setImageResource(R.drawable.ic_clear_24dp)
            contentDescription = getString(R.string.remove)
            setPadding(spacing / 2, spacing / 2, spacing / 2, spacing / 2)
            setOnClickListener { model.removeAttachment(attachment) }
        }

        row.addView(preview)
        row.addView(name)
        row.addView(remove)

        return row
    }

    private fun updateBackgroundPreview() {
        val path = model.effectiveBackgroundPath
        val bitmap = if (path.isNotEmpty()) loadThumbnail(path, BACKGROUND_PREVIEW_SIZE) else null

        imageExpenseBackground.setImageBitmap(bitmap)
        buttonClearExpenseBackground.isEnabled = model.background.value.isNotEmpty()
    }

    private fun makeEasilyEditableAmount(amount: Double?): String {
        return when {
            amount == null -> ""
            amount - amount.toInt().toDouble() == 0.0 -> amount.toInt().toString()
            else -> amount.toString()
        }
    }

    private fun updateCurrencyText(currency: Currency) {
        val context = requireContext()
        val text = context.getString(
            R.string.currency_abbreviation,
            currency.flag,
            currency.code
        )
        textSymbol.text = text
    }

    private fun updateDateText(date: LocalDate) {
        textDate.text = date.toString(READABLE_DATE_FORMAT)
    }

    private fun updateTagLayout(tags: List<Tag>) {
        updateSelectTagsText(tags.isEmpty())
        updateChipGroup(tags)
    }

    private fun updateSelectTagsText(isVisible: Boolean) {
        text_select_tags.visibility = if (isVisible) View.VISIBLE else View.GONE
    }

    private fun updateChipGroup(tags: List<Tag>) {
        chip_group.removeAllViews()
        tags.forEach { chip_group.addView(createChip(it.name)) }
    }

    private fun createChip(text: String): Chip {
        val chip = Chip(context)
        chip.text = text
        chip.isClickable = false
        return chip
    }

    private fun finish() {
        requireActivity().onBackPressed()
    }

    private fun showKeyboard() {
        showKeyboard(editTextAmount, KEYBOARD_APPEARANCE_DELAY)
    }

    // Lifecycle end

    override fun onDestroyView() {
        super.onDestroyView()
        clearDisposables()
        hideKeyboard()
    }

    private fun clearDisposables() {
        disposables.clear()
    }

    // Options

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_new_expense, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> backSelected()
            R.id.save -> saveSelected()
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun backSelected(): Boolean {
        requireActivity().onBackPressed()
        return true
    }

    private fun saveSelected(): Boolean {
        model.saveExpense()
        return true
    }

    // Results

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode != Activity.RESULT_OK) return

        when(requestCode) {
            REQUEST_CODE_SELECT_CURRENCY -> {
                val currency: Currency? =
                    data?.getParcelableExtra(CurrencySelectionActivity.EXTRA_CURRENCY)
                currency?.let { model.selectCurrency(it) }
            }
            REQUEST_CODE_ADD_ATTACHMENTS -> {
                model.addAttachments(collectPickedUris(data))
            }
            REQUEST_CODE_CHOOSE_BACKGROUND -> {
                data?.data?.let { model.chooseBackground(it) }
            }
        }
    }

    private fun collectPickedUris(data: Intent?): List<Uri> {
        if (data == null) return emptyList()

        val clipData = data.clipData
        if (clipData != null) {
            return (0 until clipData.itemCount).mapNotNull { clipData.getItemAt(it).uri }
        }

        return listOfNotNull(data.data)
    }

    companion object {

        private const val REQUEST_CODE_SELECT_CURRENCY = 1
        private const val REQUEST_CODE_ADD_ATTACHMENTS = 2
        private const val REQUEST_CODE_CHOOSE_BACKGROUND = 3

        private const val ANY_MIME_TYPE = "*/*"
        private const val IMAGE_MIME_TYPE = "image/*"

        private const val BACKGROUND_PREVIEW_SIZE = 360

        private const val ATTACHMENT_THUMBNAIL_DP = 44
        private const val ATTACHMENT_SPACING_DP = 12

        private const val KEYBOARD_APPEARANCE_DELAY = 300L
    }
}
