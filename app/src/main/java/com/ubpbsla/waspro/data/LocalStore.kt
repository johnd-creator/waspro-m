package com.ubpbsla.waspro.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "drafts", primaryKeys = ["scope", "id"])
data class Draft(
    val scope: String,
    val id: String,
    val module: String,
    val recordId: String?,
    val payload: String,
    val attachment: String?,
    val requestKey: String,
    val sourceVersion: String?,
    val updatedAt: Long,
    val submittedPayload: String? = null,
    val submittedAttachmentHash: String? = null,
)

@Entity(tableName = "cached_responses", primaryKeys = ["scope", "path"])
data class CachedResponse(val scope: String, val path: String, val payload: String, val savedAt: Long)

@Dao
interface LocalDao {
    @Query("SELECT * FROM drafts WHERE scope = :scope ORDER BY updatedAt DESC")
    fun drafts(scope: String): Flow<List<Draft>>
    @Query("SELECT * FROM drafts WHERE scope = :scope AND id = :id")
    suspend fun draft(scope: String, id: String): Draft?
    @Upsert suspend fun save(draft: Draft)
    @Query("DELETE FROM drafts WHERE scope = :scope AND id = :id")
    suspend fun deleteDraft(scope: String, id: String)
    @Upsert suspend fun cache(response: CachedResponse)
    @Query("SELECT * FROM cached_responses WHERE scope = :scope AND path = :path")
    suspend fun cached(scope: String, path: String): CachedResponse?
    @Query("DELETE FROM cached_responses WHERE scope = :scope")
    suspend fun clearCache(scope: String)
    @Query("DELETE FROM drafts WHERE scope = :scope")
    suspend fun clearDrafts(scope: String)
}

@Database(entities = [Draft::class, CachedResponse::class], version = 1, exportSchema = true)
abstract class LocalStore : RoomDatabase() {
    abstract fun dao(): LocalDao
}
