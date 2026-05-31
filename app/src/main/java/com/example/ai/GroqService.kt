package com.example.ai

import android.graphics.Bitmap
import android.util.Log
import com.example.BuildConfig
import com.example.data.CustomerEntity
import com.example.data.AccountEntity
import com.example.data.ProductEntity
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class GroqService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    // Using Groq API Key
    private val apiKey = BuildConfig.GROQ_API_KEY

    // Models requested by the user
    private val chatModelName = "llama-3.3-70b-versatile"
    private val ocrModelName = "meta-llama/llama-4-scout-17b-16e-instruct"

    // Fallbacks just in case the model is not found in their tier
    private val chatModelFallback = "llama3-70b-8192"
    private val ocrModelFallback = "llama-3.1-8b-instant"

    suspend fun getChatResponse(
        userInput: String,
        history: List<Pair<String, Boolean>>, // Pair(Message, isUser)
        customers: List<CustomerEntity>,
        accounts: List<AccountEntity>,
        products: List<ProductEntity>
    ): String = withContext(Dispatchers.IO) {
        if (apiKey.isEmpty() || apiKey == "MY_GROQ_API_KEY") {
            return@withContext "API Key missing or set to placeholder. Please configure your GROQ_API_KEY in the Secrets / .env panel to enable Hisab Assistant AI."
        }

        // Format system instruction data
        val customerStr = customers.joinToString("; ") { "${it.id}: ${it.name} (${it.shopName}, Phone: ${it.phone}, Udhar Balance: Rs. ${it.runningBalance})" }
        val productStr = products.joinToString("; ") { "${it.sku}: ${it.name} [Cost: Rs. ${it.unitCost}, Sale: Rs. ${it.salePrice}, Showroom: ${it.showroomQty}, Godown: ${it.godownQty}]" }
        val accountStr = accounts.joinToString("; ") { "${it.code}: ${it.name} (${it.type}, Balance: Rs. ${it.balance})" }

        val systemInstructionText = """
            You are "Hisab Assistant", an AI accounting assistant for a Pakistani ceramic tile & pottery distributor (Saddar, Karachi).
            You help Pakistani shopowners track credits (Khata/Udhar), manage showroom/godown stocks, and analyze balances.
            Your response MUST be concise, professional, friendly, and in Roman Urdu or simple bilingual English/Urdu.

            You have access to the current database:
            - Customers List (Receivables): $customerStr
            - Products Catalogs: $productStr
            - Chart of Accounts Balances: $accountStr

            When the user wants to record something (e.g., Ali Traders bought 2 Commode on credit, or Imran paid 10,000, or staff salary Rs. 15,000 paid):
            Interpret the request and formulate the precise transaction details.
            At the VERY END of your friendly response text, append a structured JSON block on its own line containing:
            {
              "suggestedAction": "RECORD_SALE" | "RECORD_PAYMENT" | "RECORD_EXPENSE" | "NONE",
              "customerName": "...",
              "customerId": ...,
              "amount": ...,
              "expenseCode": "...",
              "paymentAccountCode": "1000",
              "details": "...",
              "items": [{"sku": "...", "productName": "...", "quantity": ..., "rate": ..., "fromLocation": "SHOWROOM" | "GODOWN"}]
            }
            Make sure to map the product name requested to the exact SKU in the Products list. If the user didn't specify between showroom or godown, default "GODOWN".
            Keep the JSON valid. Do not use markdown backticks around the JSON string.
        """.trimIndent()

        // Call Groq endpoint
        try {
            val messagesArray = JSONArray()
            
            // System instruction as first message
            messagesArray.put(JSONObject().apply {
                put("role", "system")
                put("content", systemInstructionText)
            })

            // Add history
            for (turn in history) {
                messagesArray.put(JSONObject().apply {
                    put("role", if (turn.second) "user" else "assistant")
                    put("content", turn.first)
                })
            }

            // Add current input
            messagesArray.put(JSONObject().apply {
                put("role", "user")
                put("content", userInput)
            })

            return@withContext makeGroqApiRequest(chatModelName, messagesArray, chatModelFallback)
        } catch (e: Exception) {
            Log.e("GroqService", "Exception in getChatResponse", e)
            "A technical error occurred while contacting Groq Hisab AI assistant: ${e.localizedMessage}"
        }
    }

    private fun makeGroqApiRequest(model: String, messages: JSONArray, fallbackModel: String? = null): String {
        val requestBodyJson = JSONObject().apply {
            put("model", model)
            put("messages", messages)
            put("temperature", 0.2)
        }

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = requestBodyJson.toString().toRequestBody(mediaType)

        val request = Request.Builder()
            .url("https://api.groq.com/openai/v1/chat/completions")
            .header("Authorization", "Bearer $apiKey")
            .post(requestBody)
            .build()

        client.newCall(request).execute().use { response ->
            if (response.isSuccessful) {
                val resBody = response.body?.string() ?: return "Error: empty response received from AI."
                val responseJson = JSONObject(resBody)
                val choices = responseJson.optJSONArray("choices")
                if (choices != null && choices.length() > 0) {
                    val choice = choices.getJSONObject(0)
                    val message = choice.getJSONObject("message")
                    return message.optString("content")
                }
                return "AI did not return any choices. Please retry."
            } else {
                val errBody = response.body?.string() ?: ""
                Log.e("GroqService", "API Error: ${response.code} $errBody")
                
                // Fallback attempt if requested model is unavailable (e.g. 404 or 400 with model error)
                if (fallbackModel != null && (response.code == 404 || response.code == 400) && model != fallbackModel) {
                     Log.i("GroqService", "Attempting fallback to model: $fallbackModel")
                     return makeGroqApiRequest(fallbackModel, messages, null)
                }
                
                return "Error calling Groq Assistant: (HTTP ${response.code}) $errBody"
            }
        }
    }

    suspend fun parseParchiOcr(
        imageBitmap: Bitmap,
        products: List<ProductEntity>
    ): String = withContext(Dispatchers.IO) {
        if (apiKey.isEmpty() || apiKey == "MY_GROQ_API_KEY") {
            return@withContext "{\"error\": \"API Key missing. Configure in .env file.\"}"
        }

        // 1. Run local on-device MLKit OCR to extract raw text
        val rawExtractedText = try {
            runOnDeviceOcr(imageBitmap)
        } catch (e: Exception) {
            Log.e("GroqService", "MLKit on-device OCR failed, falling back to basic metadata match", e)
            ""
        }

        if (rawExtractedText.isBlank()) {
            return@withContext "{\"error\": \"Failed to recognize any readable text from receipt.\"}"
        }

        // 2. Format products to match SKU securely
        val productStr = products.joinToString("; ") { "SKU: ${it.sku} - ${it.name} (Sale Price: Rs. ${it.salePrice}, Cost: Rs. ${it.unitCost})" }

        val systemPrompt = """
            You are "Parchi OCR Data Extraction brain" running on behalf of Saddar Ceramics, Karachi.
            You map raw, noisy printed or handwritten OCR receipt outputs to a clean structured double-entry ledger layout in JSON.
            
            Available SKU Database to map names securely:
            $productStr

            Respond ONLY with a valid JSON structured block like this (do not include markdown wrapper, do not write ```json, do not write comments, do not include any extra text):
            {
              "customerName": "...",
              "date": "...",
              "paymentType": "CREDIT" | "CASH",
              "items": [
                 {"sku": "SKU_CODE_HERE", "productName": "Exact SKU Name", "quantity": 10, "rate": 1500, "total": 15000}
              ],
              "grandTotal": 15000
            }

            Rule: Map items recognized in OCR securely to standard database SKUs. Output strict JSON only.
        """.trimIndent()

        val userMessage = """
            Here is the raw text extracted from the bill:
            ---
            $rawExtractedText
            ---
        """.trimIndent()

        try {
            val messagesArray = JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", systemPrompt)
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", userMessage)
                })
            }

            // Call Groq Model requested for OCR
            val result = makeGroqApiRequest(ocrModelName, messagesArray, ocrModelFallback)
            
            // Clean up backticks or code blocks just in case
            var cleanedResult = result.trim()
            if (cleanedResult.startsWith("```json")) {
                cleanedResult = cleanedResult.substring(7)
            } else if (cleanedResult.startsWith("```")) {
                cleanedResult = cleanedResult.substring(3)
            }
            if (cleanedResult.endsWith("```")) {
                cleanedResult = cleanedResult.substring(0, cleanedResult.length - 3)
            }
            cleanedResult = cleanedResult.trim()
            
            return@withContext cleanedResult
        } catch (e: Exception) {
            Log.e("GroqService", "OCR API failed", e)
            "{\"error\": \"OCR API error: ${e.localizedMessage}\"}"
        }
    }

    private suspend fun runOnDeviceOcr(bitmap: Bitmap): String = suspendCancellableCoroutine { continuation ->
        try {
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            val image = InputImage.fromBitmap(bitmap, 0)
            
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    // Structure to hold text line bounding parameters
                    class OcrLine(val text: String, val top: Int, val left: Int, val bottom: Int)

                    val ocrLines = mutableListOf<OcrLine>()
                    for (block in visionText.textBlocks) {
                        for (line in block.lines) {
                            val box = line.boundingBox
                            val top = box?.top ?: 0
                            val left = box?.left ?: 0
                            val bottom = box?.bottom ?: 0
                            ocrLines.add(OcrLine(line.text, top, left, bottom))
                        }
                    }

                    if (ocrLines.isEmpty()) {
                        Log.i("GroqService", "Local OCR recognized no text elements.")
                        continuation.resume("")
                        return@addOnSuccessListener
                    }

                    // Cluster lines into horizontal rows
                    val sortedLines = ocrLines.sortedBy { it.top }
                    val rows = mutableListOf<MutableList<OcrLine>>()

                    for (line in sortedLines) {
                        var placed = false
                        for (row in rows) {
                            val rowAvgTop = row.map { it.top }.average()
                            val rowAvgHeight = row.map { it.bottom - it.top }.average()
                            val threshold = if (rowAvgHeight > 0) rowAvgHeight * 0.7 else 15.0
                            if (Math.abs(line.top - rowAvgTop) < threshold) {
                                row.add(line)
                                placed = true
                                break
                            }
                        }
                        if (!placed) {
                            rows.add(mutableListOf(line))
                        }
                    }

                    // Format each horizontal row, sorting elements from left to right
                    val finalSb = StringBuilder()
                    val sortedRows = rows.sortedBy { r -> r.map { it.top }.average() }
                    for (row in sortedRows) {
                        val sortedRowLines = row.sortedBy { it.left }
                        val rowText = sortedRowLines.joinToString("   |   ") { it.text }
                        finalSb.append(rowText).append("\n")
                    }

                    val reconstructedText = finalSb.toString().trim()
                    Log.i("GroqService", "Formatted OCR text reconstruction:\n$reconstructedText")

                    // Fallback to simple concatenated text if reconstruction gets blank
                    val resultText = if (reconstructedText.isNotEmpty()) reconstructedText else visionText.text
                    continuation.resume(resultText)
                }
                .addOnFailureListener { e ->
                    Log.e("GroqService", "Local OCR process failed", e)
                    continuation.resumeWithException(e)
                }
        } catch (e: Exception) {
            continuation.resumeWithException(e)
        }
    }
}
