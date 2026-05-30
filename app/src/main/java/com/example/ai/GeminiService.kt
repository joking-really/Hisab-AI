package com.example.ai

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.example.data.CustomerEntity
import com.example.data.AccountEntity
import com.example.data.ProductEntity
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GeminiService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val apiKey = BuildConfig.GEMINI_API_KEY

    // Model names as specified in instructions
    private val modelName = "gemini-3.5-flash"

    suspend fun getChatResponse(
        userInput: String,
        history: List<Pair<String, Boolean>>, // Pair(Message, isUser)
        customers: List<CustomerEntity>,
        accounts: List<AccountEntity>,
        products: List<ProductEntity>
    ): String = withContext(Dispatchers.IO) {
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "API Key missing or set to placeholder. Please configure your GEMINI_API_KEY in the Secrets / .env panel to enable Hisab Assistant AI."
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

            When the user wants to record something (e.g., Ali Traders bought 2 Commodes on credit, or Imran paid 10,000, or staff salary Rs. 15,000 paid):
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

        try {
            val contentsArray = JSONArray()

            // Add history
            for (turn in history) {
                val contentObj = JSONObject()
                contentObj.put("role", if (turn.second) "user" else "model")
                val partsObj = JSONArray()
                partsObj.put(JSONObject().put("text", turn.first))
                contentObj.put("parts", partsObj)
                contentsArray.put(contentObj)
            }

            // Add current input
            val currentContentObj = JSONObject()
            currentContentObj.put("role", "user")
            val currentPartsObj = JSONArray()
            currentPartsObj.put(JSONObject().put("text", userInput))
            currentContentObj.put("parts", currentPartsObj)
            contentsArray.put(currentContentObj)

            val requestBodyJson = JSONObject()
            requestBodyJson.put("contents", contentsArray)
            requestBodyJson.put("systemInstruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", systemInstructionText))))

            // Add temperature to config
            val genConfig = JSONObject()
            genConfig.put("temperature", 0.2)
            requestBodyJson.put("generationConfig", genConfig)

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBodyStr = requestBodyJson.toString()
            val requestBody = requestBodyStr.toRequestBody(mediaType)

            val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errBody = response.body?.string() ?: ""
                    Log.e("GeminiService", "API Error: ${response.code} $errBody")
                    return@withContext "Error calling Hisab Assistant: ${response.message} (HTTP ${response.code})."
                }

                val resBody = response.body?.string() ?: return@withContext "Error: empty response received from AI."
                val responseJson = JSONObject(resBody)
                val candidates = responseJson.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val firstCandidate = candidates.getJSONObject(0)
                    val content = firstCandidate.getJSONObject("content")
                    val parts = content.getJSONArray("parts")
                    if (parts.length() > 0) {
                        return@withContext parts.getJSONObject(0).optString("text")
                    }
                }
                "AI did not return any text response. Please try matching your request again."
            }
        } catch (e: Exception) {
            Log.e("GeminiService", "Exception in getChatResponse", e)
            "A technical error occurred while contacting Hisab AI assistant: ${e.localizedMessage}"
        }
    }

    suspend fun parseParchiOcr(
        imageBitmap: Bitmap,
        products: List<ProductEntity>
    ): String = withContext(Dispatchers.IO) {
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "{\"error\": \"API Key missing. Configure in Secrets panel.\"}"
        }

        // Convert Bitmap to Base64
        val baos = ByteArrayOutputStream()
        imageBitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos)
        val base64Image = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)

        val productStr = products.joinToString("; ") { "SKU: ${it.sku} - ${it.name} (Sale Price: Rs. ${it.salePrice}, Cost: Rs. ${it.unitCost})" }

        val textPrompt = """
            You are "Parchi OCR Scanner", a professional structured extractor for a ceramics tile and bathroom fittings distributor in Karachi.
            Scan the handwritten or printed invoice/receipt in the image.
            
            Extract the following details as a valid JSON object matching the exact structure below:
            - Customer Name (or "Cash Customer" if not specified)
            - Date (ISO string or DD-MM-YYYY format, if visible)
            - Items (Name, SKU, Quantity, Unit Rate, Total)
            - Grand Total
            - Payment Type ("CASH" or "CREDIT")

            Available SKU Database to map names securely:
            $productStr

            Respond ONLY with a valid JSON structured block like this (no markdown, no code fencing):
            {
              "customerName": "...",
              "date": "...",
              "paymentType": "CREDIT" | "CASH",
              "items": [
                 {"sku": "SKU_CODE_HERE", "productName": "Exact SKU Name", "quantity": 10, "rate": 1500, "total": 15000}
              ],
              "grandTotal": 15000
            }

            Rule: If raw items do not match any SKU exactly, pick the closest SKU from the database supplied, or create a plausible matching SKU, but strive to match SKUs. Keep prices and totals mathematically consistent!
        """.trimIndent()

        try {
            val partsArray = JSONArray()
            partsArray.put(JSONObject().put("text", textPrompt))
            
            val inlineDataObj = JSONObject()
            inlineDataObj.put("mimeType", "image/jpeg")
            inlineDataObj.put("data", base64Image)
            partsArray.put(JSONObject().put("inlineData", inlineDataObj))

            val contentObj = JSONObject()
            contentObj.put("parts", partsArray)

            val contentsArray = JSONArray().put(contentObj)

            val requestBodyJson = JSONObject()
            requestBodyJson.put("contents", contentsArray)

            // Dynamic format constraint
            val genConfig = JSONObject()
            genConfig.put("temperature", 0.1)
            val formatObj = JSONObject()
            formatObj.put("mimeType", "application/json")
            genConfig.put("responseFormat", formatObj)
            requestBodyJson.put("generationConfig", genConfig)

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = requestBodyJson.toString().toRequestBody(mediaType)

            val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errBody = response.body?.string() ?: ""
                    Log.e("GeminiService", "OCR Error: ${response.code} $errBody")
                    return@withContext "{\"error\": \"Server API call failed with code ${response.code}.\"}"
                }

                val resBody = response.body?.string() ?: return@withContext "{\"error\": \"Empty body from OCR service.\"}"
                val responseJson = JSONObject(resBody)
                val candidates = responseJson.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val firstCandidate = candidates.getJSONObject(0)
                    val content = firstCandidate.getJSONObject("content")
                    val parts = content.getJSONArray("parts")
                    if (parts.length() > 0) {
                        return@withContext parts.getJSONObject(0).optString("text")
                    }
                }
                "{\"error\": \"No candidates parsed by OCR engine.\"}"
            }
        } catch (e: Exception) {
            Log.e("GeminiService", "OCR Exception", e)
            "{\"error\": \"OCR failed with exception: ${e.localizedMessage}\"}"
        }
    }
}
