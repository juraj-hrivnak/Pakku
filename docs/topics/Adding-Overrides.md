# Adding Overrides

[Overrides](Pakku-Terminology.md#override) are directories or files that will be bundled with your modpack on exporting.

For example, this can be the `config` directory, or CraftTweaker/KubeJS script directories.

An override can also be a single file, in the case that you want to bundle a `README.md` or other files with your modpack.

Overrides must be added to the [config file's](Config-File.md) [`overrides`](Config-File.md#overrides),
[`server_overrides`](Config-File.md#server_overrides) or [`client_overrides`](Config-File.md#client_overrides)
properties for Pakku to be able to recognize them.

Overrides configuration accepts the [glob pattern format](#pattern-format) as input.

<procedure title="Adding the `config` directory as an override">
<step>

Open the [config file](Config-File.md) (`pakku.json`)
and create or find a property called [`overrides`](Config-File.md#overrides).

</step>
<step>

Add the name of the directory, in our case "`config`", to the `overrides`:

```JSON
{
    "overrides": [
        "config"
    ]
}
```

</step>
</procedure>

<procedure title="Adding the `resources` directory as a client override">
<step>

Open the [config file](Config-File.md) (`pakku.json`)
and create or find a property called [`client_overrides`](Config-File.md#client_overrides).

</step>
<step>

Add the name of the directory, in our case "`resources`", to the `client_overrides`:

```JSON
{
    "client_overrides": [
        "resources"
    ]
}
```

</step>
</procedure>

### Pattern Format

- The slash "`/`" is used as the directory separator.
- An asterisk "`*`" matches anything except a slash.
The character "`?`" matches any character except "`/`".
The range notation, e.g. `[a-zA-Z]`, can be used to match one of the characters in a range.
- Two consecutive asterisks ("`**`") means match in all directories; `<directory>/**` means match everything inside the directory.
- An optional prefix "`!`" negates the pattern; any matching file included by a previous pattern will become excluded again.