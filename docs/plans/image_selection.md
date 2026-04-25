# Implementation Plan: Image Selection (Camera & Gallery)

## Goal
Replace the current FAB stub in `MainActivity` with functional logic to capture a receipt photo via Camera or select an existing one from the Gallery.

## Proposed Changes

### 1. Update Dependencies
Ensure `androidx.activity:activity-compose` is up to date in `app/build.gradle.kts` to use the latest `rememberLauncherForActivityResult`.

### 2. ViewModel Logic (MainViewModel.kt)
- Add a `MutableStateFlow` or `Compose State` to hold the selected `Bitmap` or `Uri`.
- Implement a function `onImageSelected(uri: Uri, context: Context)` that converts the Uri to a Bitmap (using `ImageDecoder` or `BitmapFactory`).

### 3. UI Implementation (MainActivity.kt)
- **Selection Dialog:** Create a simple ModalBottomSheet or AlertDialog that appears when the FAB is clicked, offering "Take Photo" and "Choose from Gallery".
- **Gallery Picker:** Use `rememberLauncherForActivityResult` with `ActivityResultContracts.PickVisualMedia()`.
- **Camera Capture:** Use `rememberLauncherForActivityResult` with `ActivityResultContracts.TakePicture()`. This requires creating a temporary file URI.
- **Permission Handling:** Check and request `android.permission.CAMERA` when the user selects "Take Photo".

### 4. Integration with ReceiptAnalyzer
- Once the `Bitmap` is obtained, pass it to a (new) processing state in the ViewModel.
- For now, Jules should just log the successful capture of the Bitmap. The actual navigation to the Review Screen will be the next task.

## Verification Plan
### Manual Verification
1. Click the FAB button.
2. Select "Gallery" -> Pick an image -> Verify (via Logcat) that the Bitmap is loaded.
3. Select "Camera" -> Grant permission -> Take a photo -> Verify (via Logcat) that the Bitmap is loaded.
