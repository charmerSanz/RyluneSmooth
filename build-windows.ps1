$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath $PSScriptRoot

Write-Host '=== RyluneSmooth | Fabric 1.21.11 build ===' -ForegroundColor Cyan
try {
    & cmd.exe /c 'java -version 2>&1' | Out-Host
    if ($LASTEXITCODE -ne 0) { throw 'java not found' }
    & javac -version | Out-Host
    if ($LASTEXITCODE -ne 0) { throw 'javac not found' }
} catch { throw 'JDK 21 is required. Install JDK 21 and restart this terminal.' }

$version = '9.2.1'
$tools = Join-Path $env:LOCALAPPDATA 'RyluneSmoothBuildTools'
$gradleBin = Join-Path $tools "gradle-$version\bin\gradle.bat"
if (-not (Test-Path $gradleBin)) {
    New-Item -Path $tools -ItemType Directory -Force | Out-Null
    $archive = Join-Path $tools "gradle-$version-bin.zip"
    $url = "https://services.gradle.org/distributions/gradle-$version-bin.zip"
    Write-Host "Downloading Gradle $version once from $url" -ForegroundColor Yellow
    Invoke-WebRequest -Uri $url -OutFile $archive -UseBasicParsing
    Write-Host 'Extracting Gradle...' -ForegroundColor Yellow
    Expand-Archive -Path $archive -DestinationPath $tools -Force
    Remove-Item -LiteralPath $archive -Force
}

Write-Host 'Building and remapping your mod...' -ForegroundColor Cyan
& $gradleBin --no-daemon build
if ($LASTEXITCODE -ne 0) { throw 'Gradle build failed. Read the actual error lines above.' }

$output = Join-Path $PSScriptRoot 'output'
New-Item -Path $output -ItemType Directory -Force | Out-Null
$jar = Get-ChildItem -Path (Join-Path $PSScriptRoot 'build\libs') -Filter '*.jar' |
    Where-Object { $_.Name -notmatch 'sources|dev|javadoc' } |
    Sort-Object LastWriteTime -Descending | Select-Object -First 1
if (-not $jar) { throw 'Build finished but no distributable JAR was found.' }
Copy-Item -LiteralPath $jar.FullName -Destination (Join-Path $output $jar.Name) -Force
Write-Host ''
Write-Host "SUCCESS: $output\$($jar.Name)" -ForegroundColor Green
Write-Host 'Copy that JAR into your Minecraft mods folder alongside Fabric API.'
