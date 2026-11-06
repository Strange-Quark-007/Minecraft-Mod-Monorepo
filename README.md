# Minecraft Mod Monorepo Template

A Gradle project template for developing and maintaining **multiple Minecraft mods** across **multiple modloaders** (Fabric & NeoForge) inside a single monorepo, using source-shared common projects without third-party frameworks or shadow jars.

> **Gradle purists will hate this:**
> 
> Purists advocate for strictly declarative builds where every submodule maintains its own isolated `build.gradle`. In a multi-mod monorepo, that means maintaining dozens of identical boilerplate build scripts that quickly drift out of sync.
> 
> Instead, this repo uses **centralized dynamic convention injection**:
> * **The Platform (`gradle/platform.gradle`):**
>   * Build conventions, Loom, ModDev, toolchains, and packaging are dynamically injected across all enabled mods.
>   * Individual mod folders don't need a single `build.gradle` — just pure source code and a `gradle.properties`.
> * **The Publishing (`gradle/publishing.gradle`):**
>   * Publishing is configured dynamically from mod metadata instead of duplicated per subproject.
>   * Release notes (`release-notes.md`) stay local and gitignored.
>   * You get one-click multi-platform releases — with **zero merge conflicts** across parallel Minecraft version branches (`26.1`, `26.2`, `26.3`).

## Table of Contents

