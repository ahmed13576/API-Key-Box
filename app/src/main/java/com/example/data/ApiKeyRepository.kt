package com.example.data

import kotlinx.coroutines.flow.Flow

class ApiKeyRepository(private val apiKeyDao: ApiKeyDao) {
    val allKeys: Flow<List<ApiKeyEntry>> = apiKeyDao.getAllKeys()

    suspend fun insertKey(entry: ApiKeyEntry) {
        apiKeyDao.insertKey(entry)
    }

    suspend fun updateKey(entry: ApiKeyEntry) {
        apiKeyDao.updateKey(entry)
    }

    suspend fun deleteKey(entry: ApiKeyEntry) {
        apiKeyDao.deleteKey(entry)
    }

    suspend fun deleteKeyById(id: Int) {
        apiKeyDao.deleteKeyById(id)
    }
}
