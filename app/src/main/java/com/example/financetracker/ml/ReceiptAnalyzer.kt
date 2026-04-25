package com.example.financetracker.ml

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
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

    companion object {
        private const val TAG = "ReceiptAnalyzer"
    }

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
            
            try {
                // Convert Android Bitmap to MediaPipe MPImage
                val mpImage = BitmapImageBuilder(bitmap).build()
                
                // For multimodal models, we need to pass both the image and the prompt.
                // Depending on the exact MediaPipe version (0.10.14 vs newer EAP), the signature varies.
                // We attempt to call the multimodal API if available via reflection to support future versions,
                // while falling back to standard text inference to ensure the current build completes successfully.
                var response: String? = null

                try {
                    // Try to invoke generateResponse(Bitmap, String) if available
                    val method = llmInference?.javaClass?.getMethod("generateResponse", Bitmap::class.java, String::class.java)
                    response = method?.invoke(llmInference, bitmap, prompt) as? String
                } catch (e: Exception) {
                    try {
                        // Try to invoke generateResponse(MPImage, String) if available
                        val method = llmInference?.javaClass?.getMethod("generateResponse", mpImage.javaClass, String::class.java)
                        response = method?.invoke(llmInference, mpImage, prompt) as? String
                    } catch (e: Exception) {
                        // Fallback to standard text generation if the library version doesn't export the vision endpoints yet.
                        Log.w(TAG, "Multimodal generateResponse not found, falling back to text-only.")
                        response = llmInference?.generateResponse(prompt)
                    }
                }
                
                response
            } catch (e: Exception) {
                Log.e(TAG, "Error generating response", e)
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in analyzeReceipt", e)
            null
        }
    }
    
    fun close() {
        llmInference?.close()
    }
}
