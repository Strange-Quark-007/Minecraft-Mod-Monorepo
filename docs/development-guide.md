# Development Guide

[Back to README](../README.md)

## Contents

- [IntelliJ Setup](#intellij-setup)
- [How Builds Work](#how-builds-work)
- [Monorepo Structure](#monorepo-structure)
- [Creating and Managing Mods](#creating-and-managing-mods)
- [Development Workflow](#development-workflow)
- [Dependencies](#dependencies)
- [Common Gradle Tasks](#common-gradle-tasks)

## IntelliJ Setup

1. Clone or download this repository.
2. In `settings.gradle`, set `rootProject.name` to match your repository folder name if you rename it.
3. Open the repository root folder in IntelliJ IDEA.
4. Ensure your Gradle JVM and Project SDK match the `java_version` configured in `gradle.properties` (for example, Java 25):
   - **Gradle JVM:** `Settings > Build, Execution, Deployment > Build Tools > Gradle`
   - **Project SDK:** `Project Structure > Project > SDK`
5. Click **Reload All Gradle Projects** in the Gradle tool window.
6. Under your Run/Debug Configurations dropdown at the top toolbar, you will see client configurations for each enabled mod (for example, `Mod A Fabric Client`, `Mod A NeoForge Client`). Select one and run it.

## How Builds Work

In a multi-mod monorepo, strictly declarative builds mean maintaining dozens of identical `build.gradle` files that quickly drift out of sync. This repository injects conventions dynamically instead:

* **Platform (`gradle/platform.gradle`):** Applies toolchains, Loom, ModDev, packaging, run configurations, and shared conventions across all enabled mods.
* **Publishing (`gradle/publishing.gradle`):** Configures Modrinth and CurseForge from mod metadata instead of duplicated per-subproject scripts. Release notes (`release-notes.md`) stay local and gitignored.
* **Mod projects:** Each mod folder contains source code and a `gradle.properties`. It does not need its own `build.gradle` unless a loader-specific dependency requires custom repositories or `modImplementation`.

This keeps platform, toolchain, and publishing changes centralized, including across parallel Minecraft version branches such as `26.1`, `26.2`, and `26.3`.

## Monorepo Structure

Mods live under `mods/`. Each mod has its own `common`, `fabric`, and `neoforge` subprojects alongside a local `gradle.properties` file:

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

## Creating and Managing Mods

### Adding a Mod

Run `createMod` with your mod name:

```bash
# Basic usage (inherits root properties)
./gradlew createMod -Pname=my-new-mod

# With a custom group / namespace
./gradlew createMod -Pname=my-new-mod -Pgroup=dev.strangequark.mymod
```

**Defaults:** If flags are not provided, values are taken from the root `gradle.properties`:

* `group`: `<root_group>.<mod_name>` (for example, `com.example.mynewmod`)
* `mod_author`, `license`, and `version`: inherited from the root properties file.

This extracts `gradle/template.zip` into `mods/<name>/`, renames packages, classes, mixin configs, service loaders, and mod metadata (`fabric.mod.json`, `neoforge.mods.toml`), and adds the mod to `enabledMods` in `settings.gradle`. Existing `enabledMods` formatting, comments, and line endings are preserved.

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

### Targeting a Single Loader

Every mod generated from the template includes `common`, `fabric`, and `neoforge` by default. Loader subprojects are registered dynamically, so you can drop a platform:

* **Fabric-only:** delete `mods/<mod>/neoforge/`.
* **NeoForge-only:** delete `mods/<mod>/fabric/`.

No build script or `settings.gradle` edit is required. Gradle detects the existing loader directories and limits run configurations, `build`, `collectJars`, and `publishMods` to them.

### Removing common

`common` is optional but recommended. Generated loader projects reference shared code, assets, and mixins located in `common`. If you want to delete `common`, move those files into the loader-specific folder first. Gradle wiring adapts automatically.

Loader-local access files also work when `common` is removed:

* Fabric: `mods/<mod>/fabric/src/main/resources/<mod_id>.accesswidener`
* NeoForge: `mods/<mod>/neoforge/src/main/resources/META-INF/accesstransformer.cfg`

## Development Workflow

* **Common subproject:** Develop the majority of your mod in each mod's `common` project. It compiles against vanilla Minecraft (via NeoForm) and contains shared logic and mixins. Common code cannot access loader-specific APIs.
* **Loader subprojects:** The `fabric` and `neoforge` projects load the common code and define platform-specific implementations. They have access to all common code, but common code cannot directly access loader code.
* **Platform abstraction:** To call platform-specific logic from common code, use the included `IPlatformHelper` interface via Java's `ServiceLoader` (`Services.PLATFORM`).

## Data Generation

Datagen is configured per loader. Each loader has its own run task and output directory; generated files are not shared between platforms.

* **Fabric:** `./gradlew :<mod>:fabric:runDatagen`
  * Output: `mods/<mod>/fabric/src/main/generated/`
  * Uses the `fabric-datagen` entrypoint in `fabric.mod.json`.
* **NeoForge:** `./gradlew :<mod>:neoforge:runData`
  * Output: `mods/<mod>/neoforge/src/generated/resources/`
  * Uses the `data` run configuration.

Generated data is not committed. The output directories are gitignored, and `./gradlew clean` deletes them. Each loader's `jar` task depends on its datagen task, so `./gradlew build` and `publishMods` regenerate data before packaging. Use `./gradlew generateData` to regenerate data without building. Single-loader mods only expose the task for the loader they keep; deleting a loader directory also removes its datagen configuration from the build. Provider implementations stay in each loader's source set because common code cannot reference loader datagen APIs.

## Dependencies

### Simple Java Dependencies

For standard Java libraries (such as JOML, Gson, or SnakeYAML), declare Maven coordinates in `mods/<mod>/gradle.properties`:

```properties
# Shared across common and loaders (comma-separated if multiple)
common_deps=org.joml:joml:1.10.8

# Loader-specific libraries (optional)
fabric_deps=com.squareup.okhttp3:okhttp:4.12.0
neoforge_deps=org.yaml:snakeyaml:2.2
```

### Mod Dependencies and Custom Repositories

For dependencies requiring custom Maven repositories or Fabric Loom's `modImplementation` (which remaps mod jars at development time), create a standard `build.gradle` in that platform subproject, for example `mods/<mod>/fabric/build.gradle`:

```groovy
repositories {
    maven { url = "https://maven.shedaniel.me/" }
}

dependencies {
    modImplementation "me.shedaniel.cloth:cloth-config-fabric:15.0.127"
}
```

## Common Gradle Tasks

| Command | Purpose |
| --- | --- |
| `./gradlew createMod -Pname=<name>` | Generate a new mod from `gradle/template.zip` and register it in `settings.gradle`. |
| `./gradlew purgeTemplates` | Delete demo starter mods (`mod-a`, `mod-b`) and unregister them from `settings.gradle`. |
| `./gradlew generateData` | Run datagen for every active Fabric and NeoForge loader. |
| `./gradlew build` | Compile all active mods and collect final jars into `build/libs/<mod>/`. |
| `./gradlew clean` | Delete build directories across all mods and the root project. |
| `./gradlew cleanModDevCache` | Delete the shared ModDevGradle artifact store. |
| `./gradlew :<mod>:fabric:runClient` | Run the Fabric client for a specific mod. |
| `./gradlew :<mod>:fabric:runDatagen` | Generate Fabric data into `fabric/src/main/generated/`. |
| `./gradlew :<mod>:neoforge:runClient` | Run the NeoForge client for a specific mod. |
| `./gradlew :<mod>:neoforge:runData` | Generate NeoForge data into `neoforge/src/generated/resources/`. |
| `./gradlew :<mod>:fabric:build` | Build only the Fabric jar for a single mod. |
| `./gradlew :<mod>:publishMods` | Build and publish releases for every configured loader of a specific mod. |

All built mod jars follow the `<mod_id>-<mod_version>+<mc_version>-<loader>.jar` naming standard and are surfaced into organized folders in `build/libs/<mod-name>/` (for example, `build/libs/mod-a/`).
