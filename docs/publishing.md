# Publishing

[Back to README](../README.md)

Publishing to Modrinth and CurseForge uses [`me.modmuss50.mod-publish-plugin`](https://modmuss50.github.io/mod-publish-plugin/).

## Setup

1. Add your API tokens to `~/.gradle/gradle.properties` (or set the `MODRINTH_TOKEN` / `CURSEFORGE_TOKEN` environment variables):

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

* `both` *(default)* - Required on both client and server.
* `client` / `client_only` - Client only.
* `server` / `server_only` - Server only.
* `dedicated_server_only` - Dedicated servers only.
* `client_only_server_optional` - Client required, server optional.
* `server_only_client_optional` - Server required, client optional.
* `client_or_server` - Works on client alone, server alone, or both.
* `client_or_server_prefers_both` - Works on either side, but provides extra features when on both.
* `singleplayer_only` - Singleplayer only.

## Releasing a Mod

1. Put the notes for **this specific release** in `mods/<mod>/release-notes.md`. This file is gitignored and meant only for the current update, not a full historical changelog.
2. Run the publish task:

   ```bash
   ./gradlew :<mod>:publishMods
   ```

   Or double-click `publishMods` under `mods/<mod>/Tasks/publishing/` in IntelliJ.

The task publishes every loader that exists for the mod. Fabric-only and NeoForge-only mods therefore publish only their configured platform.

*(Tip: You can test the release without uploading by passing `-PdryRun=true`.)*
