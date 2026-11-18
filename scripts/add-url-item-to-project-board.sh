#!/usr/bin/env bash
# Adds the issue or pull request at a GitHub URL to a GitHub Project v2 board.
# The board is the one named by the target repository's variables, or the UMINO default when they are unset.
# Usage: GH_TOKEN=<token> bash scripts/add-url-item-to-project-board.sh <issue-or-pull-request-url>
set -euo pipefail

SCRIPT_DIRECTORY="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
: "${GH_TOKEN:?GH_TOKEN is not set}"

ISSUE_OR_PR_URL="${1:-}"
ISSUE_OR_PR_URL="${ISSUE_OR_PR_URL%%\?*}"
ISSUE_OR_PR_URL="${ISSUE_OR_PR_URL%/}"

if ! echo "$ISSUE_OR_PR_URL" | grep -qE '^https://github\.com/[^/]+/[^/]+/(issues|pull)/[0-9]+$'; then
  echo "Invalid URL format. Expected: https://github.com/<owner>/<repo>/issues/<number> or .../pull/<number>"
  echo "Got: $ISSUE_OR_PR_URL"
  exit 1
fi

REPO_PATH=$(echo "$ISSUE_OR_PR_URL" | awk -F'/' '{print $4"/"$5}')
NUMBER=$(echo "$ISSUE_OR_PR_URL" | grep -oE '[0-9]+$')
if echo "$ISSUE_OR_PR_URL" | grep -q "/pull/"; then
  RESOURCE_TYPE="pulls"
else
  RESOURCE_TYPE="issues"
fi

# Reading the item first also proves the token can access the repository, so a 404 below means "variable not defined".
REST_RESPONSE=$(curl -s -w "\n%{http_code}" -H "Authorization: token $GH_TOKEN" \
  "https://api.github.com/repos/$REPO_PATH/$RESOURCE_TYPE/$NUMBER")
HTTP_STATUS=$(echo "$REST_RESPONSE" | tail -n1)
REST_BODY=$(echo "$REST_RESPONSE" | sed '$d')
if [ "$HTTP_STATUS" != "200" ]; then
  echo "REST API returned HTTP $HTTP_STATUS"
  echo "$REST_BODY"
  exit 1
fi

NODE_ID=$(echo "$REST_BODY" | jq -r '.node_id')
if [ -z "$NODE_ID" ] || [ "$NODE_ID" = "null" ]; then
  echo "Failed to get node ID. REST response:"
  echo "$REST_BODY"
  exit 1
fi
echo "Node ID: $NODE_ID"

# Sets the shell variable named by $2 to the value of the repository variable $1 of REPO_PATH; empty when HTTP 404.
fetch_repository_variable() {
  local variable_name="$1"
  local target_shell_variable="$2"
  local response status body value
  response=$(curl -s -w "\n%{http_code}" -H "Authorization: token $GH_TOKEN" \
    "https://api.github.com/repos/$REPO_PATH/actions/variables/$variable_name")
  status=$(echo "$response" | tail -n1)
  body=$(echo "$response" | sed '$d')
  case "$status" in
    200)
      value=$(echo "$body" | jq -r '.value')
      printf -v "$target_shell_variable" '%s' "$value"
      ;;
    404)
      printf -v "$target_shell_variable" '%s' ''
      ;;
    *)
      echo "Failed to read variable $variable_name of $REPO_PATH: HTTP $status"
      echo "$body"
      exit 1
      ;;
  esac
}

fetch_repository_variable PROJECT_V2_ID REPOSITORY_VARIABLE_PROJECT_V2_ID
fetch_repository_variable PROJECT_STATUS_FIELD_ID REPOSITORY_VARIABLE_PROJECT_STATUS_FIELD_ID
fetch_repository_variable PROJECT_AWAITING_WORKSPACE_OPTION_ID REPOSITORY_VARIABLE_PROJECT_AWAITING_WORKSPACE_OPTION_ID
fetch_repository_variable PROJECT_CLEAR_FIELD_ID REPOSITORY_VARIABLE_PROJECT_CLEAR_FIELD_ID

