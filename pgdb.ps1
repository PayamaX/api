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
$mavenLocal = Join-Path $homeDirectory ".m2"
if (-not (Test-Path -LiteralPath (Join-Path $mavenLocal "repository"))) {
    throw "Maven repository was not found at $mavenLocal"
}

docker build --load @buildArgs --build-context "mavenlocal=$mavenLocal" -t "ghcr.io/payamax/api-local:$paxTag" -f ./Dockerfile ./
