#!/usr/bin/env bash
# Read .env into the process environment. An existing variable is left unchanged.
# When GITHUB_ENV is set, newly applied names are appended for later steps.
set -eu

script_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
dir=$script_dir
env_file=""
while [ -n "$dir" ]; do
  if [ -f "$dir/.env" ]; then
    env_file="$dir/.env"
    break
  fi

  parent=$(dirname -- "$dir")
  if [ "$parent" = "$dir" ]; then
    break
  fi
  dir=$parent
done

if [ -z "$env_file" ]; then
  echo ".env was not found in $script_dir or any parent directory." >&2
  exit 1
fi

while IFS= read -r raw_line || [ -n "$raw_line" ]; do
  line=${raw_line%$'\r'}
  line=${line#"${line%%[![:space:]]*}"}
  line=${line%"${line##*[![:space:]]}"}

  if [ -z "$line" ] || [ "${line#\#}" != "$line" ]; then
    continue
  fi

  case "$line" in
    *=*) ;;
    *) continue ;;
  esac

  name=${line%%=*}
  value=${line#*=}
  name=${name#"${name%%[![:space:]]*}"}
  name=${name%"${name##*[![:space:]]}"}

  if [ "${#value}" -ge 2 ]; then
    first=${value%"${value#?}"}
    last=${value#"${value%?}"}
    if { [ "$first" = '"' ] && [ "$last" = '"' ]; } || { [ "$first" = "'" ] && [ "$last" = "'" ]; }; then
      value=${value:1:${#value}-2}
    fi
  fi

  case "$name" in
    [A-Za-z_][A-Za-z0-9_]*) ;;
    *)
      echo "Skipping invalid environment name: $name" >&2
      continue
      ;;
  esac

  if [ -n "${!name+x}" ]; then
    continue
  fi

  export "$name=$value"

  if [ -n "${GITHUB_ENV:-}" ]; then
    delim="ENV_$(od -An -N8 -tx1 /dev/urandom | tr -d ' \n')"
    {
      printf '%s<<%s\n' "$name" "$delim"
      printf '%s\n' "$value"
      printf '%s\n' "$delim"
    } >> "$GITHUB_ENV"
  fi

  printf '%s\n' "$name"
done < "$env_file"
