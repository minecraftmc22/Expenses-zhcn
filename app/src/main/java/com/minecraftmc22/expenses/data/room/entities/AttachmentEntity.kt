package com.minecraftmc22.expenses.data.room.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.minecraftmc22.expenses.data.model.Attachment
import com.minecraftmc22.expenses.util.getCurrentTimestamp

/**
 * The expense id is kept as text on purpose: it is a Room row id when the local store is used
 * and a Firestore document id when the user is signed in, and attachments have to work in both
 * cases. That is also why there is no foreign key — deleting an expense cleans the rows up
 * explicitly instead.
 */
@Entity(
    tableName = "attachments",
    indices = [Index("expense_id")]
)
data class AttachmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long,
    @ColumnInfo(name = "expense_id") val expenseId: String,
    @ColumnInfo(name = "path") val path: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "mime_type") val mimeType: String,
    @ColumnInfo(name = "created_at") val createdAt: Long
) {

    fun mapToAttachment() = Attachment(
        id = id.toString(),
        path = path,
        name = name,
        mimeType = mimeType
    )

    companion object {

        fun prepareForInsertion(expenseId: String, attachment: Attachment) =
            AttachmentEntity(
                id = 0,
                expenseId = expenseId,
                path = attachment.path,
                name = attachment.name,
                mimeType = attachment.mimeType,
                createdAt = getCurrentTimestamp()
            )
    }
}
