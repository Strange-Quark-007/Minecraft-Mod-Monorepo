# Minecraft Mod Monorepo Template

A Gradle project template for developing and maintaining **multiple Minecraft mods** across **multiple modloaders** (Fabric & NeoForge) inside a single monorepo, using source-shared common projects without third-party frameworks or shadow jars.

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

1. Open `settings.gradle` and add your mod's folder name to the `enabledMods` list:
   ```groovy
   def enabledMods = ['mod-a', 'mod-b', 'my-new-mod']
   ```
2. Create `mods/my-new-mod/` (you can copy `mods/mod-a` as a template), then rename the placeholder `com.example.moda` packages in its `common`, `fabric`, and `neoforge` sources to your own package.
3. Set your mod's identity in `mods/my-new-mod/gradle.properties`:
   ```properties
   mod_id=my_new_mod
   mod_name=My New Mod
   version=1.0.0
   group=com.example.mynewmod
   mod_author=Strange Quark
   description=A brand new mod in the monorepo.
   license=MIT
   ```
4. Reload Gradle in IntelliJ.

### Removing or Disabling a Mod

Remove or comment out the mod name from `enabledMods` in `settings.gradle`. The folder can remain on disk without being evaluated or built by Gradle.

## Development Guide

* **Common Subproject:** Develop the majority of your mod in each mod's `common` project. It compiles against vanilla Minecraft (via NeoForm) and contains shared logic and Mixins. Common code cannot access loader-specific APIs.
* **Loader Subprojects:** The `fabric` and `neoforge` projects load the common code and define platform-specific implementations. They have access to all common code, but common code cannot directly access loader code.
* **Platform Abstraction:** To call platform-specific logic from common code, use the included `IPlatformHelper` interface via Java's `ServiceLoader` (`Services.PLATFORM`).

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

* `./gradlew build` - Compiles all active mods and collects their final jars into `build/libs/<mod>/`.
* `./gradlew clean` - Deletes build directories across all mods and the root project.
* `./gradlew :<mod>:fabric:runClient` - Runs the Fabric client for a specific mod.
* `./gradlew :<mod>:neoforge:runClient` - Runs the NeoForge client for a specific mod.
* `./gradlew :<mod>:fabric:build` - Builds only the Fabric jar for a single mod.
* `./gradlew :<mod>:publishMods` - Builds and publishes both Fabric and NeoForge releases for a specific mod.

All built mod jars follow the `<mod_id>-<mod_version>+<mc_version>-<loader>.jar` naming standard and are surfaced into organized folders in `build/libs/<mod-name>/` (e.g. `build/libs/mod-a/`).

## Publishing

Publishing to Modrinth and CurseForge uses `me.modmuss50.mod-publish-plugin`.

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
   # release_type=release  # release, beta, or alpha
   ```

### Publishing

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
