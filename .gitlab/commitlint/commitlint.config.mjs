// Rules of the "MR title lint" CI job (bin/lint-mr-title.sh), #762.
//
// The title of a merge request becomes the commit message on develop (squash), so it follows
// Conventional Commits: type(scope): description, with the issue reference at the end of the
// description, e.g. "feat(awe-model): ECharts option model for charts (#795 T1a)".
//
// Based on @commitlint/config-conventional, adjusted to how AWE writes its titles:
//   - types: the ones in use plus build, perf, style and revert;
//   - scope: optional and an open list (module names, "deps", "ci", several separated by a comma);
//   - description: any case, no full stop at the end; trailing markers such as "(#795 T1a)",
//     "(develop)" or "[support/4.x]" are part of the description, so Renovate titles pass as they are;
//   - header: up to 150 characters (the longest title of the last 100 merged requests has 134).
export default {
  extends: ['@commitlint/config-conventional'],
  rules: {
    'type-enum': [2, 'always', ['feat', 'fix', 'chore', 'ci', 'docs', 'test', 'refactor', 'build', 'perf', 'style', 'revert']],
    'subject-case': [0],
    'header-max-length': [2, 'always', 150]
  }
}
