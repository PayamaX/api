#!/usr/bin/env bash
# On main, PAX_TAG is "main". Otherwise keep PAX_TAG, or use the current branch when it is unset.
set -eu

branch=""
if [ "${GITHUB_REF:-}" = "refs/heads/main" ]; then
  branch=main
elif [ -n "${GITHUB_HEAD_REF:-}" ]; then
  branch="$GITHUB_HEAD_REF"
elif [ -n "${GITHUB_REF_NAME:-}" ]; then
  branch="$GITHUB_REF_NAME"
else
  branch=$(git branch --show-current 2>/dev/null || true)
fi

branch=$(printf '%s' "$branch" | tr -d '\r' | sed -e 's/^[[:space:]]*//' -e 's/[[:space:]]*$//')

if [ "$branch" = "main" ]; then
  tag=main
else
  tag=$(printf '%s' "${PAX_TAG:-}" | tr -d '\r' | sed -e 's/^[[:space:]]*//' -e 's/[[:space:]]*$//')
  if [ -z "$tag" ]; then
    if [ -z "$branch" ]; then
      echo "PAX_TAG is not set and the current git branch could not be determined." >&2
      exit 1
    fi
    tag="$branch"
  fi
fi

export PAX_TAG="$tag"

if [ -n "${GITHUB_ENV:-}" ]; then
  echo "PAX_TAG=$tag" >> "$GITHUB_ENV"
fi

echo "PAX_TAG: $tag"
