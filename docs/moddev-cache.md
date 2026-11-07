# ModDev Artifact Cache

[Back to README](../README.md)

NeoForge's ModDevGradle (`net.neoforged.moddev`) decompiles and patches Minecraft into local jars inside each mod (`mods/<mod>/common/build/moddev/artifacts/` and `mods/<mod>/neoforge/build/moddev/artifacts/`). Across multiple mods, this duplicates roughly 200 MB of identical jars (`vanilla-*.jar`, `minecraft-patched-*.jar`) per mod.

This monorepo eliminates that disk bloat using filesystem **hardlinks** via `gradle/moddev-cache.gradle`:

* **Single physical copy on disk:** Jars across all mods share identical disk blocks through NTFS, ext4, or APFS hardlinks pointing to a shared store in `.gradle/moddev-artifacts/`. Each mod keeps a normal-looking file in its `build/` directory without changing task or IDE behavior.
* **Access Transformer (AT) isolation:** Cache keys include a SHA-256 hash of all active `accesstransformer.cfg` files. Mods using different ATs are cached independently and never collide.
* **Instant rebuilds after clean:** Because `.gradle/moddev-artifacts/` persists across `./gradlew clean`, a clean build on any mod restores hardlinks from the shared store instead of repeating decompilation.
* **Parallel and concurrency safe:** Supports `org.gradle.parallel=true` and multi-process or CI runs through atomic staging (`.moddev-<uuid>.tmp` plus atomic rename) with Windows retry handling.
* **Graceful fallback:** On filesystems without hardlink support (FAT32, exFAT, network shares), it automatically falls back to standard copies.

## Cache Management

Purge the shared store when you want a fresh decompile from upstream:

```bash
./gradlew cleanModDevCache
```

## Configuration

```properties
# Shared store location (relative to root or absolute; must be on the same volume/drive)
moddev_artifact_store=.gradle/moddev-artifacts

# Increment to bust and invalidate all cached artifact sets repo-wide
moddev_cache_schema=1
```
