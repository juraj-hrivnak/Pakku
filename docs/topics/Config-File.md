# Config File

A config file (`pakku.json`) is a file dedicated for the user.

It must be present when exporting the modpack 
or when you need to use some of Pakku's more advanced functionalities.

## Properties

<deflist collapsible="false">
    <def id="name">
        <title><code>name</code></title>
        <p>The name of the modpack.</p>
    </def>
    <def id="version">
        <title><code>version</code></title>
        <p>The version of the modpack.</p>
    </def>
    <def id="description">
        <title><code>description</code></title>
        <p>Description of the modpack.</p>
    </def>
    <def id="author">
        <title><code>author</code></title>
        <p>The author of the modpack.</p>
    </def>
    <def id="overrides">
        <title><code>overrides</code></title>
        <p>
            A list of <a href="Pakku-Terminology.md" anchor="override">overrides</a>
            packed up with the modpack.
        </p>
    </def>
    <def id="server_overrides">
        <title><code>server_overrides</code></title>
        <p>
            A list of <a href="Pakku-Terminology.md" anchor="override">server overrides</a>
            packed up with the modpack.
        </p>
    </def>
    <def id="client_overrides">
        <title><code>client_overrides</code></title>
        <p>
            A list of <a href="Pakku-Terminology.md" anchor="override">client overrides</a>
            packed up with the modpack.
        </p>
    </def>
    <def id="paths">
        <title><code>paths</code></title>
        <p>A map of project types to their respective paths.</p>
    </def>
    <def id="projects">
        <title><code>projects</code></title>
        <p>
            A list of project slugs, names, or IDs from the <a href="Lock-File.md">lock file</a>
            with <a href="#project_properties">properties</a> you want to change.
            <br/>
            <br/>
            You can use <a href="pakku-cfg.md">cfg</a> command to change the properties as well.
        </p>
        <h2 id="projects-usage">Usage</h2>
        <code-block collapsed-title="pakku.json" collapsible="true" lang="JSON5">
            {
                "projects": {
                    "&lt;project&gt;": {
                        [[[&lt;properties&gt;...|#project_properties]]]
                    }
                }
            }
        </code-block>
        <h2 id="project_properties">Properties</h2>
        <deflist collapsible="false">
            <def id="project_aliases">
                <title><code>aliases</code></title>
                <p>
                    A list of aliases for the provided project.
                </p>
            </def>
            <def id="project_redistributable">
                <title><code>redistributable</code></title>
                <p>
                    Change whether the provided project can be redistributed.
                </p>
            </def>
            <def id="project_side">
                <title><code>side</code></title>
                <p>
                    Change the side of the provided project.
                </p>
            </def>
            <def id="project_subpath">
                <title><code>subpath</code></title>
                <p>
                    Change the subpath of the provided project.
                </p>
            </def>
            <def id="project_type">
                <title><code>type</code></title>
                <p>
                    Change the type of the provided project.
                </p>
            </def>
            <def id="project_update_strategy">
                <title><code>update_strategy</code></title>
                <p>
                    Change the update strategy of the provided project.
                </p>
            </def>
        </deflist>
    </def>
</deflist>

## Example

```JSON5
{
    "[[[name|#name]]]": "Example-Modpack",
    "[[[version|#version]]]": "1.0.0",
    "[[[overrides|#overrides]]]": [
        "config", // Adding the 'config' folder as override.
        "README.md", // Adding files as overrides.
        "LICENSE"
    ],
    "[[[client_overrides|#client_overrides]]]": [
       /**
        * The 'resources' folder is added to the
        * [[[client_overrides|#client_overrides]]] because we don't  
        * want it to be included in the server pack.
        */
        "resources" 
    ],
    "[[[projects|#projects]]]": {
        "particles-mod-3000": {
           /**
            * From testing, we learned that this mod crashes
            * on the server, so we changed its project side
            * to 'CLIENT', so it won't be included in the server pack.
            */
            "[[[side|#project_side]]]": "CLIENT"
        }
    }
}
```
