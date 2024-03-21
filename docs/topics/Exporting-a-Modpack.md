# Exporting a Modpack

To export a modpack, run the `pakku export` command:

<include from="pakku-export.md" element-id="snippet-cmd"></include>

The `[<path>]` argument is optional.

Depending on your [target](Lock-File.md#properties)
this will export the modpack in the format one or both of the platforms.

Example:

<img src="screenshot_export.png" alt="export image"/>

## Overriding Projects' Properties

If a project has a property that is not correct
(marked with a wrong side or as not redistributable,
even though you have explicit permission to redistribute it)
you can override the property in the [config file](Config-File.md).

To override a property: 

1. Open the [config file](Config-File.md) (`pakku.json`)
and find an object called `projects`.

2. Use the project slug, name, ID or filename to create
an object in it. For example, if we will use JEI:
    ```JSON
    "projects": {
        "jei": {
        
        }
    }
    ```

3. Add the property with the value you want to override:
    <code-block lang="JSON" collapsible="true" collapsed-title="&quot;side&quot;: &quot;CLIENT&quot;">
    &quot;projects&quot;: {
       &quot;jei&quot;: {
           &quot;side&quot;: &quot;CLIENT&quot;
       }
    }
    </code-block>

## File Director Integration

If your modpack contains File Director, Pakku will automatically
add missing projects to its config, instead of packing them as
[project overrides](Pakku-Terminology.md#project-override).