# The selection output is read as data, one KEY=value line at a time; it is never evaluated as shell code.
BOARD_SETTINGS=$(REPOSITORY_VARIABLE_PROJECT_V2_ID="$REPOSITORY_VARIABLE_PROJECT_V2_ID" \
  REPOSITORY_VARIABLE_PROJECT_STATUS_FIELD_ID="$REPOSITORY_VARIABLE_PROJECT_STATUS_FIELD_ID" \
  REPOSITORY_VARIABLE_PROJECT_AWAITING_WORKSPACE_OPTION_ID="$REPOSITORY_VARIABLE_PROJECT_AWAITING_WORKSPACE_OPTION_ID" \
  REPOSITORY_VARIABLE_PROJECT_CLEAR_FIELD_ID="$REPOSITORY_VARIABLE_PROJECT_CLEAR_FIELD_ID" \
  bash "$SCRIPT_DIRECTORY/project-board-selection.sh")

PROJECT_V2_ID=""
STATUS_FIELD_ID=""
AWAITING_WORKSPACE_OPTION_ID=""
SET_STATUS_WHEN_ADDED_BY_URL=""
while IFS='=' read -r SETTING_NAME SETTING_VALUE; do
  case "$SETTING_NAME" in
    PROJECT_V2_ID) PROJECT_V2_ID="$SETTING_VALUE" ;;
    STATUS_FIELD_ID) STATUS_FIELD_ID="$SETTING_VALUE" ;;
    AWAITING_WORKSPACE_OPTION_ID) AWAITING_WORKSPACE_OPTION_ID="$SETTING_VALUE" ;;
    SET_STATUS_WHEN_ADDED_BY_URL) SET_STATUS_WHEN_ADDED_BY_URL="$SETTING_VALUE" ;;
  esac
done <<< "$BOARD_SETTINGS"
if [ -z "$PROJECT_V2_ID" ] || [ -z "$SET_STATUS_WHEN_ADDED_BY_URL" ]; then
  echo "The project board settings could not be resolved"
  exit 1
fi

GRAPHQL_PAYLOAD=$(jq -n \
  --arg nodeId "$NODE_ID" \
  --arg projectId "$PROJECT_V2_ID" \
  '{"query": "mutation($contentId: ID!, $projectId: ID!) { addProjectV2ItemById(input: { contentId: $contentId, projectId: $projectId }) { item { id } } }", "variables": {"contentId": $nodeId, "projectId": $projectId}}')

RESPONSE=$(curl -s -X POST \
  -H "Authorization: bearer $GH_TOKEN" \
  -H "Content-Type: application/json" \
  https://api.github.com/graphql \
  -d "$GRAPHQL_PAYLOAD")
echo "$RESPONSE"

if ! echo "$RESPONSE" | jq '.' > /dev/null 2>&1; then
  echo "Non-JSON response received"
  exit 1
fi

if echo "$RESPONSE" | jq -e '.errors' > /dev/null; then
  echo "GraphQL error occurred"
  exit 1
fi

if [ "$SET_STATUS_WHEN_ADDED_BY_URL" = "true" ]; then
  ITEM_ID=$(echo "$RESPONSE" | jq -r '.data.addProjectV2ItemById.item.id')
  if [ -z "$ITEM_ID" ] || [ "$ITEM_ID" = "null" ]; then
    echo "No project item id returned, cannot set status"
    exit 1
  fi

  STATUS_PAYLOAD=$(jq -n \
    --arg projectId "$PROJECT_V2_ID" \
    --arg itemId "$ITEM_ID" \
    --arg fieldId "$STATUS_FIELD_ID" \
    --arg optionId "$AWAITING_WORKSPACE_OPTION_ID" \
    '{"query": "mutation($projectId: ID!, $itemId: ID!, $fieldId: ID!, $optionId: String!) { updateProjectV2ItemFieldValue(input: {projectId: $projectId, itemId: $itemId, fieldId: $fieldId, value: {singleSelectOptionId: $optionId}}) { projectV2Item { id } } }", "variables": {"projectId": $projectId, "itemId": $itemId, "fieldId": $fieldId, "optionId": $optionId}}')
  STATUS_RESPONSE=$(curl -s -X POST \
    -H "Authorization: bearer $GH_TOKEN" \
    -H "Content-Type: application/json" \
    https://api.github.com/graphql \
    -d "$STATUS_PAYLOAD")
  echo "$STATUS_RESPONSE"

  if ! echo "$STATUS_RESPONSE" | jq '.' > /dev/null 2>&1; then
    echo "Non-JSON response received for status update"
    exit 1
  fi

  if echo "$STATUS_RESPONSE" | jq -e '.errors' > /dev/null; then
    echo "GraphQL error occurred while setting status"
    exit 1
  fi
fi
