// DO NOT EDIT DIRECTLY.
// This file is auto-synchronized from HiromiShikata/repositories-management.
// Direct edits in downstream repositories will be overwritten by the next sync.
// Update the source file in HiromiShikata/repositories-management instead.

module.exports = {
  extends: ['@commitlint/config-conventional'],
  rules: {
    'type-enum': [
      2,
      'always',
      [
        'build',
        'chore',
        'ci',
        'docs',
        'feat',
        'fix',
        'perf',
        'refactor',
        'revert',
        'style',
        'test',
        'autogen',
        'prep',
        'adapt',
      ],
    ],
    'header-max-length': [0],
    'body-max-line-length': [0],
  },
};
