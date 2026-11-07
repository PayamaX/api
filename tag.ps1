$branch = git -C $PSScriptRoot branch --show-current
if ($LASTEXITCODE -ne 0) {
    throw "Could not determine the current git branch."
}

$branch = "$branch".Trim()

if ($branch -eq "main") {
    $env:PAX_TAG = "main"
}
elseif ([string]::IsNullOrWhiteSpace($env:PAX_TAG)) {
    if ([string]::IsNullOrWhiteSpace($branch)) {
        throw "PAX_TAG is not set and the current git branch could not be determined."
    }

    $env:PAX_TAG = $branch
}
