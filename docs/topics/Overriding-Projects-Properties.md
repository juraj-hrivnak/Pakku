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

> Added in version **1.4.0**

The `side` property affects how projects are exported to client modpacks, controlled by the [`export_server_side_projects_to_client`](Config-File.md#export_server_side_projects_to_client) configuration:

**When `export_server_side_projects_to_client = false` (default):**
- **CurseForge**: `SERVER`-side projects are excluded from the manifest
- **Modrinth**: `SERVER`-side projects are included with `env.client = "unsupported"`

**When `export_server_side_projects_to_client = true`:**
- All projects are included regardless of side (backward compatible behavior)

> **Note:** CurseForge lacks environment field support, so server-side mods must be excluded entirely. Modrinth can include them with proper `env` fields to prevent client loading.
