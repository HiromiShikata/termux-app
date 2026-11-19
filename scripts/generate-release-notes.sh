#!/usr/bin/env bash
set -euo pipefail

if [ "$#" -ne 2 ]; then
  echo "usage: $0 <previous-tag-or-empty> <head-ref>" >&2
  exit 2
fi

previous_ref="$1"
head_ref="$2"

if [ -z "$previous_ref" ]; then
  git log -1 --format='- %s' "$head_ref"
  exit 0
fi

if ! git rev-parse --verify --quiet "$previous_ref^{commit}" >/dev/null; then
  echo "Previous release tag '$previous_ref' does not resolve to a commit" >&2
  exit 1
fi

git log --reverse --no-merges --format='- %s' "$previous_ref..$head_ref"
