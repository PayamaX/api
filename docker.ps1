$envNames = @( & (Join-Path $PSScriptRoot "env.ps1") )
$paxTag = & (Join-Path $PSScriptRoot "tag.ps1")

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
