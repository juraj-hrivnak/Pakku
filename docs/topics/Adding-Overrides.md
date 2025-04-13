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

The following rules are used to interpret glob patterns:

- The `*` character matches zero or more characters of a path name component without crossing directory boundaries.

- The `**` characters matches zero or more characters crossing directory boundaries.

- The `?` character matches exactly one character of a path name component.

- The backslash character `\` is used to escape characters that would otherwise be interpreted as special characters. 
The expression `\\` matches a single backslash and `\{` matches a left brace for example.

- The `[ ]` characters are a _bracket expression_ that match a single character of a name component out of a set of characters.
For example, `[abc]` matches `a`, `b`, or `c`. The hyphen (`-`) may be used to specify a range so `[a-z]`specifies a range that matches from `a` to `z` (inclusive).
These forms can be mixed so `[abce-g]` matches `a`, `b`, `c`, `e`, `f` or `g`.
If the character after the `[` is a `!` then it is used for negation so `[!a-c]` matches any character except `a`, `b`, or `c`.  
Within a bracket expression the `*`, `?` and `\` characters match themselves.
The `-` character matches itself if it is the first character within the brackets, or the first character after the `!` if negating.

- The `{ }` characters are a group of subpatterns, where the group matches if any subpattern in the group matches. 
The `,` character is used to separate the subpatterns. Groups cannot be nested.

- If the first character is the `!` character, any matching file included by a previous pattern will become excluded again.

<table>
    <tr><td>Example</td><td>Description</td></tr>
    <tr>
        <td><code>*.json</code></td>
        <td>Matches a path that represents a file name ending in <code>.json</code>.</td>
    </tr>
    <tr>
        <td><code>*.*</code></td>
        <td>Matches file names containing a dot.</td>
    </tr>
    <tr>
        <td><code>*.{json,conf}</code></td>
        <td>Matches file names ending with <code>.json</code> or <code>.conf</code>.</td>
    </tr>
    <tr>
        <td><code>foo.?</code></td>
        <td>Matches file names starting with <code>foo.</code> and a single character extension.</td>
    </tr>
    </table>