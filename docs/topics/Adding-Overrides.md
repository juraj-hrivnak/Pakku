# Adding Overrides

[Overrides](Pakku-Terminology.md#override) are directories or files that you want to bundle with your modpack.

For example, this can be the `config` directory, or CraftTweaker/KubeJS script directories.

An override can also be a single file, in the case that you want to bundle a `README.md` or other files with your modpack.

Overrides must be added to the [config file's](Config-File.md) `overrides`, `server_overrides` or `client_overrides`
properties for Pakku to be able to recognize them.

<procedure title="To add an override:">
<step>

Open the [config file](Config-File.md) (`pakku.json`)
and find a property called `overrides`.

</step>
<step>

Add the directory (or file) name to the `overrides` field.

For example, if we want to add the `config` directory,
it will look like this:
```JSON
{
  "overrides": [
    "config"
  ]
}
```

</step>
</procedure>