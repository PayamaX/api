#!/usr/bin/env bash
# Exit 0 when api migrations can be copied into services the way db-sync.yml does.
# Files already in services must be unchanged. Files only in api must use a
# V<index> greater than the highest index already in services.
set -euo pipefail

if [ "$#" -ne 2 ]; then
  echo "usage: check-migrations.sh <api-migrations-dir> <services-migrations-dir>" >&2
  exit 2
fi

src="$1"
dst="$2"

if [ ! -d "$src" ]; then
  echo "::error::No migrations directory at ${src}."
  exit 1
fi

# Print the numeric Flyway version from V<digits>__<description>.sql, or return 1.
migration_index() {
  local name="$1"
  local digits rest
  case "$name" in
    V*__*.sql) ;;
    *) return 1 ;;
  esac
  digits="${name#V}"
  digits="${digits%%__*}"
  case "$digits" in
    ''|*[!0-9]*) return 1 ;;
  esac
  rest="${name#V${digits}__}"
  rest="${rest%.sql}"
  if [ -z "$rest" ]; then
    return 1
  fi
  echo "$((10#$digits))"
}

shopt -s nullglob

failed=0
max=0
index_list="$(mktemp)"
trap 'rm -f "$index_list"' EXIT

if [ -d "$dst" ]; then
  for file in "$dst"/*.sql; do
    name="$(basename "$file")"
    if ! ver="$(migration_index "$name")"; then
      echo "::error::${name} in services does not match V<index>__description.sql."
      failed=1
      continue
    fi
    if [ "$ver" -gt "$max" ]; then
      max=$ver
    fi
  done
else
  echo "Services has no migrations directory yet."
fi

if [ "$failed" -ne 0 ]; then
  exit 1
fi

new_count=0
for file in "$src"/*.sql; do
  name="$(basename "$file")"
  if ! ver="$(migration_index "$name")"; then
    echo "::error::${name} does not match V<index>__description.sql."
    failed=1
    continue
  fi
  printf '%s\t%s\n' "$ver" "$name" >> "$index_list"

  target="${dst}/${name}"
  if [ -f "$target" ]; then
    if ! cmp -s "$file" "$target"; then
      echo "::error::${name} already exists in services with different content. Applied migrations cannot be edited."
      failed=1
    fi
    continue
  fi

  if [ "$ver" -le "$max" ]; then
    echo "::error::${name} is a new migration but index ${ver} is not greater than current max index ${max}."
    failed=1
    continue
  fi

  echo "New migration ${name} (index ${ver}) is greater than current max index ${max}."
  new_count=$((new_count + 1))
done

if [ -d "$dst" ]; then
  for file in "$dst"/*.sql; do
    name="$(basename "$file")"
    if [ ! -f "${src}/${name}" ]; then
      echo "::error::${name} is missing from this pull request. Applied migrations cannot be removed."
      failed=1
    fi
  done
fi

if [ -s "$index_list" ]; then
  dups="$(awk -F '\t' '{print $1}' "$index_list" | sort -n | uniq -d)"
  if [ -n "$dups" ]; then
    while IFS= read -r ver; do
      [ -z "$ver" ] && continue
      names="$(awk -F '\t' -v v="$ver" '$1 == v { printf "%s%s", sep, $2; sep=", " }' "$index_list")"
      echo "::error::Version index ${ver} is used by more than one migration: ${names}."
      failed=1
    done <<< "$dups"
  fi
fi

if [ "$failed" -ne 0 ]; then
  exit 1
fi

if [ "$new_count" -eq 0 ]; then
  echo "Migrations match services (max index ${max}). Nothing new to sync."
else
  echo "Migrations can be synced (${new_count} new, current max index ${max})."
fi
