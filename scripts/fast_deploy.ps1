# Fast Deploy Script (Local Build -> GitHub Release)
$ErrorActionPreference = "Stop"

Write-Host "Starting Fast Deploy..." -ForegroundColor Cyan

# 1. Build the APK locally
Write-Host "Building APK..." -ForegroundColor Yellow
./gradlew assembleDebug

$apkPath = "app/build/outputs/apk/debug/app-debug.apk"
if (-not (Test-Path $apkPath)) {
    Write-Error "APK not found at $apkPath"
}

# 2. Generate a unique tag based on timestamp
$timestamp = Get-Date -Format "yyyyMMdd-HHmm"
$branch = git rev-parse --abbrev-ref HEAD
$tag = "v-local-$branch-$timestamp"

Write-Host "Creating GitHub Release with tag: $tag" -ForegroundColor Yellow

# 3. Use GitHub CLI to create release and upload APK
& "C:\Program Files\GitHub CLI\gh.exe" release create $tag $apkPath --title "Local Build: $tag" --notes "Fast deploy from local machine. Branch: $branch" --prerelease

Write-Host "Done! APK is now available on GitHub and Obtainium should see it." -ForegroundColor Green
