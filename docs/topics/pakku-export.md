# pakku export

Export modpack

## Usage

<snippet id="snippet-cmd">

<var name="cmd">export</var>
<var name="params">[&lt;options&gt;] </var>
<include from="_template_cmd.md" element-id="template-cmd"/>

</snippet>

## Description

Exports your modpack in various formats based on your target platform. The export behavior for server-side mods can be controlled using the [`export_server_side_projects_to_client`](Config-File.md#export_server_side_projects_to_client) configuration option.

**Note:** The `export_server_side_projects_to_client` option only affects **mod projects** in the manifest. Override files (like `server-overrides/`) are always exported unless the `--client-only` flag is used.

See [Exporting a Modpack](Exporting-a-Modpack.md) for detailed information about export formats and [Server-Side Mod Handling](Exporting-a-Modpack.md#server-side-mod-handling) for how different platforms handle server-side mods.

## Options

<snippet id="snippet-options-all">

<snippet id="snippet-options">

`--client-only`
: Export client-only modpack. When enabled:

  - **CurseForge**: No effect (uses standard export behavior)
  - **Modrinth**: Excludes server-side mods and `server-overrides/` directory
  - **ServerPack**: Skips export entirely

`--show-io-errors`
: Show file IO error on exporting. (These can be ignored most of the time.)

</snippet>

`-h`, `--help`
: Show the help message and exit

</snippet>

