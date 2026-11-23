# CLAUDE.md

SpringFX binds Spring Boot and JavaFX together. It is a library meant to be consumed by other projects (first: `cronometer-export-helper`) and eventually published as a Spring Boot starter, so keep it free of any application-specific code.

## Workflow rules

- **Never commit directly to `main`.** Do all work on a feature branch and merge it into `main` through a GitHub Pull Request.
- **Merge PRs with the rebase strategy** (`gh pr merge --rebase`). Never squash or create merge commits.
- **Releases are git tags** (e.g. `v0.1.0`); JitPack builds consumers' artifacts from them. See `docs/adr/0001-separate-library-published-via-jitpack.md`.

## Architecture rules

- **One top-level Java class per file.** Nest a class only when there's a specific reason, such as nested properties inside a Spring `@ConfigurationProperties` class.
- **All configurable properties go through Spring Boot `@ConfigurationProperties`**, with sensible defaults so an app works with no configuration. Don't use `@Value` or read `Environment` directly.
- **Option and value classes get a Lombok builder** (`@Builder(toBuilder = true)`), not hand-written `with*` methods or telescoping constructors. Change existing values with `toBuilder()`. Lombok is compile-only (`provided`, optional), so consumers never get it.
- **Never start the JavaFX toolkit outside `SpringFXApplication.launch`.** Beans must be creatable in a plain `@SpringBootTest` without a display.
- **Public API is deliberate.** Anything public in `com.brendondugan.springfx` is a promise to consumers; keep internals package-private or in an `internal` package.

## Agent skills

### Issue tracker

Issues are tracked in GitHub Issues for `brennydoogles/springfx`, via the `gh` CLI. See `docs/agents/issue-tracker.md`.

### Triage labels

Default vocabulary: `needs-triage`, `needs-info`, `ready-for-agent`, `ready-for-human`, `wontfix`. See `docs/agents/triage-labels.md`.

### Domain docs

Single-context: one `GLOSSARY.md` and `docs/adr/` at the repo root. See `docs/agents/domain.md`.
