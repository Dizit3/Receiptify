# Guidelines for AI Agents

This file contains important context and instructions for AI agents working on this project.

## Core Directives

1. **Strictly Offline Processing:** The primary goal of this app is to be a 100% offline personal finance tracker. **Do not** introduce any cloud-based APIs, external network calls for data processing, or traditional OCR services (like ML Kit) for receipt analysis.
2. **On-Device Multimodal AI:** The app relies on the **Gemma-4-E2B-it** edge model. All receipt parsing must be done by passing the raw `Bitmap` image and prompt directly into the local model using the LiteRT-LM / MediaPipe Tasks GenAI framework.
3. **Data Privacy:** Treat all financial data as highly sensitive. Ensure that all processing is done locally and data is stored securely in the local Room database.

## Architecture and Patterns

- **UI Framework:** Use **Jetpack Compose** with Material 3 for all UI components. Avoid traditional XML layouts.
- **Architecture:** Adhere strictly to the **MVVM (Model-View-ViewModel)** pattern.
- **Asynchronous Operations:** Use **Coroutines** and **Flow** for managing background tasks and state streams.
- **Local Storage:** Use **Room Database** for persistence.

## Development Guidelines

### Room Database and Gson
- When implementing Room `TypeConverter`s that use Gson (e.g., to serialize lists of items), **prefer reusing a single, shared, thread-safe Gson instance** and `TypeToken`s. This minimizes reflection and instantiation overhead, improving performance.

### Error Handling
- To prevent Information Exposure via Stack Trace, **always use** `android.util.Log.e(TAG, "message", exception)` instead of `exception.printStackTrace()` for error handling.

### Multimodal Inference
- When interacting with `ReceiptAnalyzer` or similar classes, be aware that the MediaPipe API might be evolving to support multimodal inputs (`Bitmap` + `String` prompt). Use the appropriate `generateResponse` overload provided by the library version in use (`0.10.14` or newer).
- The prompt used for inference must be strictly: "Analyze this receipt image. Extract the data and return strictly a JSON object containing: `shop_name` (String), `date` (YYYY-MM-DD), `total_amount` (Double), and `items` (Array of objects with `name` (String) and `price` (Double)). Do not add markdown or explanations."

### Environment and Build
- The project development environment uses JDK 17.
- Gradle commands in this environment are prone to timeouts and network failures. If the Gradle wrapper fails to download, a pre-installed global `gradle` (version 8.8) is available as a fallback.
- GitHub Actions workflows are used for CI. Remember that the `secrets` context is not directly available in step-level `if` conditionals; map them to environment variables first.
- The repository uses a custom `.githooks` directory. Enable them via `git config core.hooksPath .githooks` or `scripts/setup.sh` (if available).
