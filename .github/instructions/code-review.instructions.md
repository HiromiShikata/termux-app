---
applyTo: '**'
---

<!--
DO NOT EDIT DIRECTLY.
This file is auto-synchronized from HiromiShikata/repositories-management.
Direct edits in downstream repositories will be overwritten by the next sync.
Update the source file in HiromiShikata/repositories-management instead.
-->

# Code Review Instructions

## Scope Integrity

Flag pull requests that contain changes unrelated to the stated PR title or description.
Each PR must address a single topic or feature.
If unrelated modifications are detected, request that they be split into separate issues first.

## Lint and Static Analysis Bypass Detection

Flag any code that disables, ignores, or bypasses linting rules.
Report usage of eslint-disable, @ts-ignore, @ts-expect-error, noqa, or similar suppression comments.
Flag modifications to linter configuration files that weaken existing rules.

## Error Handling Violations

Flag code that catches exceptions but does nothing with them (empty catch blocks).
Flag API endpoints that return HTTP 200 status codes when errors have occurred.
Flag code that converts errors into success responses or hides error states.
Flag use of console.log for error handling instead of proper error propagation.
Flag code that swallows errors by returning default values without logging or reporting.

## Test Coverage Requirements

Flag new features or bug fixes that lack corresponding unit tests.
Flag modifications to existing code where related tests have not been updated.
Flag deletion or disabling of existing tests without justification.
Flag test files that use skip, only, or other mechanisms to bypass test execution.
Flag mock implementations that always return success without testing failure scenarios.
Report when test coverage for modified files appears insufficient.
Report when test assertion for modified files appears insufficient.

## Code Quality Red Flags

Flag hardcoded credentials, API keys, or secrets.
Flag commented-out code blocks.
Flag TODO or FIXME comments without associated issue references.
Flag any use of type assertions (as any, as unknown) that bypass type safety.
Flag functions with excessive cyclomatic complexity.
