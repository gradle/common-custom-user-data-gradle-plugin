# Versioning

- This plugin follows [Semantic Versioning](https://semver.org/).
- The authoritative version lives in [`release/version.txt`](release/version.txt).
- **Any user-visible change must come with a version bump in the same PR.**
- Decide the correct bump by comparing against the **currently released version** — the latest [GitHub release](https://github.com/gradle/common-custom-user-data-gradle-plugin/releases) / plugin portal version — not the value currently in `release/version.txt`. That file may already be ahead of the last release because a post-release workflow pre-bumps it to the next patch.
- Choose:
  - **Patch** (e.g. `2.6.0` → `2.6.1`) — bug fixes only.
  - **Minor** (e.g. `2.6.0` → `2.7.0`) — new user-visible features, newly captured values/tags, new configuration options, or user-visible improvements.
  - **Major** (e.g. `2.6.0` → `3.0.0`) — breaking changes to public behavior or consumer configuration.
- If `release/version.txt` already reflects the required bump relative to the released version, leave it. Only raise it further if your change warrants a larger bump than what is already staged.

# Changelog

- Every user-visible change must add an entry to [`release/changes.md`](release/changes.md) in the **same PR** that introduces the change. The contents of this file become the body of the GitHub release verbatim.
- Format: a flat markdown bullet list, one line per change, each line tagged.
- Tags:
  - `[NEW]` — a new feature or newly captured data.
  - `[FIX]` — a bug fix or corrected behavior (including renames or cleanups that users will observe).
  - `[IMPROVEMENT]` — an enhancement to existing behavior that is not a bug fix and not a wholly new feature.
- Use backticks around identifiers, tag names, value keys, environment variables, and branch-like strings.
- Examples from recent releases:

  ```markdown
  - [FIX] Rename `AI Agent` tag to `AI`
  - [NEW] For GitHub PRs, capture `GITHUB_BASE_REF` as the value `PR base branch`
  - [NEW] Capture GitHub Actions run number and run attempt as custom values to precisely identify workflow runs
  ```

- After a release, the post-release workflow resets `release/changes.md` to `- [NEW] TBD`. Replace that placeholder on your first substantive change rather than appending below it.

# Versioning and changelog exceptions

The versioning and changelog rules above target user-visible changes. They do **not** apply to:

- Routine dependency updates (e.g. Renovate minor/patch bumps of build plugins, GitHub Actions, or transitive libraries) that do not alter plugin behavior for consumers. These may merge without a version bump or changelog entry.
- Anything else that is strictly internal and invisible to plugin consumers (CI configuration, repo tooling, internal docs).

When a dependency update does change consumer-visible behavior (for example, a bumped compile-time dependency that alters captured data, changes a public API, or fixes a user-facing bug), treat it as a normal change and follow the versioning and changelog rules above. Treat borderline cases as exceptions to discuss in review rather than trying to generalize them here.
