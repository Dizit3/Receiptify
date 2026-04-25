# Implementation Plan: Review Screen (Data Validation)

## Goal
Implement a screen to display, edit, and confirm data extracted from a receipt by the AI model before saving it to the local database.

## Proposed Changes

### 1. Data Model Updates
- Ensure `Transaction` and `ReceiptItem` are easily passable between screens (using Parcelable or a SharedViewModel).

### 2. UI Implementation (`ReviewScreen.kt`)
- **TopBar:** Title "Review Receipt" and a "Save" icon button.
- **Shop Info Section:**
    - `OutlinedTextField` for `shopName`.
    - `OutlinedTextField` for `date` (with a DatePicker ideally).
    - `OutlinedTextField` for `totalAmount` (numeric).
- **Items List Section:**
    - A header "Items".
    - A lazy list of items, each with an editable `name` and `price`.
    - A "Delete" button for each item.
    - An "Add Item" button at the bottom of the list.
- **Image Preview (Optional/Later):** A small expandable section to view the original receipt photo for reference.

### 3. ViewModel Logic (`ReviewViewModel.kt`)
- **State Management:** Hold a `MutableState<Transaction>` that updates as the user edits fields.
- **Validation:** Basic checks (e.g., total amount is a valid number, shop name is not empty).
- **Persistence:** Call `transactionDao.insert()` when the user clicks "Save".

### 4. Navigation Flow
- Update `MainActivity` to navigate to the `ReviewScreen` once `ReceiptAnalyzer.analyzeReceipt()` returns a result.
- After saving, navigate back to the History List (MainActivity).

## Verification Plan
### Manual Verification
1. Mock a successful AI response.
2. Verify the `ReviewScreen` opens with pre-filled data.
3. Edit the shop name and price of one item.
4. Delete one item.
5. Click "Save".
6. Verify the updated transaction appears in the History List on the main screen.
