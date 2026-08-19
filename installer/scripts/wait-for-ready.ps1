param(
    [Parameter(Mandatory = $true)]
    [string]$Url,

    [Parameter(Mandatory = $true)]
    [string]$StateFile,

    [ValidateRange(1, 600)]
    [int]$TimeoutSeconds = 90,

    [ValidateRange(250, 10000)]
    [int]$IntervalMilliseconds = 1500
)

$ErrorActionPreference = 'Stop'

function Publish-State([string]$State) {
    $temporaryFile = "$StateFile.tmp"
    [System.IO.File]::WriteAllText($temporaryFile, "STATE=$State`r`n")
    Move-Item -LiteralPath $temporaryFile -Destination $StateFile -Force
}

Publish-State 'POLLING'
$timer = [System.Diagnostics.Stopwatch]::StartNew()

while ($timer.Elapsed.TotalSeconds -lt $TimeoutSeconds) {
    try {
        $response = Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec 2
        if ($response.StatusCode -eq 200) {
            $payload = $response.Content | ConvertFrom-Json
            if ($payload.status -eq 'UP') {
                Publish-State 'READY'
                exit 0
            }
        }
    }
    catch {
        # Connection failures are expected while the service is starting.
    }

    Start-Sleep -Milliseconds $IntervalMilliseconds
}

Publish-State 'TIMEOUT'
exit 1
