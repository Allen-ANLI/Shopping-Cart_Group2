[CmdletBinding()]
param([int]$Port = 18092)
$ErrorActionPreference = 'Stop'
$project = Split-Path -Parent $PSScriptRoot
$jar = Join-Path $project 'runtime\nexus-shopping-cart.jar'
if (-not (Test-Path -LiteralPath $jar)) { throw "Runnable JAR not found: $jar" }
$java = Get-Command java -ErrorAction SilentlyContinue
if (-not $java) { throw 'Java is missing from PATH. Install JDK 17 or newer, then reopen the terminal.' }
$probe = New-Object Net.Sockets.TcpClient
try { $probe.Connect('127.0.0.1',$Port); throw "Port $Port is already in use. Open http://127.0.0.1:$Port/products or choose another port." }
catch [Net.Sockets.SocketException] { }
finally { $probe.Dispose() }
$url = "http://127.0.0.1:$Port/products"
$logDir = Join-Path $project '.local'
New-Item -ItemType Directory -Path $logDir -Force | Out-Null
$stdoutLog = Join-Path $logDir "h2-$Port.out.log"
$stderrLog = Join-Path $logDir "h2-$Port.err.log"
Write-Host 'Starting isolated H2 demo database (data resets when the app stops)...'
Write-Host 'Administrator: admin / admin123   Customer: alice / demo123'
$args = @('-jar',$jar,'--spring.profiles.active=b-demo',"--server.port=$Port",'--server.address=127.0.0.1')
$javaArgs = ($args | ForEach-Object { '"' + ($_ -replace '"','\"') + '"' }) -join ' '
$launch = 'start "NEXUS H2" /b "' + $java.Source + '" ' + $javaArgs + ' > "' + $stdoutLog + '" 2> "' + $stderrLog + '"'
cmd.exe /d /c $launch | Out-Null
$process = $null
try {
    $ready = $false
    for ($i=0; $i -lt 60; $i++) {
        Start-Sleep -Milliseconds 500
        try { $response = Invoke-WebRequest -Uri $url -UseBasicParsing -TimeoutSec 2; if ($response.StatusCode -ge 200) { $ready = $true; break } } catch { }
    }
    if (-not $ready) { throw "The application did not become ready. Check .local/h2-$Port.out.log and .local/h2-$Port.err.log." }
    Write-Host "Application is ready at $url"
    # Use Explorer's URL handler so this also works when PowerShell is launched by a .cmd file.
    & explorer.exe $url
    Write-Host "Browser opened: $url"
    Write-Host 'Leave this window open while using the site. Press Ctrl+C to stop.'
    while ($true) { Start-Sleep -Seconds 2 }
} finally {
    Write-Host 'The browser window can be closed separately. Stop the Java process when finished.'
}
