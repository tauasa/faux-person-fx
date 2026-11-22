<#
.SYNOPSIS
Starts faux-person-fx. Builds the fat jar first when it's missing or older
than the source, or when -Rebuild is given.

.DESCRIPTION
From a PowerShell prompt (Windows PowerShell 5.1 or PowerShell 7):
  ./run.ps1                              build if needed, then start
  ./run.ps1 -Rebuild                     clean build, then start (--rebuild works too)
  ./run.ps1 --faux.fx.max-count=50000    other arguments go to the app

From a shell: pwsh run.ps1, or powershell -File run.ps1. If scripts are
blocked, add -ExecutionPolicy Bypass before -File.

FAUX_FX_JAVA_OPTS passes options to the JVM, e.g. $env:FAUX_FX_JAVA_OPTS = '-Xmx2g'
#>

# No param() block, on purpose: PowerShell would match any single-dash
# argument against it by prefix, so app arguments pass through $args as typed.

$ErrorActionPreference = 'Stop'

function Fail([string] $message) {
    [Console]::Error.WriteLine($message)
    exit 1
}

$rebuild = $false
$appArgs = @()
foreach ($arg in $args) {
    if ($arg -eq '-Rebuild' -or $arg -eq '--rebuild') { $rebuild = $true } else { $appArgs += "$arg" }
}

# Push-Location, not Set-Location: the caller's prompt stays where it was.
Push-Location -LiteralPath $PSScriptRoot
try {
    $jar = Join-Path 'target' 'faux-person-fx.jar'

    if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
        Fail 'java not found. Install JDK 17 or later and put it on PATH.'
    }
    # java -version writes to stderr; "22.0.1" -> 22, and the old style "1.8.0_391" -> 8.
    # Windows PowerShell 5.1 turns a native command's stderr into errors, which
    # 'Stop' would throw on, so relax it for this one call.
    $ErrorActionPreference = 'Continue'
    $versionLine = (& java -version 2>&1 | Select-Object -First 1)
    $ErrorActionPreference = 'Stop'
    $version = if ("$versionLine" -match 'version "([^"]+)"') { $Matches[1] } else { '' }
    $parts = $version -split '\.'
    $major = 0
    if ($parts.Count -gt 0 -and [int]::TryParse($parts[0], [ref] $major) -and $major -eq 1 -and $parts.Count -gt 1) {
        [void] [int]::TryParse($parts[1], [ref] $major)
    }
    if ($major -lt 17) {
        $shown = if ($version) { $version } else { 'unknown' }
        Fail "java on PATH is version $shown; faux-person-fx needs 17 or later."
    }

    $stale = $true
    if (Test-Path -LiteralPath $jar) {
        $built = (Get-Item -LiteralPath $jar).LastWriteTime
        $newer = @(Get-Item -LiteralPath 'pom.xml') + @(Get-ChildItem -LiteralPath 'src' -Recurse -File) |
            Where-Object { $_.LastWriteTime -gt $built } |
            Select-Object -First 1
        $stale = $null -ne $newer
    }

    if ($rebuild -or $stale) {
        if (-not (Get-Command mvn -ErrorAction SilentlyContinue)) {
            Fail 'mvn not found. Install Maven 3.6.3 or later to build the app.'
        }
        $goals = if ($rebuild) { @('clean', 'package') } else { @('package') }
        Write-Host "Building $jar ..."
        # The tests run in mvn verify; starting the app shouldn't wait on them.
        & mvn -q -B -DskipTests @goals
        if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
    }

    $javaOpts = @()
    if ($env:FAUX_FX_JAVA_OPTS) {
        $javaOpts = @($env:FAUX_FX_JAVA_OPTS -split '\s+' | Where-Object { $_ })
    }
    & java @javaOpts -jar $jar @appArgs
    exit $LASTEXITCODE
}
finally {
    Pop-Location
}
