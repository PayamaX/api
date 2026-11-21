$envNames = @( & (Join-Path $PSScriptRoot "env.ps1") )
$paxTag = & (Join-Path (Split-Path -Parent $PSScriptRoot) "scripts" "tag.ps1") -Repo $PSScriptRoot

echo $paxTag

$buildArgs = @()
foreach ($name in $envNames) {
    if ([string]::IsNullOrWhiteSpace($name)) {
        continue
    }

    $buildArgs += "--build-arg"
    $buildArgs += $name
}

$homeDirectory = if ($env:HOME) { $env:HOME } else { $env:USERPROFILE }
$mavenLocal = Join-Path $homeDirectory ".m2x"
$mavenLocalRepository = Join-Path $mavenLocal "repository"
if (-not (Test-Path -LiteralPath $mavenLocalRepository)) {
    New-Item -ItemType Directory -Force -Path $mavenLocalRepository | Out-Null
}

docker build --load @buildArgs --build-context "mavenlocal=$mavenLocal" -t "ghcr.io/payamax/api-local:$paxTag" -f ./Dockerfile ./
