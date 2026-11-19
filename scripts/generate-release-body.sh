#!/usr/bin/env bash
set -euo pipefail

if [ "$#" -ne 5 ]; then
  echo "usage: $0 <current-tag> <commit-sha> <version-name> <build-number> <recent-tags-file>" >&2
  exit 2
fi

current_tag="$1"
commit_sha="$2"
version_name="$3"
build_number="$4"
recent_tags_file="$5"

if [ ! -f "$recent_tags_file" ]; then
  echo "Recent tags file '$recent_tags_file' does not exist" >&2
  exit 1
fi

previous_tag=""
while IFS= read -r candidate_tag; do
  if [ -n "$candidate_tag" ] && [ "$candidate_tag" != "$current_tag" ]; then
    previous_tag="$candidate_tag"
    break
  fi
done < "$recent_tags_file"

if ! change_lines="$("$(dirname "$0")/generate-release-notes.sh" "$previous_tag" "$commit_sha")"; then
  echo "Failed to generate change lines for '$current_tag'" >&2
  exit 1
fi

if [ -n "$previous_tag" ]; then
  heading="## Changes since $previous_tag"
else
  heading="## Changes since the first release"
fi

if [ -z "$change_lines" ]; then
  change_lines="No changes since the previous release."
fi

printf 'Automated build of main at %s (version %s, build %s).\n' "${commit_sha:0:7}" "$version_name" "$build_number"
printf '\n%s\n' "Install the APK matching your device ABI; termux-app_arm64-v8a.apk is recommended. termux-app_universal.apk works on all ABIs. The in-app updater reads this release via the anonymous releases/latest API."
printf '\n%s\n\n' "$heading"
printf '%s\n' "$change_lines"
