package com.minecraftmc22.expenses.data.attachment

import android.content.Context
import android.net.Uri
import android.util.Log
import android.webkit.MimeTypeMap
import com.minecraftmc22.expenses.data.model.Attachment
import com.minecraftmc22.expenses.data.room.dao.AttachmentDao
import com.minecraftmc22.expenses.data.room.entities.AttachmentEntity
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers.io
import java.io.File
import java.io.IOException
import java.util.UUID

/**
 * Attachments live on this device only: the data rows are kept in Room and the files in the
 * app's private storage. Nothing here touches Firestore, so attachments neither need the
 * Firebase Storage setup nor a signed in user.
 */
class AttachmentStore(
    private val context: Context,
    private val attachmentDao: AttachmentDao
) {

    fun observeAttachments(expenseId: String): Observable<List<Attachment>> {
        return attachmentDao.observeByExpenseId(expenseId)
            .map { entities -> entities.map { it.mapToAttachment() } }
    }

    fun getAttachments(expenseId: String): Single<List<Attachment>> {
        return attachmentDao.getByExpenseId(expenseId)
            .map { entities -> entities.map { it.mapToAttachment() } }
    }

    /** Drops the rows and files of an expense, used when the expense itself is deleted. */
    fun deleteAttachments(expenseId: String): Completable {
        return Completable.fromAction {
            val paths = attachmentDao.getByExpenseId(expenseId).blockingGet().map { it.path }

            attachmentDao.deleteByExpenseId(expenseId)

            paths.forEach { deleteFile(it) }
        }.subscribeOn(io())
    }

    fun getAllPaths(): Single<List<String>> {
        return attachmentDao.getAll().map { entities -> entities.map { it.path } }
    }

    /**
     * Copies the picked document into the app's storage and returns the attachment, which is
     * not written to the database yet: a new expense has no id until it is inserted, so the
     * rows are written by [saveAttachments] once the expense exists.
     */
    fun importAttachment(uri: Uri): Single<Attachment> {
        return Single.fromCallable {
            val name = queryDisplayName(uri)
            val mimeType = context.contentResolver.getType(uri) ?: mimeTypeFromName(name)

            val target = File(attachmentsDirectory(), UUID.randomUUID().toString())

            context.contentResolver.openInputStream(uri)?.use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            } ?: throw IOException("Could not read the picked document.")

            Attachment(
                id = "",
                path = target.absolutePath,
                name = name,
                mimeType = mimeType
            )
        }.subscribeOn(io())
    }

    /** Makes the stored rows match [attachments] and deletes the files that are gone. */
    fun saveAttachments(expenseId: String, attachments: List<Attachment>): Completable {
        return Completable.fromAction {
            val previousPaths = attachmentDao.getByExpenseId(expenseId)
                .blockingGet()
                .map { it.path }

            attachmentDao.replaceForExpense(
                expenseId,
                attachments.map { AttachmentEntity.prepareForInsertion(expenseId, it) }
            )

            val keptPaths = attachments.map { it.path }
            previousPaths
                .filter { previous -> keptPaths.none { it == previous } }
                .forEach { deleteFile(it) }
        }.subscribeOn(io())
    }

    /** Copies a picture picked as an expense background and returns its path. */
    fun importBackground(uri: Uri): Single<String> {
        return Single.fromCallable {
            val target = File(backgroundsDirectory(), UUID.randomUUID().toString())

            context.contentResolver.openInputStream(uri)?.use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            } ?: throw IOException("Could not read the picked background picture.")

            target.absolutePath
        }.subscribeOn(io())
    }

    fun deleteFile(path: String) {
        val file = File(path)
        if (file.exists() && !file.delete()) {
            Log.w(TAG, "Failed to delete the attachment file.")
        }
    }

    /** Removes files that no attachment row refers to any more. */
    fun deleteOrphanFiles(): Completable {
        return Completable.fromAction {
            val knownPaths = getAllPaths().blockingGet().toSet()

            attachmentsDirectory().listFiles()?.forEach { file ->
                if (knownPaths.none { it == file.absolutePath }) {
                    deleteFile(file.absolutePath)
                }
            }
        }.subscribeOn(io())
    }

    private fun attachmentsDirectory(): File {
        val directory = File(context.filesDir, DIRECTORY_NAME)
        if (!directory.exists() && !directory.mkdirs()) {
            Log.w(TAG, "Failed to create the attachments directory.")
        }

        return directory
    }

    private fun backgroundsDirectory(): File {
        val directory = File(context.filesDir, BACKGROUND_DIRECTORY_NAME)
        if (!directory.exists() && !directory.mkdirs()) {
            Log.w(TAG, "Failed to create the backgrounds directory.")
        }

        return directory
    }

    private fun queryDisplayName(uri: Uri): String {
        val projection = arrayOf(android.provider.OpenableColumns.DISPLAY_NAME)

        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (index >= 0) {
                    cursor.getString(index)?.let { return it }
                }
            }
        }

        return uri.lastPathSegment ?: DEFAULT_NAME
    }

    private fun mimeTypeFromName(name: String): String {
        val extension = name.substringAfterLast('.', "").toLowerCase()
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
            ?: DEFAULT_MIME_TYPE
    }

    companion object {

        private const val TAG = "AttachmentStore"

        private const val DIRECTORY_NAME = "attachments"

        private const val BACKGROUND_DIRECTORY_NAME = "backgrounds"

        private const val DEFAULT_NAME = "attachment"

        private const val DEFAULT_MIME_TYPE = "application/octet-stream"
    }
}
