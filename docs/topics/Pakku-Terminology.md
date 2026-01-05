# Pakku Terminology

Since Pakku deals with many names for different things from various sources,
it is crucial to clarify what means what.

## Project

A 'project' describes a mod, resource pack, shader, etc.

## Project File

A 'project file' describes the file associated with a project.

## Project Side

A 'project side' describes where a project can run:

- **CLIENT**: Runs only on the client (e.g., JEI, shader mods, client HUD enhancements)
- **SERVER**: Runs only on the server (e.g., server management tools, server-side optimization)
- **BOTH**: Runs on both client and server (e.g., most content mods, API libraries)

The side property affects how projects are exported to different platforms. See [Exporting a Modpack](Exporting-a-Modpack.md#server-side-mod-handling) for details.

## Override

An 'override' describes a file or directory that will be packaged with the modpack when exporting.

