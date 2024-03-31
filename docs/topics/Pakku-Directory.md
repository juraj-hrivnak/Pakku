# Pakku Directory

Pakku directory (`.pakku`) can be currently used to define additional [project overrides].
Pakku directory must be created in your modpack folder
starting with a dot in the name: `.pakku`.

## Adding Project Overrides

<procedure>

To add a [project override]:

<step>

Create an `overrides`, `server-overrides` or `client-overrides` subdirectory
in the pakku directory.

```
.pakku/
┣━━━ overrides/
┣━━━ server-overrides/
┗━━━ client-overrides/
```

</step>

<step>

Create subdirectory for each project type you want to override.

```
.pakku/
┣━━━ overrides/
┃    ┗━━━ mods/
┗━━━ client-overrides/
     ┣━━━ resourcepacks/
     ┗━━━ shaderpacks/
```

</step>

<step>

Add project overrides.

```
.pakku/
┣━━━ overrides/
┃    ┗━━━ mods/<put mod files here>
┗━━━ client-overrides/
     ┣━━━ resourcepacks/<put resourcepack files here>
     ┗━━━ shaderpacks/<put shader files here>
```

These project overrides will br synchronized with your modpack folder
when running [pakku fetch](pakku-fetch.md), and exported properly
when running [pakku export](pakku-export.md).

</step>

</procedure>

[project override]: Pakku-Terminology.md#project-override
[project overrides]: Pakku-Terminology.md#project-override
