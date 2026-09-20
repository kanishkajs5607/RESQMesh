# Lightweight Gradle bootstrap. Does not change the machine execution policy.
$ErrorActionPreference = "Stop"
$root = Split-Path $PSScriptRoot -Parent
Set-Location $root
$installed = Get-Command gradle -ErrorAction SilentlyContinue
if ($installed) {
    & $installed.Source @args
    exit $LASTEXITCODE
}
$cacheRoot = if ($env:GRADLE_USER_HOME) { $env:GRADLE_USER_HOME } else { Join-Path $env:USERPROFILE ".gradle" }
$cache = Join-Path $cacheRoot "resqmesh-bootstrap"
$gradle = Join-Path $cache "gradle-8.9\bin\gradle.bat"
if (!(Test-Path $gradle)) {
    New-Item -ItemType Directory -Force -Path $cache | Out-Null
    $archive = Join-Path $cache "gradle.zip"
    Invoke-WebRequest "https://services.gradle.org/distributions/gradle-8.9-bin.zip" -OutFile $archive
    $expected = (Invoke-WebRequest "https://services.gradle.org/distributions/gradle-8.9-bin.zip.sha256" -UseBasicParsing).Content.Trim()
    $actual = (Get-FileHash $archive -Algorithm SHA256).Hash
    if ($actual -ne $expected) { throw "Gradle checksum mismatch" }
    Expand-Archive -Path $archive -DestinationPath $cache -Force
}
& $gradle @args
exit $LASTEXITCODE
