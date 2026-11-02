# On main, TAG is "main". Otherwise use the TAG environment variable, then docker.tag.
$branch = "$(git -C $PSScriptRoot branch --show-current)".Trim()

if ($branch -eq "main") {
    $tag = "main"
}
elseif (-not [string]::IsNullOrWhiteSpace($env:TAG)) {
    $tag = $env:TAG.Trim()
}
else {
    $tagFile = Join-Path $PSScriptRoot "docker.tag"
    $tagLine = $null
    if (Test-Path -LiteralPath $tagFile) {
        $tagLine = Get-Content -LiteralPath $tagFile | Where-Object { $_ -match '^\s*TAG\s*=' } | Select-Object -First 1
    }

    $tag = if ($tagLine) { ($tagLine -replace '^\s*TAG\s*=\s*', '').Trim().Trim('"').Trim("'") } else { "" }

    if ([string]::IsNullOrWhiteSpace($tag)) {
        throw "TAG is not set. Set the TAG environment variable or add TAG to docker.tag."
    }
}

$env:TAG = $tag
$env:FB_AP_VER = $tag
Write-Host "TAG: $tag"