- [Getting Started](#getting-started)
- [Monorepo Structure](#monorepo-structure)
- [Adding and Removing Mods](#adding-and-removing-mods)
  - [Adding a Mod](#adding-a-mod)
  - [Purging Demo Mods](#purging-demo-mods)
  - [Disabling a Mod](#disabling-a-mod)
- [Development Guide](#development-guide)
- [Shared ModDev Artifact Store & Deduplication](#shared-moddev-artifact-store--deduplication)
- [Adding Dependencies](#adding-dependencies)
- [Common Gradle Tasks](#common-gradle-tasks)
- [Publishing](#publishing)
- [License](#license)

## Getting Started

### IntelliJ IDEA

1. Clone or download this repository.
2. In `settings.gradle`, set `rootProject.name` to match your repository folder name if you rename it.
3. Open the repository root folder in IntelliJ IDEA.
4. Ensure your Gradle JVM and Project SDK match the `java_version` configured in `gradle.properties` (e.g. Java 25):
   - **Gradle JVM:** `Settings > Build, Execution, Deployment > Build Tools > Gradle`
   - **Project SDK:** `Project Structure > Project > SDK`
5. Click **Reload All Gradle Projects** in the Gradle tool window.
6. Under your Run/Debug Configurations dropdown at the top toolbar, you will see client configurations for each enabled mod (e.g., `Mod A Fabric Client`, `Mod A NeoForge Client`). Select one and run it.

## Monorepo Structure

Mods live under the `mods/` directory. Each mod has its own `common`, `fabric`, and `neoforge` subprojects alongside a local `gradle.properties` file:

```
mods/
├── mod-a/
│   ├── common/
│   ├── fabric/
│   ├── neoforge/
│   └── gradle.properties
└── mod-b/
    ├── common/
    ├── fabric/
    ├── neoforge/
    └── gradle.properties
```

Build conventions for all mods and loaders are managed centrally in `gradle/platform.gradle`. Individual mod folders do not require `build.gradle` files.

## Adding and Removing Mods

### Adding a Mod

Run `createMod` with your mod name:

```bash
# Basic usage (inherits root properties)
./gradlew createMod -Pname=my-new-mod

# With a custom group / namespace
./gradlew createMod -Pname=my-new-mod -Pgroup=dev.strangequark.mymod

# Interactive prompt (prompts in terminal for name and package)
./gradlew createMod
```

**Defaults:** If flags are not provided, values are taken directly from root `gradle.properties`:
* `group`: `<root_group>.<mod_name>` (e.g. `com.example.mynewmod`)
* `mod_author`, `license`, and `version`: inherited directly from the root properties file.

This extracts `gradle/template.zip` into `mods/<name>/`, renames packages, classes, mixin configs, service loaders, and mod metadata (`fabric.mod.json`, `neoforge.mods.toml`), and adds the mod to `enabledMods` in `settings.gradle`.

### Purging Demo Mods

To remove the starter demo mods (`mod-a` and `mod-b`):

```bash
./gradlew purgeTemplates
```

This removes `mods/mod-a/` and `mods/mod-b/` and unregisters them from `settings.gradle`.

### Disabling a Mod

Remove or comment out the mod name from `enabledMods` in `settings.gradle`. The mod directory can remain on disk but is ignored by Gradle:

```groovy
def enabledMods = [
    // 'my-new-mod'
]
```


## Development Guide

* **Common Subproject:** Develop the majority of your mod in each mod's `common` project. It compiles against vanilla Minecraft (via NeoForm) and contains shared logic and Mixins. Common code cannot access loader-specific APIs.
* **Loader Subprojects:** The `fabric` and `neoforge` projects load the common code and define platform-specific implementations. They have access to all common code, but common code cannot directly access loader code.
* **Platform Abstraction:** To call platform-specific logic from common code, use the included `IPlatformHelper` interface via Java's `ServiceLoader` (`Services.PLATFORM`).

## Shared ModDev Artifact Store & Deduplication

NeoForge's ModDevGradle (`net.neoforged.moddev`) decompiles and patches Minecraft into local jars inside each mod (`mods/<mod>/common/build/moddev/artifacts/` and `mods/<mod>/neoforge/build/moddev/artifacts/`). Across multiple mods, this duplicates ~200 MB of identical jars (`vanilla-*.jar`, `minecraft-patched-*.jar`) per mod.

This monorepo automatically eliminates disk bloat using filesystem **hardlinks** via `gradle/moddev-cache.gradle`:

* **Single Physical Copy on Disk:** Jars across all mods share identical disk blocks via NTFS/ext4/APFS hardlinks pointing to a shared store in `.gradle/moddev-artifacts/`. Each mod maintains a normal-looking file in its `build/` directory without altering task or IDE behavior.
* **Access Transformer (AT) Isolation:** Cache keys incorporate a SHA-256 hash of all active `accesstransformer.cfg` files. Mods using differing ATs are cached independently and will never collide.
* **Instant Rebuilds After Clean:** Because `.gradle/moddev-artifacts/` persists across `./gradlew clean`, running a clean build on any mod instantly restores hardlinks from the shared store instead of re-running lengthy decompilation.
* **Parallel & Concurrency Safe:** Supports `org.gradle.parallel=true` and multi-process/CI runs through atomic staging (`.moddev-<uuid>.tmp` + atomic rename) with Windows retry handling.
* **Graceful Fallback:** If run on filesystems that do not support hardlinks (e.g. FAT32, exFAT, network shares), it automatically falls back to standard copies.

### Cache Management Tasks

* **Purge the shared store:**
  ```bash
  ./gradlew cleanModDevCache
  ```
  Deletes the shared artifact store when you want a fresh decompile from upstream.

### Configuration (`gradle.properties`)

```properties
# Shared store location (relative to root or absolute; must be on the same volume/drive)
moddev_artifact_store=.gradle/moddev-artifacts

# Increment to bust and invalidate all cached artifact sets repo-wide
moddev_cache_schema=1
```

## Adding Dependencies

### 1. Simple Java Dependencies (via `gradle.properties`)
For standard libraries (e.g. Gson, JOML, YAML, math utilities), add comma-separated Maven coordinates directly in that mod's `mods/<mod>/gradle.properties`:

```properties
# Shared by common, fabric, and neoforge
common_deps=org.joml:joml:1.10.8

# Loader-specific dependencies
fabric_deps=net.fabricmc.fabric-api:fabric-api-deprecated:0.1.0
neoforge_deps=org.yaml:snakeyaml:2.2
```

### 2. Mod Dependencies & Custom Repositories (via `build.gradle`)
For dependencies requiring custom Maven repositories or Fabric Loom's `modImplementation` (which remaps mod jars at dev time), create a standard `build.gradle` in that platform subproject (e.g., `mods/<mod>/fabric/build.gradle`):

```groovy
repositories {
    maven { url = "https://maven.shedaniel.me/" }
}

dependencies {
    modImplementation "me.shedaniel.cloth:cloth-config-fabric:15.0.127"
}
```

## Common Gradle Tasks

* `./gradlew createMod -Pname=<name>` - Generates a new mod from `gradle/template.zip` and registers it in `settings.gradle`.
* `./gradlew purgeTemplates` - Deletes demo starter mods (`mod-a`, `mod-b`) and unregisters them from `settings.gradle`.
* `./gradlew build` - Compiles all active mods and collects their final jars into `build/libs/<mod>/`.
* `./gradlew clean` - Deletes build directories across all mods and the root project.
* `./gradlew cleanModDevCache` - Deletes the shared ModDevGradle artifact store.
* `./gradlew :<mod>:fabric:runClient` - Runs the Fabric client for a specific mod.
* `./gradlew :<mod>:neoforge:runClient` - Runs the NeoForge client for a specific mod.
* `./gradlew :<mod>:fabric:build` - Builds only the Fabric jar for a single mod.
* `./gradlew :<mod>:publishMods` - Builds and publishes both Fabric and NeoForge releases for a specific mod.

All built mod jars follow the `<mod_id>-<mod_version>+<mc_version>-<loader>.jar` naming standard and are surfaced into organized folders in `build/libs/<mod-name>/` (e.g. `build/libs/mod-a/`).

## Publishing

Publishing to Modrinth and CurseForge uses [`me.modmuss50.mod-publish-plugin`](https://modmuss50.github.io/mod-publish-plugin/).

### Setup

1. Add your API tokens to `~/.gradle/gradle.properties`:
   ```properties
   MODRINTH_TOKEN=mrp_...
   CURSEFORGE_TOKEN=...
   ```

2. Add your project IDs in `mods/<mod>/gradle.properties`:
   ```properties
   modrinth_id=my-mod
   curseforge_id=123456
   # release_type=release              # release (default), beta, or alpha
   # environment=both                  # see supported values below
   # publish_minecraft_versions=       # optional: override target versions (e.g. 26.3, 26.3.1)
   # publish_requires=                 # optional: required dependencies across platforms (e.g. cloth-config)
   # fabric_publish_optional=          # optional: Fabric-only dependencies (e.g. modmenu)
   # neoforge_publish_optional=        # optional: NeoForge-only dependencies
   ```

Supported `environment` values:
* `both` *(default)* - Required on both client & server.
* `client` / `client_only` - Client only.
* `server` / `server_only` - Server only.
* `dedicated_server_only` - Dedicated servers only.
* `client_or_server` - Works on client alone, server alone, or both.
* `client_or_server_prefers_both` - Works on either side, but provides extra features when on both.
* `singleplayer_only` - Singleplayer only.

### Releasing a Mod

1. Put the notes for **this specific release** in `mods/<mod>/release-notes.md` (this file is gitignored and meant only for the current update, not a full historical changelog).
2. Run the publish task:
   ```bash
   ./gradlew :<mod>:publishMods
   ```
   Or double-click `publishMods` under `mods/<mod>/Tasks/publishing/` in IntelliJ.

*(Tip: You can test the release without uploading by passing `-PdryRun=true`)*

## License

The template infrastructure and build scripts are licensed under the [MIT License](LICENSE). 

Each mod inside `mods/` can declare its own license in its `gradle.properties` file and provide a corresponding `LICENSE` file in its mod folder (`mods/<mod>/LICENSE`).
