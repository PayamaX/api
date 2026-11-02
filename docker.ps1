. "$PSScriptRoot\compose.tag.ps1"

if ([string]::IsNullOrWhiteSpace($tag)) {
    throw "TAG is not set in docker.tag"
}

Remove-Item -Path ".nuget" -Recurse -Force
mkdir .nuget
Copy-Item $env:NUGET_HOME/* .nuget/
docker build --load -t "ghcr.io/farabank/api-local:$tag" -f ./app/Dockerfile ./