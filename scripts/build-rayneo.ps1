param([Parameter(Mandatory=$true)][string]$JavaHome)
$ErrorActionPreference = 'Stop'
$repo = (Resolve-Path "$PSScriptRoot/..").Path
$java = Join-Path $JavaHome 'bin/java.exe'
if (!(Test-Path $java)) { throw "Java executable missing: $java" }
$previousJava = $env:JAVA_HOME
$env:JAVA_HOME = (Resolve-Path $JavaHome).Path
Push-Location $repo
try {
    & ./gradlew.bat :app:assembleNonRootDebug :app:testNonRootDebugUnitTest :app:lintNonRootDebug --console=plain
    if ($LASTEXITCODE -ne 0) { throw 'Gradle verification failed' }
    & ./tests/rayneo/run.ps1
    if ($LASTEXITCODE -ne 0) { throw 'Policy regression tests failed' }
    & python -m unittest discover -s tests -p test_capture_rayneo.py -v
    if ($LASTEXITCODE -ne 0) { throw 'Capture regression failed' }
    New-Item -ItemType Directory -Force out | Out-Null
    $metadata = Get-Content 'app/build/outputs/apk/nonRoot/debug/output-metadata.json' -Raw | ConvertFrom-Json
    $apk = Join-Path 'out' ('Moonlight-' + $metadata.elements[0].versionName + '-debug.apk')
    Copy-Item 'app/build/outputs/apk/nonRoot/debug/app-nonRoot-debug.apk' $apk
    Get-FileHash $apk -Algorithm SHA256
} finally {
    Pop-Location
    $env:JAVA_HOME = $previousJava
}
