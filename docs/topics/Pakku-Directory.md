# Pakku Directory

Pakku directory (`.pakku`) can be used to define [project overrides].

Pakku directory must be created in your modpack folder
starting with a dot in the name: `.pakku`

To add [project overrides], create an `overrides` subdirectory,
in which you can create subdirectories for each project type.

## Example

`ExampleMod.jar` will be added to the `overrides/mods` folder in
the exported modpack, and will be copied to your modpack directory
mods folder when running [`pakku fetch`](pakku-fetch.md)

```
.pakku/
┗━━━ overrides/
     ┣━━━ mods/
     ┃    ┗━━━ ExampleMod.jar
     ┣━━━ resourcepacks/
     ┗━━━ shaderpacks/
```

[project overrides]: Pakku-Terminology.md#project-override