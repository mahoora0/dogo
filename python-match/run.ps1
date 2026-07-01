# 1. Sync virtual environment first to ensure .venv and torch are fully installed
Write-Host "Syncing virtual environment..."
uv sync

# 2. Check and patch MSVC Redist DLLs if outdated
Write-Host "Checking for MSVC Redistributable DLL compatibility..."
$systemMsvcp = "C:\Windows\System32\msvcp140.dll"
$systemVersion = [version]"0.0.0.0"
if (Test-Path $systemMsvcp) {
    $versionStr = (Get-Item $systemMsvcp).VersionInfo.ProductVersion
    if ($versionStr -match '^\d+(\.\d+)+') {
        $systemVersion = [version]$Matches[0]
    }
}

$minRequiredVersion = [version]"14.30.0.0" # VS 2022 minimum version
if ($systemVersion -lt $minRequiredVersion) {
    Write-Host "System VC++ Redist version ($systemVersion) is outdated (< 14.30). Patching locally..."
    
    $candidates = @(
        "C:\Program Files\JetBrains\IntelliJ IDEA *\jbr\bin",
        "C:\Program Files\Java\jdk-*\bin",
        "$env:USERPROFILE\AppData\Roaming\uv\python\cpython-3.12-*\*",
        "$env:USERPROFILE\AppData\Roaming\uv\python\cpython-3.14-*\*"
    )
    
    $bestFolder = $null
    $bestVersion = [version]"0.0.0.0"
    
    foreach ($pathPattern in $candidates) {
        $matchingPaths = Resolve-Path $pathPattern -ErrorAction SilentlyContinue
        foreach ($p in $matchingPaths) {
            $folder = $p.Path
            $msvcp = Join-Path $folder "msvcp140.dll"
            $vcrun = Join-Path $folder "vcruntime140.dll"
            $vcrun1 = Join-Path $folder "vcruntime140_1.dll"
            
            if ((Test-Path $msvcp) -and (Test-Path $vcrun) -and (Test-Path $vcrun1)) {
                $versionStr = (Get-Item $msvcp).VersionInfo.ProductVersion
                if ($versionStr -match '^\d+(\.\d+)+') {
                    $ver = [version]$Matches[0]
                    if ($ver -gt $bestVersion) {
                        $bestVersion = $ver
                        $bestFolder = $folder
                    }
                }
            }
        }
    }
    
    if ($bestFolder) {
        $destFolder = ".venv\Lib\site-packages\torch\lib"
        if (Test-Path $destFolder) {
            Copy-Item -Path (Join-Path $bestFolder "msvcp140.dll") -Destination $destFolder -Force
            Copy-Item -Path (Join-Path $bestFolder "vcruntime140.dll") -Destination $destFolder -Force
            Copy-Item -Path (Join-Path $bestFolder "vcruntime140_1.dll") -Destination $destFolder -Force
            Write-Host "DLL compatibility patch applied successfully from: $bestFolder"
        }
    } else {
        Write-Warning "Could not locate modern VC++ Redistributable DLLs (>= 14.30) on this system."
    }
} else {
    Write-Host "System VC++ Redist is compatible. No patching required."
}

# 3. Setup environment and run the application
$env:MATCH_MODEL_BACKEND = "onnx"
$env:MATCH_ONNX_SAVE_DIR = ".onnx\kosimcse"
$env:MATCH_ONNX_FILE_NAME = "model_int8.onnx"

if (-not $env:CLIP_ONNX_ENABLED) {
    $env:CLIP_ONNX_ENABLED = "false"
}
if (-not $env:CLIP_ONNX_MODEL_PATH) {
    $env:CLIP_ONNX_MODEL_PATH = ".onnx\pet-image\pet-image-qint8.onnx"
}
if (-not $env:CLIP_EMBEDDING_MODEL_NAME -and $env:CLIP_ONNX_ENABLED -in @("1", "true", "yes", "on")) {
    $env:CLIP_EMBEDDING_MODEL_NAME = "AvitoTech/Zer0int-CLIP-L-for-animal-identification:onnx-int8"
}

uv run uvicorn app.main:app --host 0.0.0.0 --port 8001
