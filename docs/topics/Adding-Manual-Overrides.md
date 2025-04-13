# Adding Manual Overrides

Manual overrides are files that will be bundled with your modpack on exporting similarly to [overrides](Pakku-Terminology.md#override).

However, they are specified differently and serve two main functions:

1. They can be used to [add projects, which are not hosted on any platform, to your modpack](#adding_manual_project_overrides).
    > These project overrides will be synchronized with your modpack folder
when running [](pakku-fetch.md), and exported properly when running [](pakku-export.md).
2. They can be used to manually add overrides to your modpack, which are stored separately, so they don't bloat your modpack directory.

<procedure title="Adding projects, which are not hosted on any platform, to your modpack" id="adding_manual_project_overrides">

<step>

Create a directory named `overrides`, `server-overrides` or `client-overrides` in the `.pakku` directory.

```
.pakku/
┣━━━ overrides/
┣━━━ server-overrides/
┗━━━ client-overrides/
```

</step>

<step>

Create directory for each project type you want to override.

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

Add your manual project overrides.

```
.pakku/
┣━━━ overrides/
┃    ┗━━━ mods/<put mod files here>
┗━━━ client-overrides/
     ┣━━━ resourcepacks/<put resourcepack files here>
     ┗━━━ shaderpacks/<put shader files here>
```

</step>

</procedure>
