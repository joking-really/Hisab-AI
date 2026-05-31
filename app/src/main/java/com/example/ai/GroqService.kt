package com.example.ai

import android.graphics.Bitmap
import android.util.Log
import com.example.BuildConfig
import com.example.data.CustomerEntity
import com.example.data.AccountBalanceView
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

    // Using Supabase Edge proxy URLs
    private val supabaseUrl = BuildConfig.SUPABASE_URL
    private val supabaseAnonKey = BuildConfig.SUPABASE_ANON_KEY

    // Models requested by the user
    private val chatModelName = "llama-3.3-70b-versatile"
    private val ocrModelName = "meta-llama/llama-4-scout-17b-16e-instruct"

    // Fallbacks just in case the model is not found in their tier
    private val chatModelFallback = "llama3-70b-8192"
    private val ocrModelFallback = "llama-3.1-8b-instant"

    private fun getActiveStrategy(): String {
        val hasSupabase = try {
            BuildConfig.SUPABASE_URL.isNotEmpty() && 
            BuildConfig.SUPABASE_URL != "https://your-project-ref.supabase.co" && 
            BuildConfig.SUPABASE_ANON_KEY.isNotEmpty() && 
            !BuildConfig.SUPABASE_ANON_KEY.startsWith("eyJhbGciOiJIUzI1NiIs")
        } catch (e: Throwable) {
            false
        }

        val hasGroq = try {
            BuildConfig.GROQ_API_KEY.isNotEmpty() && 
            BuildConfig.GROQ_API_KEY != "MY_GROQ_API_KEY"
        } catch (e: Throwable) {
            false
        }

        return when {
            hasSupabase -> "SUPABASE_PROXY"
            hasGroq -> "GROQ_DIRECT"
            else -> "GEMINI_DIRECT"
        }
    }

    suspend fun getChatResponse(
        userInput: String,
        history: List<Pair<String, Boolean>>, // Pair(Message, isUser)
        customers: List<CustomerEntity>,
        accounts: List<AccountBalanceView>,
        products: List<ProductEntity>,
        toolExecutor: suspend (String, Map<String, Any>) -> String
    ): String = withContext(Dispatchers.IO) {
        val strategy = getActiveStrategy()
        Log.d("GroqService", "Active chat strategy: $strategy")

        val systemInstructionText = """
            You are Hisab Assistant, an AI accounting helper for a Pakistani ceramic distributor.
            You have access to tools to query the database. Never make up data.
            Respond in the user's language (Roman Urdu, English, or mix).
            When discussing amounts, always include numeric and written format: "Rs. 1,25,000 (Ek lakh pachis hazar)".
            
            Whenever asked about stock, account balance, overdue accounts, or daily summary, ALWAYS use your tool function calls rather than guessing.
            When you want to schedule a sale draft or payment draft for the user to confirm, call suggest_sale or suggest_payment tool.
        """.trimIndent()


        // Tools Array JSON
        val toolsArray = JSONArray().apply {
            put(JSONObject().apply {
                put("type", "function")
                put("function", JSONObject().apply {
                    put("name", "get_customer_balance")
                    put("description", "Get ledger balance and credit limit for a customer by name")
                    put("parameters", JSONObject().apply {
                        put("type", "object")
                        put("properties", JSONObject().apply {
                            put("name", JSONObject().apply { 
                                put("type", "string") 
                                put("description", "Name of the customer or shop")
                            })
                        })
                        put("required", JSONArray().apply { put("name") })
                    })
                })
            })
            put(JSONObject().apply {
                put("type", "function")
                put("function", JSONObject().apply {
                    put("name", "get_stock_level")
                    put("description", "Get quantities available in stock for a product")
                    put("parameters", JSONObject().apply {
                        put("type", "object")
                        put("properties", JSONObject().apply {
                            put("name", JSONObject().apply { 
                                put("type", "string") 
                                put("description", "Product name or SKU code")
                            })
                            put("location", JSONObject().apply {
                                put("type", "string")
                                put("enum", JSONArray().apply { put("showroom"); put("godown") })
                                put("description", "Optional storage location name")
                            })
                        })
                        put("required", JSONArray().apply { put("name") })
                    })
                })
            })
            put(JSONObject().apply {
                put("type", "function")
                put("function", JSONObject().apply {
                    put("name", "get_daily_summary")
                    put("description", "Get total sales summary for today or an optional date")
                    put("parameters", JSONObject().apply {
                        put("type", "object")
                        put("properties", JSONObject().apply {
                            put("date", JSONObject().apply { 
                                put("type", "string")
                                put("description", "DD-MM-YYYY format, optional")
                            })
                        })
                    })
                })
            })
            put(JSONObject().apply {
                put("type", "function")
                put("function", JSONObject().apply {
                    put("name", "get_overdue_accounts")
                    put("description", "List credit clients currently overdue or exceeding limits")
                    put("parameters", JSONObject().apply {
                        put("type", "object")
                        put("properties", JSONObject().apply {
                            put("days", JSONObject().apply { 
                                put("type", "integer")
                                put("description", "Minimum days overdue")
                            })
                        })
                    })
                })
            })
            put(JSONObject().apply {
                put("type", "function")
                put("function", JSONObject().apply {
                    put("name", "get_cash_position")
                    put("description", "Get cash balances in vault, banks, and phone registers")
                    put("parameters", JSONObject().apply {
                        put("type", "object")
                        put("properties", JSONObject().apply { })
                    })
                })
            })
            put(JSONObject().apply {
                put("type", "function")
                put("function", JSONObject().apply {
                    put("name", "suggest_sale")
                    put("description", "Prepare a sale draft for confirmation. REQUIRES user approval before executing.")
                    put("parameters", JSONObject().apply {
                        put("type", "object")
                        put("properties", JSONObject().apply {
                            put("customer_name", JSONObject().apply { put("type", "string") })
                            put("items", JSONObject().apply {
                                put("type", "array")
                                put("items", JSONObject().apply {
                                    put("type", "object")
                                    put("properties", JSONObject().apply {
                                        put("sku", JSONObject().apply { put("type", "string") })
                                        put("product_name", JSONObject().apply { put("type", "string") })
                                        put("quantity", JSONObject().apply { put("type", "integer") })
                                        put("unit_price", JSONObject().apply { put("type", "number") })
                                        put("fromLocation", JSONObject().apply {
                                            put("type", "string")
                                            put("enum", JSONArray().apply { put("SHOWROOM"); put("GODOWN") })
                                        })
                                    })
                                })
                            })
                            put("payment_type", JSONObject().apply {
                                put("type", "string")
                                put("enum", JSONArray().apply { put("CASH"); put("CREDIT") })
                            })
                        })
                        put("required", JSONArray().apply { put("customer_name"); put("items"); put("payment_type") })
                    })
                })
            })
            put(JSONObject().apply {
                put("type", "function")
                put("function", JSONObject().apply {
                    put("name", "suggest_payment")
                    put("description", "Prepare a payment receipt draft for confirmation. REQUIRES user approval.")
                    put("parameters", JSONObject().apply {
                        put("type", "object")
                        put("properties", JSONObject().apply {
                            put("customer_name", JSONObject().apply { put("type", "string") })
                            put("amount", JSONObject().apply { put("type", "number") })
                            put("method", JSONObject().apply {
                                put("type", "string")
                                put("enum", JSONArray().apply { put("cash"); put("easypaisa"); put("jazzcash"); put("bankTransfer") })
                            })
                        })
                        put("required", JSONArray().apply { put("customer_name"); put("amount"); put("method") })
                    })
                })
            })
        }

        if (strategy == "GEMINI_DIRECT") {
            try {
                return@withContext callGeminiWithTools(systemInstructionText, history, userInput, toolsArray, toolExecutor)
            } catch (e: Exception) {
                Log.e("GroqService", "Gemini call failed, trying direct Groq fallback if configured", e)
            }
        }

        // Call Groq endpoint
        try {
            val messagesArray = JSONArray()
            
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

            return@withContext makeGroqApiRequestWithTools(chatModelName, messagesArray, toolsArray, toolExecutor, chatModelFallback)
        } catch (e: Exception) {
            Log.e("GroqService", "Exception in getChatResponse", e)
            "A technical error occurred while contacting Groq Hisab AI assistant: ${e.localizedMessage}"
        }
    }

    private suspend fun makeGroqApiRequestWithTools(
        model: String,
        messages: JSONArray,
        tools: JSONArray,
        toolExecutor: suspend (String, Map<String, Any>) -> String,
        fallbackModel: String? = null
    ): String = withContext(Dispatchers.IO) {
        val strategy = getActiveStrategy()
        val url = if (strategy == "SUPABASE_PROXY") {
            "$supabaseUrl/functions/v1/groq-chat"
        } else {
            "https://api.groq.com/openai/v1/chat/completions"
        }

        val authHeaderValue = if (strategy == "SUPABASE_PROXY") {
            "Bearer $supabaseAnonKey"
        } else {
            "Bearer ${BuildConfig.GROQ_API_KEY}"
        }

        val requestBodyJson = JSONObject().apply {
            put("model", model)
            put("messages", messages)
            put("temperature", 0.2)
            put("tools", tools)
            put("tool_choice", "auto")
        }

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = requestBodyJson.toString().toRequestBody(mediaType)

        val request = Request.Builder()
            .url(url)
            .header("Authorization", authHeaderValue)
            .post(requestBody)
            .build()

        client.newCall(request).execute().use { response ->
            val resBody = response.body?.string() ?: ""
            if (response.isSuccessful) {
                val responseJson = JSONObject(resBody)
                val choices = responseJson.optJSONArray("choices")
                if (choices != null && choices.length() > 0) {
                    val choice = choices.getJSONObject(0)
                    val message = choice.getJSONObject("message")
                    
                    if (message.has("tool_calls")) {
                        val toolCalls = message.getJSONArray("tool_calls")
                        // Add assistant response to messages queue
                        messages.put(message)

                        // Process tool calls
                        for (i in 0 until toolCalls.length()) {
                            val tc = toolCalls.getJSONObject(i)
                            val id = tc.getString("id")
                            val func = tc.getJSONObject("function")
                            val name = func.getString("name")
                            val argsStr = func.getString("arguments")

                            val argsMap = mutableMapOf<String, Any>()
                            try {
                                val jo = JSONObject(argsStr)
                                val keys = jo.keys()
                                while (keys.hasNext()) {
                                    val key = keys.next()
                                    argsMap[key] = jo.get(key)
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }

                            // Execute local Kotlin logic against local Room DB instance
                            val toolResult = toolExecutor(name, argsMap)

                            // Append tool response
                            messages.put(JSONObject().apply {
                                put("role", "tool")
                                put("content", toolResult)
                                put("name", name)
                                put("tool_call_id", id)
                            })
                        }

                        // Send call back to AI with data filled
                        val requestFinalBodyJson = JSONObject().apply {
                            put("model", model)
                            put("messages", messages)
                            put("temperature", 0.2)
                        }
                        val requestFinalBody = requestFinalBodyJson.toString().toRequestBody(mediaType)
                        val finalReq = Request.Builder()
                            .url(url)
                            .header("Authorization", authHeaderValue)
                            .post(requestFinalBody)
                            .build()

                        client.newCall(finalReq).execute().use { finalResponse ->
                            val finalResBody = finalResponse.body?.string() ?: ""
                            if (finalResponse.isSuccessful) {
                                val fJson = JSONObject(finalResBody)
                                val fChoices = fJson.optJSONArray("choices")
                                if (fChoices != null && fChoices.length() > 0) {
                                    return@withContext fChoices.getJSONObject(0).getJSONObject("message").optString("content")
                                }
                            }
                            return@withContext "Error resolving tool call loop: ${finalResponse.code} $finalResBody"
                        }
                    } else {
                        return@withContext message.optString("content")
                    }
                }
                return@withContext "AI did not return any content. Please try again."
            } else {
                Log.e("GroqService", "API Error: ${response.code} $resBody")
                if (fallbackModel != null && (response.code == 404 || response.code == 400) && model != fallbackModel) {
                    return@withContext makeGroqApiRequestWithTools(fallbackModel, messages, tools, toolExecutor, null)
                }
                return@withContext "Error calling Groq Assistant: (HTTP ${response.code}) $resBody"
            }
        }
    }

    private fun makeGroqApiRequest(model: String, messages: JSONArray, fallbackModel: String? = null): String {
        val strategy = getActiveStrategy()
        val url = if (strategy == "SUPABASE_PROXY") {
            "$supabaseUrl/functions/v1/groq-chat"
        } else {
            "https://api.groq.com/openai/v1/chat/completions"
        }

        val authHeaderValue = if (strategy == "SUPABASE_PROXY") {
            "Bearer $supabaseAnonKey"
        } else {
            "Bearer ${BuildConfig.GROQ_API_KEY}"
        }

        val requestBodyJson = JSONObject().apply {
            put("model", model)
            put("messages", messages)
            put("temperature", 0.1)
        }

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = requestBodyJson.toString().toRequestBody(mediaType)

        val request = Request.Builder()
            .url(url)
            .header("Authorization", authHeaderValue)
            .post(requestBody)
            .build()

        client.newCall(request).execute().use { response ->
            val resBody = response.body?.string() ?: ""
            if (response.isSuccessful) {
                val responseJson = JSONObject(resBody)
                val choices = responseJson.optJSONArray("choices")
                if (choices != null && choices.length() > 0) {
                    return choices.getJSONObject(0).getJSONObject("message").optString("content")
                }
                return "AI empty response."
            } else {
                Log.e("GroqService", "API Error: ${response.code} $resBody")
                if (fallbackModel != null && (response.code == 404 || response.code == 400) && model != fallbackModel) {
                    return makeGroqApiRequest(fallbackModel, messages, null)
                }
                return "Error (HTTP ${response.code}) $resBody"
            }
        }
    }

    suspend fun parseParchiOcr(
        imageBitmap: Bitmap,
        products: List<ProductEntity>
    ): String = withContext(Dispatchers.IO) {
        val strategy = getActiveStrategy()
        Log.d("GroqService", "Selected OCR strategy: $strategy")

        // 1. Preprocess then run on-device local text recognition
        val preprocessed = OcrPreprocessor.preprocessImage(imageBitmap)
        val rawExtractedText = try {
            runOnDeviceOcr(preprocessed)
        } catch (e: Exception) {
            Log.e("GroqService", "MLKit text recognition failed", e)
            ""
        }

        if (rawExtractedText.isBlank()) {
            return@withContext "{\"error\": \"Failed to recognize any readable text from receipt.\"}"
        }

        val productStr = products.joinToString("; ") { "SKU: ${it.sku} - ${it.name} (Sale Price: Rs. ${it.salePrice}, Cost: Rs. ${it.unitCost})" }

        val systemPrompt = """
            You are an OCR engine for Pakistani wholesale receipts (parchi).
            The receipt may be handwritten or printed, in Urdu, English, or mixed.

            Extract ONLY this JSON structure. Do not add explanations. Do not write markdown wrappers or any conversational text.
            {
              "customerName": "string or null",
              "date": "DD-MM-YYYY or null",
              "paymentType": "CREDIT" or "CASH",
              "items": [
                {
                  "sku": "matching sku from sku database or TI-MARB-01 as default",
                  "productName": "product name in English or Roman Urdu matching name in sku database",
                  "quantity": number,
                  "unit": "piece / dozen / box / set",
                  "rate": number,
                  "total": number
                }
              ],
              "grandTotal": number,
              "confidence": number
            }

            RULES:
            - If handwriting is unclear, set confidence below 0.6.
            - "Commode", "Basin", "Flush Tank", "Faucet" are common items.
            - Rates are in Pakistani Rupees (PKR).
            - "dz" or "doz" means dozen = 12 pieces.
            - If total is missing but rate and qty exist, calculate: total = rate × qty.
            - If any field is unreadable, use null or defaults. Never guess.

            SKU DATABASE REFERENCE FOR MAPPING:
            $productStr
        """.trimIndent()

        val userMessage = """
            Here is the raw text extracted from the bill:
            ---
            $rawExtractedText
            ---
        """.trimIndent()

        if (strategy == "GEMINI_DIRECT") {
            try {
                return@withContext makeGeminiRequestDirect(systemPrompt, userMessage, true)
            } catch (e: Exception) {
                Log.e("GroqService", "Gemini OCR format failed", e)
            }
        }

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

            // Call Groq Vision OCR Model
            val result = makeGroqApiRequest(ocrModelName, messagesArray, ocrModelFallback)
            
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

    private suspend fun callGeminiWithTools(
        systemInstructionText: String,
        history: List<Pair<String, Boolean>>,
        userInput: String,
        tools: JSONArray,
        toolExecutor: suspend (String, Map<String, Any>) -> String
    ): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        val modelName = "gemini-3.5-flash"
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"

        val contents = JSONArray()

        // Map general conversational history to Gemini roles
        for (turn in history) {
            contents.put(JSONObject().apply {
                put("role", if (turn.second) "user" else "model")
                put("parts", JSONArray().apply {
                    put(JSONObject().apply {
                        put("text", turn.first)
                    })
                })
            })
        }

        // Add current input
        contents.put(JSONObject().apply {
            put("role", "user")
            put("parts", JSONArray().apply {
                put(JSONObject().apply {
                    put("text", userInput)
                })
            })
        })

        // Map tools to Gemini format
        val funcsList = JSONArray()
        for (i in 0 until tools.length()) {
            val toolObj = tools.getJSONObject(i)
            if (toolObj.optString("type") == "function") {
                val funcObj = toolObj.getJSONObject("function")
                val mappedFuncObj = mapFunctionPropertiesToGemini(funcObj)
                funcsList.put(mappedFuncObj)
            }
        }
        val geminiTools = JSONArray().apply {
            put(JSONObject().apply {
                put("functionDeclarations", funcsList)
            })
        }

        val requestBodyJson = JSONObject().apply {
            put("contents", contents)
            put("systemInstruction", JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().apply {
                        put("text", systemInstructionText)
                    })
                })
            })
            put("tools", geminiTools)
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.2)
            })
        }

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = requestBodyJson.toString().toRequestBody(mediaType)

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        client.newCall(request).execute().use { response ->
            val resBody = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                return@withContext "Gemini API error (HTTP ${response.code}): $resBody"
            }

            val responseJson = JSONObject(resBody)
            val candidates = responseJson.optJSONArray("candidates")
            val candidate = candidates?.optJSONObject(0)
            val contentObj = candidate?.optJSONObject("content")
            val parts = contentObj?.optJSONArray("parts")
            val firstPart = parts?.optJSONObject(0)

            if (firstPart != null && firstPart.has("functionCall")) {
                val functionCall = firstPart.getJSONObject("functionCall")
                val name = functionCall.getString("name")
                val argsObj = functionCall.optJSONObject("args")
                val argsMap = mutableMapOf<String, Any>()
                if (argsObj != null) {
                    val keys = argsObj.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        argsMap[key] = argsObj.get(key)
                    }
                }

                // Execute local function against local Room database
                val toolResult = toolExecutor(name, argsMap)

                // Append assistant turn with functionCall
                contents.put(JSONObject().apply {
                    put("role", "model")
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("functionCall", functionCall)
                        })
                    })
                })

                // Append tool response turn
                contents.put(JSONObject().apply {
                    put("role", "function")
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("functionResponse", JSONObject().apply {
                                put("name", name)
                                put("response", JSONObject().apply {
                                    put("result", toolResult)
                                })
                            })
                        })
                    })
                })

                // Request final response from Gemini
                val finalRequestBodyJson = JSONObject().apply {
                    put("contents", contents)
                    put("systemInstruction", JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", systemInstructionText)
                            })
                        })
                    })
                    put("generationConfig", JSONObject().apply {
                        put("temperature", 0.2)
                    })
                }

                val finalRequestBody = finalRequestBodyJson.toString().toRequestBody(mediaType)
                val finalRequest = Request.Builder()
                    .url(url)
                    .post(finalRequestBody)
                    .build()

                client.newCall(finalRequest).execute().use { finalResponse ->
                    val finalResBody = finalResponse.body?.string() ?: ""
                    if (!finalResponse.isSuccessful) {
                        return@withContext "Gemini API tool final response error (HTTP ${finalResponse.code}): $finalResBody"
                    }

                    val finalResponseJson = JSONObject(finalResBody)
                    val fCandidates = finalResponseJson.optJSONArray("candidates")
                    val fCandidate = fCandidates?.optJSONObject(0)
                    val fContentObj = fCandidate?.optJSONObject("content")
                    val fParts = fContentObj?.optJSONArray("parts")
                    val fFirstPart = fParts?.optJSONObject(0)
                    return@withContext fFirstPart?.optString("text") ?: "AI Tool final parsing error."
                }
            } else {
                return@withContext firstPart?.optString("text") ?: "AI did not return text response."
            }
        }
    }

    private suspend fun makeGeminiRequestDirect(
        systemInstructionText: String,
        userInput: String,
        asJson: Boolean = false
    ): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        val modelName = "gemini-3.5-flash"
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"

        val contents = JSONArray().apply {
            put(JSONObject().apply {
                put("role", "user")
                put("parts", JSONArray().apply {
                    put(JSONObject().apply {
                        put("text", userInput)
                    })
                })
            })
        }

        val requestBodyJson = JSONObject().apply {
            put("contents", contents)
            put("systemInstruction", JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().apply {
                        put("text", systemInstructionText)
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.1)
                if (asJson) {
                    put("responseMimeType", "application/json")
                }
            })
        }

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = requestBodyJson.toString().toRequestBody(mediaType)

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        client.newCall(request).execute().use { response ->
            val resBody = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                return@withContext "Error: Gemini API direct failed (HTTP ${response.code}): $resBody"
            }

            val responseJson = JSONObject(resBody)
            val candidates = responseJson.optJSONArray("candidates")
            val candidate = candidates?.optJSONObject(0)
            val contentObj = candidate?.optJSONObject("content")
            val parts = contentObj?.optJSONArray("parts")
            val firstPart = parts?.optJSONObject(0)
            return@withContext firstPart?.optString("text") ?: "AI error."
        }
    }

    private fun mapFunctionPropertiesToGemini(funcObj: JSONObject): JSONObject {
        val result = JSONObject(funcObj.toString())
        if (result.has("parameters")) {
            val params = result.getJSONObject("parameters")
            capitalizeTypeInJson(params)
        }
        return result
    }

    private fun capitalizeTypeInJson(obj: JSONObject) {
        if (obj.has("type")) {
            val t = obj.get("type")
            if (t is String) {
                obj.put("type", t.uppercase())
            }
        }
        if (obj.has("properties")) {
            val props = obj.getJSONObject("properties")
            val keys = props.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val prop = props.optJSONObject(key)
                if (prop != null) {
                    capitalizeTypeInJson(prop)
                }
            }
        }
        if (obj.has("items")) {
            val itemsObj = obj.optJSONObject("items")
            if (itemsObj != null) {
                capitalizeTypeInJson(itemsObj)
            }
        }
    }
}
