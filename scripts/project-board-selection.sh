#!/usr/bin/env bash
# Prints the GitHub Project v2 board settings used to add issues and pull requests of one repository.
# Inputs, read from the environment, each possibly empty:
#   REPOSITORY_VARIABLE_PROJECT_V2_ID                          the repository variable PROJECT_V2_ID
#   REPOSITORY_VARIABLE_PROJECT_STATUS_FIELD_ID                the repository variable PROJECT_STATUS_FIELD_ID
#   REPOSITORY_VARIABLE_PROJECT_AWAITING_WORKSPACE_OPTION_ID   the repository variable PROJECT_AWAITING_WORKSPACE_OPTION_ID
#   REPOSITORY_VARIABLE_PROJECT_CLEAR_FIELD_ID                 the repository variable PROJECT_CLEAR_FIELD_ID
# When PROJECT_V2_ID is empty, the UMINO board defaults apply. When it is set, the status field and the
# awaiting-workspace option must be set too, and the clear field is optional.
# Output: one KEY=value line per setting, for appending to GITHUB_OUTPUT or for eval.
set -euo pipefail

CUSTOM_PROJECT_V2_ID="${REPOSITORY_VARIABLE_PROJECT_V2_ID:-}"
CUSTOM_STATUS_FIELD_ID="${REPOSITORY_VARIABLE_PROJECT_STATUS_FIELD_ID:-}"
CUSTOM_AWAITING_WORKSPACE_OPTION_ID="${REPOSITORY_VARIABLE_PROJECT_AWAITING_WORKSPACE_OPTION_ID:-}"
CUSTOM_CLEAR_FIELD_ID="${REPOSITORY_VARIABLE_PROJECT_CLEAR_FIELD_ID:-}"

if [ -z "$CUSTOM_PROJECT_V2_ID" ]; then
  if [ -n "$CUSTOM_STATUS_FIELD_ID$CUSTOM_AWAITING_WORKSPACE_OPTION_ID$CUSTOM_CLEAR_FIELD_ID" ]; then
    echo "PROJECT_V2_ID must be set when another project variable is set" >&2
    exit 1
  fi
  PROJECT_V2_ID=PVT_kwHOAGJHa84AFWnr
  STATUS_FIELD_ID=PVTSSF_lAHOAGJHa84AFWnrzgDFcxE
  AWAITING_WORKSPACE_OPTION_ID=d1c19cce
  CLEAR_FIELD_ID=PVTF_lAHOAGJHa84AFWnrzgIk_H0
  SET_STATUS_WHEN_ADDED_BY_URL=false
else
  if [ -z "$CUSTOM_STATUS_FIELD_ID" ] || [ -z "$CUSTOM_AWAITING_WORKSPACE_OPTION_ID" ]; then
    echo "PROJECT_STATUS_FIELD_ID and PROJECT_AWAITING_WORKSPACE_OPTION_ID must be set together with PROJECT_V2_ID" >&2
    exit 1
  fi
  for SETTING_NAME in PROJECT_V2_ID STATUS_FIELD_ID AWAITING_WORKSPACE_OPTION_ID CLEAR_FIELD_ID; do
    case "$SETTING_NAME" in
      PROJECT_V2_ID) SETTING_VALUE="$CUSTOM_PROJECT_V2_ID" ;;
      STATUS_FIELD_ID) SETTING_VALUE="$CUSTOM_STATUS_FIELD_ID" ;;
      AWAITING_WORKSPACE_OPTION_ID) SETTING_VALUE="$CUSTOM_AWAITING_WORKSPACE_OPTION_ID" ;;
      CLEAR_FIELD_ID) SETTING_VALUE="$CUSTOM_CLEAR_FIELD_ID" ;;
    esac
    if [ -n "$SETTING_VALUE" ] && ! [[ "$SETTING_VALUE" =~ ^[A-Za-z0-9_]+$ ]]; then
      echo "$SETTING_NAME must be a plain project identifier made of letters, digits and underscores" >&2
      exit 1
    fi
  done
  PROJECT_V2_ID="$CUSTOM_PROJECT_V2_ID"
  STATUS_FIELD_ID="$CUSTOM_STATUS_FIELD_ID"
  AWAITING_WORKSPACE_OPTION_ID="$CUSTOM_AWAITING_WORKSPACE_OPTION_ID"
  CLEAR_FIELD_ID="$CUSTOM_CLEAR_FIELD_ID"
  SET_STATUS_WHEN_ADDED_BY_URL=true
fi

printf 'PROJECT_V2_ID=%s\n' "$PROJECT_V2_ID"
printf 'STATUS_FIELD_ID=%s\n' "$STATUS_FIELD_ID"
printf 'AWAITING_WORKSPACE_OPTION_ID=%s\n' "$AWAITING_WORKSPACE_OPTION_ID"
printf 'CLEAR_FIELD_ID=%s\n' "$CLEAR_FIELD_ID"
printf 'SET_STATUS_WHEN_ADDED_BY_URL=%s\n' "$SET_STATUS_WHEN_ADDED_BY_URL"
