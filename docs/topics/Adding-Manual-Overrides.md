# Adding Manual Overrides

<procedure>

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

These project overrides will be synchronized with your modpack folder
when running [](pakku-fetch.md), and exported properly
when running [](pakku-export.md).

</step>

</procedure>
