$ErrorActionPreference = 'Stop'
$repo = (Resolve-Path "$PSScriptRoot/../..").Path
$testOutput = Join-Path $repo 'build/rayneo-tests'
New-Item -ItemType Directory -Force $testOutput | Out-Null
$sources = @("$repo/app/src/main/java/com/limelight/ui/rayneo/RayNeoPolicy.java", "$repo/app/src/main/java/com/limelight/ui/rayneo/TempleGesture.java", "$PSScriptRoot/PolicyTest.java", "$PSScriptRoot/GestureTest.java")
$javac = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME 'bin/javac.exe' } else { 'javac' }
$java = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME 'bin/java.exe' } else { 'java' }
& $javac -d $testOutput @sources
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
& $java -cp $testOutput com.limelight.ui.rayneo.PolicyTest
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
& $java -cp $testOutput com.limelight.ui.rayneo.GestureTest
exit $LASTEXITCODE
