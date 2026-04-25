# Implementation Plan: AI Model Management System

## Goal
Implement a system to check for the presence of the Gemma AI model on the device and download it automatically if missing. This avoids bundling large binary files in the APK.

## Proposed Changes

### 1. New Component: `ModelManager.kt`
- **Check Existence:** Function to verify if `gemma-4-E2B-it-int4.task` exists in `context.filesDir`.
- **Download Logic:** Use `DownloadManager` or a simple Coroutine-based HTTP client (like OkHttp) to download the model from a remote URL.
- **Progress Tracking:** Provide a `Flow<DownloadStatus>` to the UI to show progress (percentage, bytes downloaded).
- **Validation:** Verify the file size or checksum after download to ensure integrity.

### 2. UI Updates
- **Download Overlay:** If the model is missing, show a full-screen overlay or a specialized screen before the user can use the "Scan" feature.
- **Progress Bar:** Display a clear progress bar with the text: "Downloading AI Model (~450MB). Please stay on this screen..."

### 3. Integration with `ReceiptAnalyzer`
- Update the `ReceiptAnalyzer` constructor to accept a file path dynamically.
- Ensure `ReceiptAnalyzer` is only initialized AFTER the model is confirmed to be on disk.

### 4. Configuration
- Define the `MODEL_URL` in a configuration object or `BuildConfig`.
- Default Model: `gemma-4-E2B-it-int4-task`.

## Verification Plan
### Manual Verification
1. Clear app data/cache.
2. Launch the app.
3. Verify that the download starts automatically or when clicking the scan button.
4. Verify that the progress bar updates correctly.
5. Verify that `ReceiptAnalyzer` initializes correctly after the download finishes.
