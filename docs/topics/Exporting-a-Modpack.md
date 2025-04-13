# Exporting a Modpack

To export a modpack, run the [`pakku export`] command:

<include from="pakku-export.md" element-id="snippet-cmd"></include>

Depending on your [target](Lock-File.md#properties),
this will export the modpack in the CurseForge's or Modrinth's format or both.

> If a project is missing on the platform you are exporting,
> Pakku will try to add the file from the other platforms,
> but only if it is redistributable.
{style=note}

The location of the output files is: `./build/<export_profile>/<modpack_file>`.<br>
Where: `<export_profile>` can be `curseforge`, `modrinth` or `serverpack`,
and `<modpack_file>` is based on your modpack's name and version.

Example output: 

<img src="screenshot_export.png" alt="export image"/>

## File Director Integration

If your modpack contains [File Director](https://github.com/TerraFirmaCraft-The-Final-Frontier/FileDirector), Pakku will automatically add missing projects to its config,
instead of packing them as [overrides](Pakku-Terminology.md#override).

[`pakku export`]: pakku-export.md