package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ApiKeyDao {
    @Query("SELECT * FROM api_keys ORDER BY addedTimestamp DESC")
    fun getAllKeys(): Flow<List<ApiKeyEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKey(entry: ApiKeyEntry)

    @Update
    suspend fun updateKey(entry: ApiKeyEntry)

    @Delete
    suspend fun deleteKey(entry: ApiKeyEntry)

    @Query("DELETE FROM api_keys WHERE id = :id")
    suspend fun deleteKeyById(id: Int)
}
