param(
    [switch]$InstallUv
)

$ErrorActionPreference = "Stop"
Set-Location (Join-Path $PSScriptRoot "..")

if (-not $env:UV_CACHE_DIR) {
    $env:UV_CACHE_DIR = Join-Path (Get-Location) ".tmp\uv-cache"
}
New-Item -ItemType Directory -Force -Path $env:UV_CACHE_DIR | Out-Null

if (-not $env:UV_PYTHON_INSTALL_DIR) {
    $env:UV_PYTHON_INSTALL_DIR = Join-Path (Get-Location) ".tmp\uv-python"
}
New-Item -ItemType Directory -Force -Path $env:UV_PYTHON_INSTALL_DIR | Out-Null

function Test-Command($Name) {
    return [bool](Get-Command $Name -ErrorAction SilentlyContinue)
}

$uvBins = @(
    (Join-Path $env:USERPROFILE ".local\bin"),
    (Join-Path $env:USERPROFILE ".cargo\bin")
)
foreach ($uvBin in $uvBins) {
    if ((Test-Path $uvBin) -and ($env:Path -notlike "*$uvBin*")) {
        $env:Path = "$uvBin;$env:Path"
    }
}

if (-not (Test-Command "uv")) {
    if (-not $InstallUv) {
        Write-Error "uv is not installed. Install it first, or run: powershell -ExecutionPolicy Bypass -File scripts/setup_seed_env.ps1 -InstallUv"
    }

    Write-Host "Installing uv..."
    irm https://astral.sh/uv/install.ps1 | iex

    foreach ($uvBin in $uvBins) {
        if (Test-Path $uvBin) {
            $env:Path = "$uvBin;$env:Path"
        }
    }
}

if (-not (Test-Command "uv")) {
    Write-Error "uv was installed but is not available on PATH yet. Open a new terminal and run this script again."
}

uv venv .venv-seed
if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}

uv pip install --python .venv-seed\Scripts\python.exe -r scripts\requirements-seed.txt
if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}

Write-Host "Seed Python environment is ready at .venv-seed"
