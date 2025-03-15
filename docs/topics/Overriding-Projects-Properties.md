# Overriding Project&#39;s Properties

If a project has a property that is not correct
(marked with a wrong side or as not redistributable,
even though you have explicit permission to redistribute it)
you can override it using the [config file](Config-File.md).

<procedure title="Changing project side to `CLIENT`">
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