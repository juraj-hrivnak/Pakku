# Overriding Project&#39;s Properties

If a project has a property that is not correct
(marked with a wrong side or as not redistributable,
even though you have explicit permission to redistribute it)
you can override it using the [config file](Config-File.md).

<procedure title="Changing project side to `CLIENT`">
<step>

Open the [config file](Config-File.md) (`pakku.json`)
and find a property named [`projects`](Config-File.md#projects).

</step>
<step>

Use the project slug, name, ID or filename to create a new property in it:

```JSON
{
    "projects": {
        "jei": {
        }
    }
}
```

</step>
<step>

And finally, add the property with the value you want to override.

In our example, we will override JEI's project side to `CLIENT`:
```JSON
{
    "projects": {
        "jei": {
            "side": "CLIENT"
        }
    }
}
```

</step>
</procedure>

## Understanding Project Sides

The `side` property determines where a project (mod, resource pack, etc.) can run:

- **`CLIENT`**: Only on the client (e.g., JEI, shader mods, client-side HUD mods)
- **`SERVER`**: Only on the server (e.g., server management tools, server-side optimization mods)
- **`BOTH`**: On both client and server (e.g., most content mods, Fabric API)
- **`null`** (unspecified): Treated as `BOTH` by default

### Impact on Exporting

> Added in version **1.3.3**

The `side` property, combined with the [`export_server_side_projects_to_client`](Config-File.md#export_server_side_projects_to_client) configuration, controls how projects are exported:

**For CurseForge exports:**
- When `export_server_side_projects_to_client = false`: `SERVER`-side projects are excluded from the manifest
- When `export_server_side_projects_to_client = true`: All projects are included

**For Modrinth exports:**
- All projects are always included
- `SERVER`-side projects: Set `env.client = "unsupported"` when `export_server_side_projects_to_client = false`, or `env.client = "required"` when `true`
- `CLIENT`-side projects: Always set `env.client = "required", env.server = "unsupported"`
- `BOTH`/unspecified projects: Always set `env.client = "required", env.server = "required"`

> See [Exporting a Modpack](Exporting-a-Modpack.md#server-side-mod-handling) for more details on export behavior.
