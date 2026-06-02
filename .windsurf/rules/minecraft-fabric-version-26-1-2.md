---
trigger: always_on
---

# Fabric Kotlin Mod Workspace Rules

## Project Configuration
- **Minecraft Version**: `26.1.2`
- **Fabric Loader**: `0.19.2`
- **Fabric Loom**: `1.16-SNAPSHOT`
- **Fabric API**: `0.150.0+26.1.2`
- **Kotlin**: `2.3.21`
- **Fabric Language Kotlin**: `1.13.11+kotlin.2.3.21`
- **Java Target**: `25`
- **JVM Target**: `JVM_25`

## Build System
- Use **Kotlin DSL** for all Gradle scripts (`build.gradle.kts`, `settings.gradle.kts`, `gradle.properties`).
- Declare versions in `gradle.properties` and reference them via `providers.gradleProperty("...").get()` in `build.gradle.kts`.
- Do not hardcode dependency versions in `build.gradle.kts`.

## Source Sets & Entrypoints
- The project uses Loom's `splitEnvironmentSourceSets()`:
  - `src/main/kotlin` — server/common code.
  - `src/client/kotlin` — client-side code only.
- Always annotate environment-specific entrypoints:
  - Common/server entrypoint implements `net.fabricmc.api.ModInitializer`
  - Client entrypoint implements `net.fabricmc.api.ClientModInitializer`
  - Data generator entrypoint implements `net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint`
- Register all entrypoints in `src/main/resources/fabric.mod.json` under `entrypoints`.

## Kotlin & Java Interop
- Write all mod code in **Kotlin**.
- Use Kotlin `object` for singleton entrypoint classes.
- Use `org.slf4j.LoggerFactory` for logging: `LoggerFactory.getLogger("mod-id")`.
- When accessing Java APIs from Kotlin, use idiomatic Kotlin patterns (e.g., `apply { }`, `with`, null-safety).
- Keep Java compatibility settings in sync:
  - `tasks.withType<JavaCompile>().configureEach { options.release = 25 }`
  - `kotlin { compilerOptions { jvmTarget = JvmTarget.JVM_25 } }`
  - `java { sourceCompatibility = JavaVersion.VERSION_25; targetCompatibility = JavaVersion.VERSION_25 }`

## Fabric API Patterns
- Use `net.fabricmc.fabric.api.*` for hooks, events, and helpers (registries, networking, client rendering, datagen, etc.).
- Use `fabric-language-kotlin` for Kotlin-friendly DSLs and coroutine support if needed.
- Prefer Fabric's `FabricDataGenerator` for data generation via the `fabricApi { configureDataGeneration { client = true } }` block.

## Version & Resource Processing
- The build injects `version` into `fabric.mod.json` via `tasks.processResources` with `expand("version" to version)`.
- Ensure `fabric.mod.json` contains `"version": "${version}"` for this substitution to work.
- The JAR includes the `LICENSE` file renamed to `LICENSE_<projectName>`.

## Mod Metadata
- **Mod ID**: `mc-modvalidator-fabric`
- **Maven Group**: `mcmodvalidator.pro`
- **Root Package**: `mcmodvalidator.pro` / `mcmodvalidator.pro.client`
- Ensure `fabric.mod.json` IDs, `settings.gradle.kts` `rootProject.name`, and Loom `mods { register(...) }` block names are consistent.

## Minecraft 26.1 Unobfuscation
- Minecraft `26.1.x` ships **unobfuscated** — there are no obfuscated class/field/method names to remap.
- When porting or updating code for this version, **do not perform complex name remapping** (e.g., MCP/SRG name translation, access-widener hacks for hidden names, or reflection-based name resolution).
- Instead, focus migration efforts on:
  1. **Type Replacement** — update class references to match the new unobfuscated codebase (e.g., `net.minecraft.class_123` → `net.minecraft.world.level.block.Block`).
  2. **Refactoring** — move logic away from Java-based event hooks toward **Data-driven systems** where Mojang/Fabric now provides declarative alternatives (e.g., JSON data definitions, tags, recipe/datapack systems, or Fabric's datagen APIs).
- Prefer direct imports and fully-qualified class names; de-obfuscation mapping utilities or intermediary jars are unnecessary for this version.

## General Guidelines
- Do not add repositories to `repositories` unless depending on third-party mods; Loom adds Minecraft essentials automatically.
- Avoid using `cd` in terminal commands; use `cwd` parameter instead when running Gradle tasks.
- Common Gradle tasks: `gradlew build`, `gradlew runClient`, `gradlew runDatagen`.
