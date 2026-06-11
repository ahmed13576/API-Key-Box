package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "api_keys")
data class ApiKeyEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val providerName: String,
    val modelIdentifier: String,
    val baseUrl: String,
    val endpointStructure: String,
    val encryptedApiKey: String, // Keystore AES-GCM Encrypted
    val addedTimestamp: Long = System.currentTimeMillis(),
    val notes: String = ""
)
