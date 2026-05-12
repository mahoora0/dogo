param(
    [string]$DumpFile,
    [switch]$ResetDb
)

$ErrorActionPreference = "Stop"
Set-Location (Join-Path $PSScriptRoot "..")

function Read-DotEnv {
    $envPath = Join-Path (Get-Location) ".env"
    if (-not (Test-Path $envPath)) {
        return
    }

    Get-Content $envPath | ForEach-Object {
        $line = $_.Trim()
        if (-not $line -or $line.StartsWith("#") -or -not $line.Contains("=")) {
            return
        }

        $parts = $line.Split("=", 2)
        $name = $parts[0].Trim()
        $value = $parts[1].Trim().Trim("'").Trim('"')
        if ($name -and -not [Environment]::GetEnvironmentVariable($name, "Process")) {
            [Environment]::SetEnvironmentVariable($name, $value, "Process")
        }
    }
}

function Get-EnvOrDefault($Name, $Default) {
    $value = [Environment]::GetEnvironmentVariable($Name, "Process")
    if ([string]::IsNullOrWhiteSpace($value)) {
        return $Default
    }
    return $value
}

Read-DotEnv

if ([string]::IsNullOrWhiteSpace($DumpFile)) {
    $latestDump = Get-ChildItem -Path "seed" -Filter "*.sql.gz" -File -ErrorAction SilentlyContinue |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First 1

    if (-not $latestDump) {
        Write-Error "No dump file found under seed/*.sql.gz. Pass a dump path explicitly."
    }

    $DumpFile = $latestDump.FullName
}

if (-not (Test-Path $DumpFile)) {
    Write-Error "Dump file not found: $DumpFile"
}

$python = Join-Path (Get-Location) ".venv-seed\Scripts\python.exe"
if (-not (Test-Path $python)) {
    Write-Error "Seed Python environment is missing. Run: powershell -ExecutionPolicy Bypass -File scripts/setup_seed_env.ps1"
}

$args = @("scripts\import_seed_to_dev.py", $DumpFile)
if ($ResetDb) {
    $args += "--reset-db"
}

& $python @args
if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}
