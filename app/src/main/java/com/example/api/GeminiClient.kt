package com.example.api

import com.example.BuildConfig
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    @Json(name = "contents") val contents: List<Content>,
    @Json(name = "generationConfig") val generationConfig: GenerationConfig? = null,
    @Json(name = "systemInstruction") val systemInstruction: Content? = null
)

@JsonClass(generateAdapter = true)
data class Content(
    @Json(name = "parts") val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class Part(
    @Json(name = "text") val text: String
)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    @Json(name = "temperature") val temperature: Float? = null,
    @Json(name = "responseMimeType") val responseMimeType: String? = null
)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(
    @Json(name = "candidates") val candidates: List<Candidate>?
)

@JsonClass(generateAdapter = true)
data class Candidate(
    @Json(name = "content") val content: Content?
)

@JsonClass(generateAdapter = true)
data class ProviderDetails(
    @Json(name = "baseUrl") val baseUrl: String,
    @Json(name = "endpointStructure") val endpointStructure: String,
    @Json(name = "documentationNotes") val documentationNotes: String,
    @Json(name = "headerStructure") val headerStructure: String
)

interface GeminiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

object GeminiClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val service: GeminiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiService::class.java)
    }

    suspend fun lookupProviderDocs(providerName: String, modelIdentifier: String): ProviderDetails = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext getFallbackDocs(providerName, modelIdentifier)
        }

        val prompt = "Service Provider Name: $providerName\nLLM Model Identifier (or MCP description): $modelIdentifier"

        val systemPrompt = """
            You are an expert developer-assistant. Based exclusively on the official, public documentation for the given Service Provider and LLM/MCP identifier (as of mid-2026), suggest the correct, official Base URL, endpoint structure, main headers (such as authentication or content-type), and a 1-sentence tip.
            
            Your output MUST be a valid JSON object with the following fields:
            1. "baseUrl": String (The official API Base URL. End with NO trailing slash, e.g., "https://api.openai.com/v1" or "https://api.anthropic.com/v1")
            2. "endpointStructure": String (The primary endpoint route, e.g., "/chat/completions" or "/v1/messages")
            3. "documentationNotes": String (A single brief sentence with tips or context, e.g., "Supports streaming and temperature configurations")
            4. "headerStructure": String (The standard authentication header format, e.g., "Authorization: Bearer [KEY]" or "x-api-key: [KEY]")
            
            Strictly return a plain JSON string. No surrounding markdown, no backticks, no comments.
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(
                Content(parts = listOf(Part(text = prompt)))
            ),
            generationConfig = GenerationConfig(
                temperature = 0.1f,
                responseMimeType = "application/json"
            ),
            systemInstruction = Content(parts = listOf(Part(text = systemPrompt)))
        )

        try {
            val response = service.generateContent(apiKey, request)
            val rawText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (rawText != null) {
                val cleanedText = cleanJsonString(rawText)
                val jsonAdapter = moshi.adapter(ProviderDetails::class.java)
                jsonAdapter.fromJson(cleanedText) ?: getFallbackDocs(providerName, modelIdentifier)
            } else {
                getFallbackDocs(providerName, modelIdentifier)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            getFallbackDocs(providerName, modelIdentifier)
        }
    }

    private fun cleanJsonString(raw: String): String {
        var text = raw.trim()
        if (text.startsWith("```json")) {
            text = text.removePrefix("```json")
        } else if (text.startsWith("```")) {
            text = text.removePrefix("```")
        }
        if (text.endsWith("```")) {
            text = text.removeSuffix("```")
        }
        return text.trim()
    }

    fun getFallbackDocs(providerName: String, modelIdentifier: String): ProviderDetails {
        val providerLower = providerName.lowercase()
        val modelLower = modelIdentifier.lowercase()

        return when {
            providerLower.contains("openai") -> ProviderDetails(
                baseUrl = "https://api.openai.com/v1",
                endpointStructure = "/chat/completions",
                documentationNotes = "Requires standard Bearer authorization token header.",
                headerStructure = "Authorization: Bearer [KEY]"
            )
            providerLower.contains("anthropic") || providerLower.contains("claude") -> ProviderDetails(
                baseUrl = "https://api.anthropic.com",
                endpointStructure = "/v1/messages",
                documentationNotes = "Requires x-api-key and anthropic-version headers.",
                headerStructure = "x-api-key: [KEY]"
            )
            providerLower.contains("gemini") || providerLower.contains("google") -> ProviderDetails(
                baseUrl = "https://generativelanguage.googleapis.com",
                endpointStructure = "/v1beta/models/$modelIdentifier:generateContent",
                documentationNotes = "Authentication passes directly in request query parameter (key=API_KEY).",
                headerStructure = "Query URL Parameter: ?key=[KEY]"
            )
            providerLower.contains("groq") -> ProviderDetails(
                baseUrl = "https://api.groq.com/openai/v1",
                endpointStructure = "/chat/completions",
                documentationNotes = "Groq supports high-speed inference with OpenAI compatibility endpoints.",
                headerStructure = "Authorization: Bearer [KEY]"
            )
            providerLower.contains("deepseek") -> ProviderDetails(
                baseUrl = "https://api.deepseek.com",
                endpointStructure = "/chat/completions",
                documentationNotes = "DeepSeek matches standard OpenAI-compatible completions endpoints securely.",
                headerStructure = "Authorization: Bearer [KEY]"
            )
            providerLower.contains("openrouter") -> ProviderDetails(
                baseUrl = "https://openrouter.ai/api/v1",
                endpointStructure = "/chat/completions",
                documentationNotes = "Centralized proxy routing to hundreds of model endpoints.",
                headerStructure = "Authorization: Bearer [KEY]"
            )
            providerLower.contains("mistral") -> ProviderDetails(
                baseUrl = "https://api.mistral.ai/v1",
                endpointStructure = "/chat/completions",
                documentationNotes = "Mistral API endpoints support structured JSON extraction.",
                headerStructure = "Authorization: Bearer [KEY]"
            )
            providerLower.contains("cohere") -> ProviderDetails(
                baseUrl = "https://api.cohere.com/v1",
                endpointStructure = "/v1/chat",
                documentationNotes = "Requires Cohere bearer key for direct chat inference.",
                headerStructure = "Authorization: Bearer [KEY]"
            )
            providerLower.contains("mcp") || modelLower.contains("mcp") -> ProviderDetails(
                baseUrl = "http://localhost:3000",
                endpointStructure = "/v1/sse",
                documentationNotes = "Default SSE Model Context Protocol server channel. Configure your host port.",
                headerStructure = "Content-Type: application/json"
            )
            else -> ProviderDetails(
                baseUrl = "https://api.${providerLower.filter { it.isLetterOrDigit() }}.com/v1",
                endpointStructure = "/chat/completions",
                documentationNotes = "General fallback generated using provider host signature.",
                headerStructure = "Authorization: Bearer [KEY]"
            )
        }
    }
}
