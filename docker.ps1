$envNames = @( & (Join-Path $PSScriptRoot "env.ps1") )
& (Join-Path $PSScriptRoot "tag.ps1")

$buildArgs = @()
foreach ($name in $envNames) {
    if ([string]::IsNullOrWhiteSpace($name)) {
        continue
    }

    $buildArgs += "--build-arg"
    $buildArgs += $name
}

docker build --load @buildArgs -t "ghcr.io/payamax/api-local:$env:PAX_TAG" -f ./Dockerfile ./
