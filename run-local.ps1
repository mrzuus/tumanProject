param(
    [string]$JavaHome = $env:JAVA_HOME,
    [string]$MavenCommand
)
$ErrorActionPreference = 'Stop'
if (-not $JavaHome) {
    $javaCommand = Get-Command java -ErrorAction SilentlyContinue
    if ($javaCommand) { $JavaHome = Split-Path (Split-Path $javaCommand.Source -Parent) -Parent }
}
if (-not $JavaHome -or -not (Test-Path -LiteralPath (Join-Path $JavaHome 'bin/javac.exe'))) {
    throw 'A JDK is required. Set JAVA_HOME or pass -JavaHome with your JDK directory.'
}
if (-not $MavenCommand) { $MavenCommand = Join-Path $PSScriptRoot 'mvnw.cmd' }
if (-not (Test-Path -LiteralPath $MavenCommand)) { throw 'Maven launcher not found.' }
$previousJavaHome = $env:JAVA_HOME
try {
    $env:JAVA_HOME = $JavaHome
    Push-Location $PSScriptRoot
    try {
        & $MavenCommand '-ntp' 'compile' 'javafx:run'
        if ($LASTEXITCODE -ne 0) { throw 'Application startup failed. See the output above.' }
    } finally { Pop-Location }
} finally { $env:JAVA_HOME = $previousJavaHome }
