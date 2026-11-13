# Version 261113.1144

$branch = git -C $PSScriptRoot branch --show-current 2>$null
if ($LASTEXITCODE -eq 0) {
    $branch = "$branch".Trim()
    if (-not [string]::IsNullOrWhiteSpace($branch)) {
        return $branch
    }
}

$tag = "$env:PAX_TAG".Trim()
if (-not [string]::IsNullOrWhiteSpace($tag)) {
    return $tag
}

throw "PAX_TAG is not set and the current git branch could not be determined."
