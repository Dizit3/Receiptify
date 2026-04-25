# Implementation Plan: Fix AI Status Bar and Verify Inference

## Goal
Improve the UI by adding a status indicator for AI operations and ensure the inference logic correctly parses the AI output.

## Proposed Changes

### 1. UI: AI Status Bar
- **Component:** Create an `AIStatusBar` composable that displays the model's status (Ready, Downloading, Analyzing, Error).
- **Integration:** Add this bar just below the `TopAppBar` in `MainActivity.kt`.
- **Visibility:** It should be visible whenever the model is downloading or analyzing. When ready, it could show a small indicator or be hidden.
- **Refactoring:** Move the full-screen `AI Analysis Overlay` to a less intrusive indicator if possible, or keep it as an option.

### 2. Logic: Verify and Fix Inference
- **Data Model:** Update `Transaction` and `ReceiptItem` in `Transaction.kt` to use `@SerializedName` for matching the AI's snake_case output (`shop_name`, `total_amount`).
- **Prompt:** Refine the prompt in `ReceiptAnalyzer.kt` to be more explicit about the JSON format and match the data model.
- **JSON Parsing:** Use a shared `Gson` instance in `MainActivity.kt` for better performance, as recommended in `AGENTS.md`.
- **Inference Logic:** Enhance `ReceiptAnalyzer.kt` to handle the multimodal inference more robustly, adding better logging for debug purposes.

### 3. Verification & Testing
- **Unit Test:** Create `TransactionParsingTest.kt` to verify that a sample JSON from the AI is correctly parsed into a `Transaction` object.
- **Manual Verification:** (Self-check) Ensure the UI transitions correctly between states.

## Steps
1. Create `docs/plans/fix_status_bar.md` with this plan.
2. Modify `Transaction.kt` to add `@SerializedName`.
3. Create a unit test for JSON parsing.
4. Modify `MainActivity.kt` to:
    - Add `AIStatusBar` component.
    - Use a shared `Gson` instance.
    - Improve state management for the status bar.
5. Modify `ReceiptAnalyzer.kt` to refine the prompt and logging.
6. Verify the build and run tests.
