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

docker build --load @buildArgs -t "ghcr.io/payamax/api-local:$paxTag" -f ./Dockerfile ./
