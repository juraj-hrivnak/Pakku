# Exporting a Modpack

To export a modpack, run the [`pakku export`] command:

<var name="arg" value="[<path>]"/>
<include from="pakku-export.md" element-id="snippet-cmd"></include>

The `[<path>]` argument is optional.

Depending on your [target](Lock-File.md#properties)
this will export the modpack in the format of the platforms you target.

<note>
If a project is missing on one of the platforms,
Pakku will try to add the file from the other platform,
but only if it is redistributable.
</note>

Example output:

<img src="screenshot_export.png" alt="export image"/>

## Adding Overrides

[Overrides](Pakku-Terminology.md#override) are simply
directories which you want to bundle with your modpack.
This can be, for example, the `config` directory,
or the CraftTweaker/KubeJS script directories.

An override can also be a single file.
In the case that you want to bundle a `README` or other files with your modpack.

Overrides must be added to the [config file's](Config-File.md)
`overrides` field for Pakku to be able to recognize them.

<procedure title="To add an override:">
<step>

Open the [config file](Config-File.md) (`pakku.json`)
and find a field called `overrides`.

</step>
<step>

Add the directory (or file) name to the `overrides` field.

For example, if we want to add the `config` directory,
it will look like this:
```JSON
"overrides": [
    "scripts"
]
```

</step>
</procedure>

## Overriding Projects' Properties

If a project has a property that is not correct
(marked with a wrong side or as not redistributable,
even though you have explicit permission to redistribute it)
you can override it in the [config file](Config-File.md).

<procedure title="To override a property:">
<step>

Open the [config file](Config-File.md) (`pakku.json`)
and find a field called `projects`.

</step>
<step>

Use the project slug, name, ID or filename to create
a new field in it.

For example, if we use the JEI mod,
it will look like this:
```JSON
"projects": {
    "jei": {
       
    }
}
```

</step>
<step>

And finally, add the property with the value you want to override.

In our example, we will override JEI's project side to `CLIENT`:
```JSON
"projects": {
    "jei": {
        "side": "CLIENT"
    }
}
```

</step>
</procedure>

## Exporting a Server Pack

To export a server pack, run the [`pakku export`] command
with the [`-s`] or [`--server-pack`] flag:

<var name="arg" value="[<path>] --server-pack"/>
<include from="pakku-export.md" element-id="snippet-cmd"></include>

The `[<path>]` argument is optional.

This will export only:

- Projects with the `BOTH` or `SERVER` side. Project with no side will be exported as `BOTH`.
- `overrides` and `server_overrides` defined in the [config file](Config-File.md) (`pakku.json`),
- [project overrides](Pakku-Terminology.md#project-override) included in the 
`.pakku/overrides` or `.pakku/server-overrides` directory.

## File Director Integration

If your modpack contains [File Director](https://github.com/TerraFirmaCraft-The-Final-Frontier/FileDirector),
Pakku will automatically add missing projects to its config,
instead of packing them as [project overrides](Pakku-Terminology.md#project-override).

[`pakku export`]: pakku-export.md
[`-s`]: pakku-export.md#options
[`--server-pack`]: pakku-export.md#options