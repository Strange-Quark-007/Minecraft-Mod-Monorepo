# Minecraft Mod Monorepo Template

A Gradle project template for developing and maintaining **multiple Minecraft mods** across **multiple mod loaders** (Fabric & NeoForge) inside one monorepo, using source-shared common projects without third-party frameworks or shadow jars.

## The Anti-Framework Approach

**Gradle purists will hate this:**

Purists advocate for strictly declarative builds where every submodule maintains its own isolated `build.gradle`.

In a multi-mod monorepo, that means maintaining dozens of identical boilerplate build scripts that quickly drift out of sync.

Instead, this repo uses **centralized dynamic convention injection**:
* **The Platform (`gradle/platform.gradle`):**
  * Build conventions, Loom, ModDev, toolchains, and packaging are dynamically injected across all enabled mods.
  * Individual mod folders don't need a single `build.gradle` — just pure source code and a `gradle.properties`.
* **The Publishing (`gradle/publishing.gradle`):**
  * Publishing is configured dynamically from mod metadata instead of duplicated per subproject.
  * Release notes (`release-notes.md`) stay local and gitignored.
  * You get one-click multi-platform releases — with **zero merge conflicts** across parallel Minecraft version branches (`26.1`, `26.2`, `26.3`).

### What This Solves

1. **Eliminates the "Gradle Tax":** Normally, 5 mods × 3 subprojects = 15 `build.gradle` files that inevitably drift out of sync. Here, you maintain **zero** build files per mod. Build conventions live in `gradle/`.
2. **Solves the ModDevGradle storage bloat:** ModDevGradle decompiles ~200 MB of Minecraft jars per mod. A dedicated hardlink script deduplicates identical jars into a shared `.gradle/` store, so those copies share one physical copy on disk and persist across `./gradlew clean`.
3. **Eliminates version hell:** Parallel Minecraft versions live on Git branches (`26.1`, `26.2`, `26.3`) with properties in a single root `gradle.properties`. When a new Minecraft update drops, update the root version properties once and every inheriting mod follows.
4. **Zero-ceremony publishing:** One task (`./gradlew :<mod>:publishMods`) publishes to the configured Modrinth and CurseForge projects, resolving dependencies, client/server environments, and local release notes.
5. **Deliberately un-enterprise:** No custom `build-logic` plugin, no self-hosted Maven plugin repository, no shadow jars. Each adds overhead: manually applying plugins in every submodule, plugin publishing/versioning and release infrastructure, jar bundling/classloading. Raw Groovy, `apply from:`, and filesystem primitives stay readable and easy to debug. Architectury is not bundled; add it later if you need heavier cross-loader abstraction.

**The Bottom Line:** It trades enterprise abstraction for readable, editable scripts. It is the anti-framework for anyone maintaining a portfolio of Minecraft mods.

## Quick Start

```bash
# Create a mod from gradle/template.zip
./gradlew createMod -Pname=my-new-mod

# Run it
./gradlew :my-new-mod:fabric:runClient
./gradlew :my-new-mod:neoforge:runClient
```

Remove the starter demo mods (`mod-a`, `mod-b`) with:

```bash
./gradlew purgeTemplates
```

## Real-World Example

See this template in action in [StrangeQuark-Mods](https://github.com/Strange-Quark-007/StrangeQuark-Mods) — a collection of lightweight, vanilla-friendly Minecraft mods for Fabric and NeoForge.

## Documentation

* **[Development Guide](docs/development-guide.md):** IntelliJ setup, project structure, creating and managing mods, single-loader support, development workflow, dependencies, and common Gradle tasks.
* **[Publishing](docs/publishing.md):** Modrinth and CurseForge tokens, project IDs, environment flags, and the release flow.
* **[ModDev Artifact Cache](docs/moddev-cache.md):** Shared ModDevGradle artifact store, disk deduplication, and cache management.

## License

The template infrastructure and build scripts are licensed under the [MIT License](LICENSE).

Each mod inside `mods/` can declare its own license in its `gradle.properties` file and provide a corresponding `LICENSE` file in its mod folder (`mods/<mod>/LICENSE`).
