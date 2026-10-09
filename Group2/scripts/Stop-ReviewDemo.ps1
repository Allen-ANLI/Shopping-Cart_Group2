# Author: OpenAI Codex (stop only the background demo launched during local review).
$ErrorActionPreference = 'Stop'
$projectPath = Split-Path -Parent $PSScriptRoot
$pidFile = Join-Path $projectPath '.local\demo-launcher.pid'
if (-not (Test-Path -LiteralPath $pidFile)) {
    Write-Host 'No review background demo recorded. Stop a normal launch with Ctrl+C in its window.'
    exit 0
}
$launcherPid = 0
if (-not [int]::TryParse((Get-Content -LiteralPath $pidFile -Raw).Trim(), [ref]$launcherPid) -or $launcherPid -le 0) {
    throw 'Invalid recorded demo launcher PID.'
}
$allowedJars = @(
    (Join-Path $projectPath 'runtime\shopping-cart.jar'),
    (Join-Path $projectPath 'target\shopping-cart-0.0.1-SNAPSHOT.jar')
)
$demoProcesses = @(Get-CimInstance Win32_Process -Filter "ParentProcessId=$launcherPid AND Name='java.exe'" | Where-Object {
    $commandLine = $_.CommandLine
    $hasProjectJar = $false
    foreach ($jarPath in $allowedJars) {
        if ($commandLine -and $commandLine.IndexOf($jarPath, [StringComparison]::OrdinalIgnoreCase) -ge 0) {
            $hasProjectJar = $true
        }
    }
    $hasProjectJar -and $commandLine.Contains('--spring.profiles.active=b-demo')
})
foreach ($demoProcess in $demoProcesses) {
    [System.Diagnostics.Process]::GetProcessById([int]$demoProcess.ProcessId).Kill()
    Write-Host "Stopped this project's review demo: PID $($demoProcess.ProcessId)."
}
if ($demoProcesses.Count -eq 0) { Write-Host 'The recorded background demo is no longer running.' }
