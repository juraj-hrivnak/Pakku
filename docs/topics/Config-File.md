# Config File

A config file (`pakku.json`) is a file dedicated for the user.

It must be present when exporting the modpack 
or when you need to use some of Pakku's more advanced functionalities.

## Properties

<deflist collapsible="true">
    <def>
        <title><code>name</code></title>
        <p>The name of the modpack.</p>
        <h2 id="example-name">Example</h2>
        <code-block collapsed-title="pakku.json" collapsible="true">
            {
                "name": "Example-Modpack"
            }
        </code-block>
    </def>
    <def>
        <title><code>version</code></title>
        <p>The version of the modpack.</p>
        <h2 id="example-version">Example</h2>
        <code-block collapsed-title="pakku.json" collapsible="true">
            {
                "version": "1.0.0"
            }
        </code-block>
    </def>
    <def>
        <title><code>description</code></title>
        <p>Description of the modpack.</p>
    </def>
    <def>
        <title><code>author</code></title>
        <p>The author of the modpack.</p>
    </def>
    <def>
        <title><code>overrides</code></title>
        <p>
            A list of <a href="Pakku-Terminology.md" anchor="override">overrides</a> packed up with the modpack.
        </p>
    </def>
    <def>
        <title><code>server_overrides</code></title>
        <p>
            A list of <a href="Pakku-Terminology.md" anchor="override">server overrides</a> packed up with the modpack.
        </p>
    </def>
    <def>
        <title><code>client_overrides</code></title>
        <p>
            A list of <a href="Pakku-Terminology.md" anchor="override">client overrides</a> packed up with the modpack.
        </p>
    </def>
    <def>
        <title><code>paths</code></title>
        <p>A map of project types to their respective paths.</p>
    </def>
    <def>
        <title><code>projects</code></title>
        <p>
            A list of project slugs, names, or IDs from the <a href="Lock-File.md">lock file</a>
            with <a href="#project_properties">properties</a> you want to change.
            <br/>
            <br/>
            You can use <a href="pakku-cfg.md">cfg</a> command to change the properties as well.
        </p>
        <h2 id="example-projects">Example</h2>
        <code-block collapsed-title="pakku.json" collapsible="true">
            {
                "projects": {
                    "&lt;project&gt;": {
                        &lt;properties&gt;...
                    }
                }
            }
        </code-block>
        <h2 id="project_properties">Properties</h2>
        <deflist collapsible="true">
            <def>
                <title><code>aliases</code></title>
                <p>
                    A list of aliases for the provided project.
                </p>
            </def>
            <def>
                <title><code>redistributable</code></title>
                <p>
                    Change whether the provided project can be redistributed.
                </p>
            </def>
            <def>
                <title><code>side</code></title>
                <p>
                    Change the side of the provided project.
                </p>
            </def>
            <def>
                <title><code>subpath</code></title>
                <p>
                    Change the subpath of the provided project.
                </p>
            </def>
            <def>
                <title><code>type</code></title>
                <p>
                    Change the type of the provided project.
                </p>
            </def>
            <def>
                <title><code>update_strategy</code></title>
                <p>
                    Change the update strategy of the provided project.
                </p>
            </def>
        </deflist>
    </def>
</deflist>