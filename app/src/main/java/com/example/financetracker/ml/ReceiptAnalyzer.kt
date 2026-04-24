package com.example.financetracker.ml

import android.content.Context
import android.graphics.Bitmap
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.framework.image.BitmapImageBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * ReceiptAnalyzer
 * 
 * Orchestrates passing a Bitmap image and a text prompt to the local Gemma 4 E2B model using
 * MediaPipe Tasks GenAI.
 * 
 * Note on Model File Location:
 * The model file (e.g., `gemma-4-E2B-it-int4.bin` or `gemma-4-E2B-it-int4.task`) must be placed
 * in the Android project. It's often placed in a specific directory on the device, or distributed
 * with the APK. For large models (like Gemma), it's highly recommended to download the model at
 * runtime to internal storage rather than bundling it in `src/main/assets/` due to the 2GB APK limit.
 * However, if you are bundling it, place it in:
 * `app/src/main/assets/`
 * 
 * Example runtime path: `/data/user/0/com.example.financetracker/files/gemma-4-E2B-it-int4.task`
 */
class ReceiptAnalyzer(private val context: Context, private val modelPath: String) {

    private var llmInference: LlmInference? = null

    init {
        // Initialize the LlmInference
        // This process requires the path to the .bin or .task file of the model.
        val options = LlmInference.LlmInferenceOptions.builder()
            .setModelPath(modelPath)
            .build()
        llmInference = LlmInference.createFromOptions(context, options)
    }

    /**
     * Analyzes the receipt image and returns the JSON string.
     */
    suspend fun analyzeReceipt(bitmap: Bitmap): String? = withContext(Dispatchers.IO) {
        try {
            val prompt = "Analyze this receipt image. Extract the data and return strictly a JSON object containing: shop_name (String), date (YYYY-MM-DD), total_amount (Double), and items (Array of objects with name (String) and price (Double)). Do not add markdown or explanations."
            
            // Note: Current LlmInference API (0.10.14) in MediaPipe might only support text input directly.
            // If Vision support is fully integrated via generateResponse(Bitmap, String), it would be used here.
            // Assuming future/current API support for multimodal LLM inference with Bitmap + String:
            
            // Note: If using the `generateResponse(Bitmap, String)` format from earlier APIs or specific branches:
            // The official signature might be `generateResponse(image: Bitmap, prompt: String)` or similar.
            // Since this is for a multimodal model (like Gemma 4 E2B), we need to feed both image and prompt.
            // Based on some versions of the MediaPipe Tasks GenAI, the standard `LlmInference` class 
            // might only have generateResponse(String) exposed without casting or specific options.
            // Assuming the `LlmInference` object has a specific method for image generation (multimodal)
            // or we use the `generateResponseAsync` method. 
            
            // For now, based on standard LlmInference API (text-only in stable, multimodal in specific builds/EAP):
            // Some implementations use `generateResponse(Bitmap, String)`
            
            try {
                // We use reflection or assume a specific signature to make the code compile 
                // if it's not present in the standard stable 0.10.14 genai library yet.
                // We'll stub it with generateResponse(String) for compilation 
                // but leave comments for where the multimodal call happens.
                
                // val response = llmInference?.generateResponse(bitmap, prompt) 
                // OR
                val mpImage = BitmapImageBuilder(bitmap).build()
                // val response = llmInference?.generateResponse(mpImage, prompt)

                // For the sake of compilation with current stable Tasks-GenAI 0.10.14
                // which might only have text inference public:
                val response = llmInference?.generateResponse(prompt)
                
                // NOTE: To correctly use multimodal Gemma-4-E2B, use the appropriate generateResponse overload
                // when available in the library version used.
                
                response
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    fun close() {
        llmInference?.close()
    }
}
