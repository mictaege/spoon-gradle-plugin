# AGENTS.md — spoon-gradle-plugin

> **End of life:** This repository is no longer maintained. Since jitter 2026.3 strips bytecode
> instead of transforming sources with Spoon, `jitter-plugin` no longer depends on this plugin.

## Project purpose
`spoon-gradle-plugin` is a Gradle plugin wrapping the Java analysis and transformation framework
[Spoon](http://spoon.gforge.inria.fr/index.html). Unlike the official SpoonLabs Gradle plugin (which
only processes production sources and targets Android), this plugin processes **both production and
test** Java source sets and has no Android support — it's a general-purpose Spoon integration for
plain Gradle Java projects.

Part of the jitter/spoon family: it was a low-level building block consumed by
[`jitter-plugin`](https://github.com/mictaege/jitter-plugin), which used Spoon-based source
transformation to implement application "flavours" (until jitter 2026.3 switched to bytecode
stripping — see the EOL note above).

## Technical stack
- Language: Kotlin (plugin sources under `src/main/kotlin`), implementation class
  `com.github.mictaege.spoon_gradle_plugin.SpoonPlugin`.
- Build tool: Gradle (Kotlin DSL, `build.gradle.kts`) — build via `./gradlew`.
- Plugin packaging: `java-gradle-plugin` + `com.gradle.plugin-publish`, published to the Gradle
  Plugin Portal as `io.github.mictaege.spoon-gradle-plugin`.
- Key dependency: Spoon core (`fr.inria.gforge.spoon:spoon-core`, currently `11.2.0`), with the
  bundled `org.eclipse.jdt.core` excluded; also uses `javaparser-symbol-solver-core`.
- Release/publishing: `org.jreleaser`, publishing to Maven Central under group `io.github.mictaege`.
- License: Apache License 2.0.

## Project semantics / domain notes
- The plugin is expected to work against arbitrary Gradle-defined Java source sets (production,
  test, and custom ones like the `beans`/`ui` source sets seen in `eval.jitter`), so avoid
  hardcoding assumptions about a fixed `main`/`test` layout.
- A generated properties file (`spoon-gradle-plugin.properties`, written during the build) exposes
  the plugin's own version at runtime — keep this generation step working when touching the build.
- Treat the Spoon dependency version as a stack-relevant detail primarily in terms of "Spoon is the
  underlying transformation engine used here" rather than the exact pinned version.

## Working conventions
- Run `./gradlew test` before submitting changes.
- Since `jitter-plugin` depends on this plugin directly, keep its public Gradle extension/task API
  backward compatible, or coordinate version bumps across both projects.
