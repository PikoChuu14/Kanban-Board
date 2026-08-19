$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$backendRoot = Join-Path $projectRoot 'backend'

Write-Host 'Starting Kovax FlowOps development backend on http://localhost:8081'
Write-Host 'Profile: dev | Database: kanban_db | PostgreSQL role: postgres (override in backend/config/dev-secrets.properties)'

Push-Location $backendRoot
try {
    $env:SPRING_PROFILES_ACTIVE = 'dev'
    $env:SERVER_PORT = '8081'
    $env:SPRING_DATASOURCE_URL = 'jdbc:postgresql://localhost:5432/kanban_db'
    & .\mvnw.cmd spring-boot:run
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
}
finally {
    Pop-Location
}
