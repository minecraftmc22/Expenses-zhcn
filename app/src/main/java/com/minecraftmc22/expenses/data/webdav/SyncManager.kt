package com.minecraftmc22.expenses.data.webdav

import com.minecraftmc22.expenses.data.backup.BackupContent
import com.minecraftmc22.expenses.data.backup.BackupManager
import com.minecraftmc22.expenses.data.model.Expense
import com.minecraftmc22.expenses.data.model.Tag
import com.minecraftmc22.expenses.data.store.DataStore
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers.io

/** What one sync did, shown to the user afterwards. */
data class SyncSummary(
    val added: Int,
    val updated: Int,
    val total: Int
)

/**
 * Two way merge with a WebDAV file, per expense and by modification time.
 *
 * Matching relies on the uuid rather than on the row id, because row ids differ between devices
 * and between the local and the cloud store. The newer side wins; records that only exist on one
 * side are copied to the other.
 *
 * Known limitation: deleting an expense does not propagate. There are no tombstones, so a record
 * that was deleted here comes back on the next sync as long as the other side still has it.
 */
class SyncManager(
    private val dataStore: DataStore,
    private val backupManager: BackupManager,
    private val client: WebDavClient
) {

    fun sync(): Single<SyncSummary> {
        return Single.zip(
            dataStore.getExpenses(),
            dataStore.getTags(),
            BiFunction<List<Expense>, List<Tag>, Pair<List<Expense>, List<Tag>>> { expenses, tags ->
                Pair(expenses, tags)
            }
        )
            .flatMap { local: Pair<List<Expense>, List<Tag>> ->
                // An empty document is a valid answer: the server has no file yet. It has to be
                // a string, because a Single must never emit null.
                Single.fromCallable { client.download().orEmpty() }
                    .map { document ->
                        if (document.isBlank()) {
                            BackupContent(emptyList(), emptyList())
                        } else {
                            backupManager.read(document)
                        }
                    }
                    .flatMap { remote -> merge(local.first, local.second, remote) }
            }
            .subscribeOn(io())
    }

    private fun merge(
        localExpenses: List<Expense>,
        localTags: List<Tag>,
        remote: BackupContent
    ): Single<SyncSummary> {
        val remoteByUuid = remote.expenses
            .mapNotNull { expense -> expense.uuid?.let { it to expense } }
            .toMap()

        val merged = mutableListOf<Expense>()
        val toUpdate = mutableListOf<Pair<String, Expense>>()

        localExpenses.forEach { local ->
            val remoteExpense = local.uuid?.let { remoteByUuid[it] }

            if (remoteExpense != null && modifiedAt(remoteExpense) > modifiedAt(local)) {
                merged += remoteExpense
                toUpdate += local.id to remoteExpense
            } else {
                merged += local
            }
        }

        val localUuids = localExpenses.mapNotNull { it.uuid }.toSet()

        val toDownload = remote.expenses.filter { expense ->
            expense.uuid == null || expense.uuid !in localUuids
        }
        merged += toDownload

        val document = backupManager.write(localTags, merged)

        return Single.fromCallable { client.upload(document) }
            .flatMapCompletable { applyChanges(toDownload, toUpdate, localTags) }
            .toSingleDefault(SyncSummary(toDownload.size, toUpdate.size, merged.size))
    }

    private fun applyChanges(
        toDownload: List<Expense>,
        toUpdate: List<Pair<String, Expense>>,
        localTags: List<Tag>
    ): Completable {
        if (toDownload.isEmpty() && toUpdate.isEmpty()) return Completable.complete()

        return Completable.fromAction {
            val tagsByName = localTags.associateBy { it.name }.toMutableMap()

            toDownload.forEach { expense ->
                dataStore.insertExpense(
                    expense.copy(id = "", tags = resolveTags(expense.tags, tagsByName))
                ).blockingGet()
            }

            toUpdate.forEach { (localId, remoteExpense) ->
                dataStore.updateExpense(
                    remoteExpense.copy(
                        id = localId,
                        tags = resolveTags(remoteExpense.tags, tagsByName)
                    )
                ).blockingAwait()
            }
        }.subscribeOn(io())
    }

    /** Tags travel by name, so a tag that only the other side knows about is created here. */
    private fun resolveTags(tags: List<Tag>, tagsByName: MutableMap<String, Tag>): List<Tag> {
        return tags.map { tag ->
            tagsByName.getOrPut(tag.name) {
                Tag(dataStore.insertTag(Tag("", tag.name)).blockingGet(), tag.name)
            }
        }
    }

    private fun modifiedAt(expense: Expense) =
        expense.modifiedAt ?: expense.timestamp ?: 0L
}
