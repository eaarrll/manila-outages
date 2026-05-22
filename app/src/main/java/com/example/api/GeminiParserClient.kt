package com.example.api

import com.example.BuildConfig
import com.example.data.OutageEntity
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

// --- Moshi Models for Gemini ---

@JsonClass(generateAdapter = true)
data class Part(
    val text: String? = null
)

@JsonClass(generateAdapter = true)
data class Content(
    val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class ResponseFormatText(
    val mimeType: String
)

@JsonClass(generateAdapter = true)
data class ResponseFormat(
    val text: ResponseFormatText? = null
)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    val responseFormat: ResponseFormat? = null,
    val temperature: Float? = null
)

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null,
    val systemInstruction: Content? = null
)

@JsonClass(generateAdapter = true)
data class Candidate(
    val content: Content
)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(
    val candidates: List<Candidate>?
)

// --- Structured Response Schema from Gemini ---

@JsonClass(generateAdapter = true)
data class OutageParseSchema(
    val type: String, // "POWER" or "WATER"
    val provider: String, // "MERALCO", "MAYNILAD", "MANILA_WATER"
    val title: String,
    val city: String,
    val barangay: String,
    val streets: String,
    val details: String,
    val startDate: String, // "YYYY-MM-DD HH:MM"
    val endDate: String // "YYYY-MM-DD HH:MM"
)

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

object GeminiParserClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val apiService: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApiService::class.java)
    }

    suspend fun parseAnnouncement(rawText: String, currentTimestamp: Long): OutageEntity? {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            // No API Key configured, throw custom descriptive error
            throw IllegalStateException("Gemini API key is not configured in the Secrets panel.")
        }

        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss'Z'", Locale.US)
        val formattedCurrentTime = dateFormat.format(Date(currentTimestamp))

        val prompt = """
            You are an expert Metro Manila Outage Advisory parser in an Android app. 
            The user will paste some unstructured text of an announcement, PDF report, or Facebook post outlining scheduled electricity/water service interruptions.
            Your task is to analyze this advisory text, identify affected areas in Metro Manila, and structure it into a JSON object conforming EXACTLY to the OutageParseSchema.
            
            Current Time reference is: $formattedCurrentTime
            
            Return the parsed details as a JSON object adhering to this structure:
            {
              "type": "POWER" or "WATER",
              "provider": "MERALCO" or "MAYNILAD" or "MANILA_WATER",
              "title": "A short, descriptive headline summarizing the work, e.g. 'Line Upgrading', 'Emergency Valve Rehab'",
              "city": "The single primary Metro Manila City affected (e.g. 'Quezon City', 'Pasig', 'Makati', 'Parañaque', 'Muntinlupa', 'Marikina', 'Malabon', 'Pateros', etc.)",
              "barangay": "The main Barangay affected. If multiple, summarize/list briefly.",
              "streets": "Specific streets, phases, or subdivisions affected.",
              "details": "An explanation of the work being performed, or customer guidelines from the text.",
              "startDate": "YYYY-MM-DD HH:MM (Calculate the start date and time relative to the current reference time from the text details)",
              "endDate": "YYYY-MM-DD HH:MM (Calculate the end date and time relative to the current reference time from the text details)"
            }

            Advisory Text to Parse:
            $rawText
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            generationConfig = GenerationConfig(
                responseFormat = ResponseFormat(text = ResponseFormatText(mimeType = "application/json")),
                temperature = 0.2f
            ),
            systemInstruction = Content(parts = listOf(Part(text = "You extract scheduled power & water interruptions into the specified JSON format strictly, estimating timestamps carefully relative to the current live reference timestamp.")))
        )

        val response = apiService.generateContent(apiKey, request)
        val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: return null

        try {
            val schemaAdapter = moshi.adapter(OutageParseSchema::class.java)
            val parsedResult = schemaAdapter.fromJson(jsonText) ?: return null

            // Parse dates to Timestamps
            val targetFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
            val startMs = try { targetFormat.parse(parsedResult.startDate)?.time ?: (currentTimestamp + 3600000L) } catch (e: Exception) { currentTimestamp + 3600000L }
            val endMs = try { targetFormat.parse(parsedResult.endDate)?.time ?: (currentTimestamp + 7200000L) } catch (e: Exception) { currentTimestamp + 7200000L }

            return OutageEntity(
                type = parsedResult.type.uppercase(),
                provider = parsedResult.provider.uppercase(),
                isScheduled = true,
                title = parsedResult.title,
                scheduledStart = startMs,
                scheduledEnd = endMs,
                city = parsedResult.city,
                barangay = parsedResult.barangay,
                streets = parsedResult.streets,
                details = parsedResult.details,
                reportedAt = System.currentTimeMillis(),
                reportedBy = "AI Copilot Parser",
                isVerified = true
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
}
