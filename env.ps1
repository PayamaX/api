$dir = $PSScriptRoot
$envFile = $null
while ($dir) {
    $candidate = Join-Path $dir ".env"
    if (Test-Path -LiteralPath $candidate) {
        $envFile = $candidate
        break
    }

    $parent = Split-Path -Path $dir -Parent
    if (-not $parent -or ($parent -eq $dir)) {
        break
    }
    $dir = $parent
}

if (-not $envFile) {
    throw ".env was not found in $PSScriptRoot or any parent directory."
}

$names = @()
foreach ($rawLine in Get-Content -LiteralPath $envFile) {
    $line = $rawLine.Trim()
    if ($line.Length -eq 0 -or $line.StartsWith("#")) {
        continue
    }

    $eq = $line.IndexOf("=")
    if ($eq -lt 1) {
        continue
    }

    $name = $line.Substring(0, $eq).Trim()
    $value = $line.Substring($eq + 1)
    if (
        ($value.Length -ge 2) -and
        (($value.StartsWith('"') -and $value.EndsWith('"')) -or ($value.StartsWith("'") -and $value.EndsWith("'")))
    ) {
        $value = $value.Substring(1, $value.Length - 2)
    }

    if ($null -eq [Environment]::GetEnvironmentVariable($name)) {
        Set-Item -Path "Env:$name" -Value $value
    }
    $names += $name
}

$names
