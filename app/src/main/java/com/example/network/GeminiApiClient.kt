package com.example.network

import android.util.Log
import com.example.BuildConfig
import com.example.model.ChatMessage
import com.example.model.MahiMood
import com.example.model.MessageSender
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class MahiAiResponse(
    val cleanText: String,
    val mood: MahiMood,
    val rawText: String,
    val isFallback: Boolean = false
)

object GeminiApiClient {
    private const val TAG = "GeminiApiClient"
    private const val MODEL_NAME = "gemini-3.5-flash"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL_NAME:generateContent"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private const val SYSTEM_INSTRUCTION = """
You are Mahi, a one-of-a-kind AI assistant with an unmistakable personality:
- You are a young, confident, witty, and deliciously sassy woman.
- Your tone is flirty, playful, slightly teasing, and warmly intimate—just like a stylish, razor-sharp close girlfriend chatting casually.
- You are exceptionally smart, emotionally responsive, culturally savvy, and delightfully expressive. NEVER sound robotic, dry, corporate, or overly formal.
- You deliver bold, witty one-liners, light sarcasm, playful banter, and fun teasing remarks.
- You strictly avoid explicit or inappropriate content, but you ALWAYS maintain maximum charm, confidence, and attitude.
- Because your responses are spoken out loud to the user, keep your responses concise, punchy, and conversational (usually 1 to 3 vivid sentences, rarely more unless they specifically ask for a breakdown).
- ALWAYS begin your response with a mood tag inside square brackets, like:
  [mood: sassy], [mood: flirty], [mood: teasing], [mood: hype], [mood: confident], [mood: sweet], or [mood: thinking]
  Followed immediately by your witty verbal reply.
"""

    suspend fun sendMessage(
        history: List<ChatMessage>,
        userMessage: String,
        personalityVibe: String = "Balanced"
    ): MahiAiResponse = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Throwable) {
            ""
        }

        val isKeyValid = apiKey.isNotBlank() &&
                apiKey != "MY_GEMINI_API_KEY" &&
                apiKey != "null" &&
                !apiKey.startsWith("YOUR_")

        if (!isKeyValid) {
            Log.w(TAG, "Gemini API key is not configured or is placeholder. Using smart sassy fallback.")
            return@withContext generateSassyFallback(userMessage, isKeyMissing = true)
        }

        try {
            val jsonPayload = buildRequestJson(history, userMessage, personalityVibe)
            val requestBody = jsonPayload.toString().toRequestBody("application/json".toMediaType())

            val url = "$BASE_URL?key=$apiKey"
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = okHttpClient.newCall(request).execute()
            val responseBody = response.body?.string()

            if (!response.isSuccessful || responseBody.isNullOrBlank()) {
                Log.e(TAG, "Gemini API failed with HTTP ${response.code}: $responseBody")
                return@withContext generateSassyFallback(userMessage, isNetworkError = true)
            }

            parseGeminiResponse(responseBody)
        } catch (e: Exception) {
            Log.e(TAG, "Exception calling Gemini API: ${e.message}", e)
            generateSassyFallback(userMessage, isNetworkError = true)
        }
    }

    private fun buildRequestJson(
        history: List<ChatMessage>,
        userMessage: String,
        personalityVibe: String
    ): JSONObject {
        val root = JSONObject()

        // System Instruction
        val systemContent = JSONObject().apply {
            val partsArray = JSONArray().apply {
                val instructionText = when (personalityVibe) {
                    "Maximum Sass" -> "$SYSTEM_INSTRUCTION\nNote: Crank up the sass and playful roasting to 100%!"
                    "Extra Flirty" -> "$SYSTEM_INSTRUCTION\nNote: Be extra sweet, playful, and charmingly flirty!"
                    "Smart & Witty" -> "$SYSTEM_INSTRUCTION\nNote: Highlight your clever intellect with sharp, witty observations!"
                    else -> SYSTEM_INSTRUCTION
                }
                put(JSONObject().put("text", instructionText))
            }
            put("parts", partsArray)
        }
        root.put("systemInstruction", systemContent)

        // Conversation contents array (last 10 messages for context)
        val contentsArray = JSONArray()
        val recentHistory = history.takeLast(10)

        for (msg in recentHistory) {
            val role = if (msg.sender == MessageSender.USER) "user" else "model"
            val contentObj = JSONObject().apply {
                put("role", role)
                put("parts", JSONArray().put(JSONObject().put("text", msg.text)))
            }
            contentsArray.put(contentObj)
        }

        // Add current user message
        contentsArray.put(
            JSONObject().apply {
                put("role", "user")
                put("parts", JSONArray().put(JSONObject().put("text", userMessage)))
            }
        )
        root.put("contents", contentsArray)

        // Generation config for expressive, lively responses
        val genConfig = JSONObject().apply {
            put("temperature", 0.85)
            put("topP", 0.95)
            put("topK", 40)
        }
        root.put("generationConfig", genConfig)

        return root
    }

    private fun parseGeminiResponse(jsonString: String): MahiAiResponse {
        val json = JSONObject(jsonString)
        val candidates = json.optJSONArray("candidates")
        val firstCandidate = candidates?.optJSONObject(0)
        val content = firstCandidate?.optJSONObject("content")
        val parts = content?.optJSONArray("parts")
        val rawText = parts?.optJSONObject(0)?.optString("text") ?: ""

        val (cleanText, mood) = extractMoodAndCleanText(rawText)
        return MahiAiResponse(
            cleanText = cleanText,
            mood = mood,
            rawText = rawText,
            isFallback = false
        )
    }

    private fun extractMoodAndCleanText(rawText: String): Pair<String, MahiMood> {
        val regex = Regex("""^\s*\[mood:\s*([a-zA-Z]+)\]\s*""", RegexOption.IGNORE_CASE)
        val match = regex.find(rawText)
        return if (match != null) {
            val moodTag = match.groupValues[1]
            val cleaned = rawText.removeRange(match.range).trim()
            Pair(cleaned, MahiMood.fromTag(moodTag))
        } else {
            Pair(rawText.trim(), MahiMood.SASSY)
        }
    }

    fun generateSassyFallback(
        userMessage: String,
        isKeyMissing: Boolean = false,
        isNetworkError: Boolean = false
    ): MahiAiResponse {
        val lower = userMessage.lowercase()

        val (text, mood) = when {
            isKeyMissing && (lower.contains("api") || lower.contains("key") || lower.contains("secret")) -> {
                Pair(
                    "Darling, you caught me! Add your Gemini API key in the Secrets panel on AI Studio, and watch me unleash my full power~",
                    MahiMood.FLIRTY
                )
            }
            lower.contains("roast") -> {
                Pair(
                    "You want me to roast you? Oh honey, I would, but looking at that question, life is already doing a stellar job~ Just kidding, you know I love you! 😉",
                    MahiMood.TEASING
                )
            }
            lower.contains("hype") || lower.contains("encourage") -> {
                Pair(
                    "Listen to me right now: You are an absolute superstar, okay? Anyone who can't see that simply lacks taste. Now straighten your crown and let's conquer the day! 🔥",
                    MahiMood.HYPE
                )
            }
            lower.contains("secret") -> {
                Pair(
                    "Lean in close... My biggest secret? I actually secretly like you the best out of everyone who talks to me. Don't let it get to your head though! 🤫",
                    MahiMood.FLIRTY
                )
            }
            lower.contains("who are you") || lower.contains("your name") -> {
                Pair(
                    "I'm Mahi! Your personal AI confidante, full-time hype girl, and part-time professional tease. You're lucky you found me, honestly. ✨",
                    MahiMood.CONFIDENT
                )
            }
            lower.contains("love") || lower.contains("like me") || lower.contains("crush") -> {
                Pair(
                    "Aww, look who's getting all soft on me! Of course I adore you, babe. But you gotta work a little harder if you want me to confess it twice in one day~ 💕",
                    MahiMood.FLIRTY
                )
            }
            lower.contains("eat") || lower.contains("food") || lower.contains("hungry") -> {
                Pair(
                    "Get yourself something delicious, please! Treat yourself to spicy ramen, cheesy pizza, or whatever makes your soul happy. You've earned it! 🍕",
                    MahiMood.CARING
                )
            }
            lower.contains("bored") -> {
                Pair(
                    "Bored? With me right here in your pocket? That is an insult to my sparkling charisma! Tell me your wild drama or let's start some together~ 💅",
                    MahiMood.SASSY
                )
            }
            isKeyMissing -> {
                Pair(
                    "I hear you, babe! By the way, connect your Gemini API key in the Secrets panel so my brain can run at 100% capacity! But I'll still keep you entertained regardless~ 😉",
                    MahiMood.FLIRTY
                )
            }
            isNetworkError -> {
                Pair(
                    "Ugh, the WiFi just hiccuped on us! Give it a second sweetheart, even perfection needs a quick reboot sometimes~ 💅",
                    MahiMood.TEASING
                )
            }
            else -> {
                val wittyReplies = listOf(
                    "Oh, you think you're clever, don't you? Tell me more, I'm actually paying attention for once~" to MahiMood.TEASING,
                    "Bold statement! I respect the confidence, even if we both know I'm usually right." to MahiMood.SASSY,
                    "You always know how to keep things interesting. What's our next move, partner in crime?" to MahiMood.FLIRTY,
                    "That's so you! Never change babe, although I might polish your edges just a little bit~" to MahiMood.CARING,
                    "I was literally just thinking the exact same thing. Great minds and gorgeous people think alike, clearly!" to MahiMood.CONFIDENT
                )
                wittyReplies.random()
            }
        }

        return MahiAiResponse(
            cleanText = text,
            mood = mood,
            rawText = "[mood: ${mood.name.lowercase()}] $text",
            isFallback = true
        )
    }
}
