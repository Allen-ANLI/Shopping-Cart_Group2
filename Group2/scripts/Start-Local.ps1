# Author: OpenAI Codex (local setup assistance; team should review before submission).
[CmdletBinding()]
param(
    [ValidateSet('Demo', 'MySQL')][string]$Mode = 'Demo',
    [ValidateRange(1, 65535)][int]$Port = 0,
    [string]$DatabaseUrl
)
$ErrorActionPreference = 'Stop'
$projectPath = Split-Path -Parent $PSScriptRoot
$javaCommand = Get-Command java -ErrorAction SilentlyContinue
if (-not $javaCommand) { throw 'Java is missing from PATH. Install JDK 17 or newer, then reopen the terminal.' }
$jars = @(
    (Join-Path $projectPath 'target\shopping-cart-0.0.1-SNAPSHOT.jar'),
    (Join-Path $projectPath 'runtime\nexus-shopping-cart.jar'),
    (Join-Path $projectPath 'runtime\shopping-cart.jar')
) | Where-Object { Test-Path -LiteralPath $_ } | ForEach-Object { Get-Item -LiteralPath $_ }
$jar = $jars | Sort-Object LastWriteTime -Descending | Select-Object -First 1
if (-not $jar) { throw 'No runnable JAR found. Run mvnw.cmd package first.' }
if ($Port -eq 0) { $Port = if ($Mode -eq 'Demo') { 18083 } else { 18080 } }
$probe = New-Object System.Net.Sockets.TcpClient
try {
    $probe.Connect('127.0.0.1', $Port)
    throw "Port $Port is already in use. If the store is running, open http://127.0.0.1:$Port/products. Otherwise choose another -Port."
} catch [System.Net.Sockets.SocketException] {
    # No listener; Java may now bind the port.
} finally { $probe.Dispose() }
$arguments = @('-jar', $jar.FullName, "--server.port=$Port", '--server.address=127.0.0.1')
$oldUsername = $env:DB_USERNAME
$oldPassword = $env:DB_PASSWORD
$oldUrl = $env:DB_URL
try {
    if ($Mode -eq 'Demo') {
        $arguments += '--spring.profiles.active=b-demo'
        Write-Host 'Demo database is in memory. Data is lost when the application stops.'
        Write-Host 'Customer: alice / demo123; administrator: admin / admin123'
    } else {
        # The explicit profile also prevents an inherited b-demo profile from overriding MySQL.
        $arguments += '--spring.profiles.active=mysql'
        if (-not $DatabaseUrl) { $DatabaseUrl = $env:DB_URL }
        if (-not $DatabaseUrl) { $DatabaseUrl = 'jdbc:mysql://127.0.0.1:3306/shopping_cart' }
        if (-not $DatabaseUrl.StartsWith('jdbc:mysql://')) { throw 'DatabaseUrl must be a MySQL JDBC URL.' }
        $env:DB_URL = $DatabaseUrl
        if (-not $env:DB_USERNAME) {
            $env:DB_USERNAME = Read-Host 'MySQL username (Enter for root)'
            if (-not $env:DB_USERNAME) { $env:DB_USERNAME = 'root' }
        }
        if ($null -eq $env:DB_PASSWORD) {
            $passwordInput = Read-Host 'MySQL password' -AsSecureString
            $env:DB_PASSWORD = [System.Net.NetworkCredential]::new('', $passwordInput).Password
        }
        Write-Host "Database: $DatabaseUrl"
        Write-Host 'Create shopping_cart in MySQL Workbench first (database/create-database.sql).'
    }
    Write-Host "JAR: $($jar.FullName)"
    Write-Host "Open http://127.0.0.1:$Port/products after the Started message. Press Ctrl+C to stop."
    & $javaCommand.Source @arguments
    if ($LASTEXITCODE -ne 0) { throw "Java exited with code $LASTEXITCODE. Check the error shown above." }
} finally {
    $env:DB_USERNAME = $oldUsername
    $env:DB_PASSWORD = $oldPassword
    $env:DB_URL = $oldUrl
}
