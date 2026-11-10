$branch = git -C $PSScriptRoot branch --show-current
if ($LASTEXITCODE -ne 0) {
    throw "Could not determine the current git branch."
}

$branch = "$branch".Trim()

if ($branch -eq "main") {
    $env:PAX_TAG = "main"
}
else {
    $envFile = Join-Path $PSScriptRoot "+env"
    if (-not (Test-Path -LiteralPath $envFile)) {
        throw "+env was not found in $PSScriptRoot."
    }

    $tag = $null
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
        if ($name -ne "PAX_TAG") {
            continue
        }

        $value = $line.Substring($eq + 1)
        if (
            ($value.Length -ge 2) -and
            (($value.StartsWith('"') -and $value.EndsWith('"')) -or ($value.StartsWith("'") -and $value.EndsWith("'")))
        ) {
            $value = $value.Substring(1, $value.Length - 2)
        }

        $tag = $value
        break
    }

    if ([string]::IsNullOrWhiteSpace($tag)) {
        if ([string]::IsNullOrWhiteSpace($branch)) {
            throw "PAX_TAG is not set and the current git branch could not be determined."
        }

        $env:PAX_TAG = $branch
    }
    else {
        $env:PAX_TAG = $tag
    }
}
