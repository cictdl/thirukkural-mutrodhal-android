# Builds the signed release APK and Play Store bundle (AAB) and copies them to .\release
$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot

if (-not $env:JAVA_HOME -or -not (Test-Path (Join-Path $env:JAVA_HOME 'bin\java.exe'))) {
    $env:JAVA_HOME = 'C:\Program Files\Java\jdk-23'
}
if (-not (Test-Path (Join-Path $root 'keystore.properties'))) {
    Write-Warning 'keystore.properties not found: the release build will be unsigned.'
}

Push-Location $root
try {
    & .\gradlew.bat clean assembleRelease bundleRelease
    if ($LASTEXITCODE -ne 0) { throw 'Gradle build failed.' }

    $gradleFile = Get-Content (Join-Path $root 'app\build.gradle.kts') -Raw
    $versionName = [regex]::Match($gradleFile, 'versionName\s*=\s*"([^"]+)"').Groups[1].Value
    $versionCode = [regex]::Match($gradleFile, 'versionCode\s*=\s*(\d+)').Groups[1].Value
    $name = "Thirukkural-Mutrodhal-v$versionName-$versionCode"

    $out = Join-Path $root 'release'
    New-Item -ItemType Directory -Force $out | Out-Null
    Copy-Item 'app\build\outputs\apk\release\app-release.apk' (Join-Path $out "$name.apk") -Force
    Copy-Item 'app\build\outputs\bundle\release\app-release.aab' (Join-Path $out "$name.aab") -Force
    # Keep the web copies of the privacy policy identical to the one bundled in the app.
    # docs/ is published by GitHub Pages.
    Copy-Item 'app\src\main\assets\privacy-policy.html' 'store-listing\privacy-policy.html' -Force
    Copy-Item 'app\src\main\assets\privacy-policy.html' 'docs\privacy-policy.html' -Force
    Get-ChildItem $out | Format-Table Name, @{ n = 'MB'; e = { [math]::Round($_.Length / 1MB, 2) } } -AutoSize
} finally {
    Pop-Location
}
