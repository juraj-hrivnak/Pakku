# Config File

A config file (`pakku.json`) is a file dedicated for the user.

It should be located in your modpack directory (`.minecraft`).

It must be present when exporting the modpack 
or when you need to use some of Pakku's more advanced functionalities.

## Properties

<deflist>
    <def id="name">
        <title><code>name</code></title>
        <p>The name of the modpack.</p>
        <p>
            Type: <code>String</code>
        </p>
    </def>
    <def id="version">
        <title><code>version</code></title>
        <p>The version of the modpack.</p>
        <p>
            Type: <code>String</code>
        </p>
    </def>
    <def id="description">
        <title><code>description</code></title>
        <p>Description of the modpack.</p>
        <p>
            Type: <code>String</code>
        </p>
    </def>
    <def id="author">
        <title><code>author</code></title>
        <p>The author of the modpack.</p>
        <p>
            Type: <code>String</code>
        </p>
    </def>
    <def id="overrides">
        <title><code>overrides</code></title>
        <p>
            A list of <a href="Pakku-Terminology.md" anchor="override">overrides</a>.
        </p>
        <p>
            Type: <code>List&lt;String&gt;</code> (glob)
        </p>
    </def>
    <def id="server_overrides">
        <title><code>server_overrides</code></title>
        <p>
            A list of <a href="Pakku-Terminology.md" anchor="override">server overrides</a>.
        </p>
        <p>
            Type: <code>List&lt;String&gt;</code> (glob)
        </p>
    </def>
    <def id="client_overrides">
        <title><code>client_overrides</code></title>
        <p>
            A list of <a href="Pakku-Terminology.md" anchor="override">client overrides</a>.
        </p>
        <p>
            Type: <code>List&lt;String&gt;</code> (glob)
        </p>
    </def>
    <def id="export_server_side_projects_to_client">
        <title><code>export_server_side_projects_to_client</code></title>
        <note>
            <p>Added in version <b>1.3.3</b></p>
        </note>
        <p>
            Controls how server-side mods (mods with <code>side: SERVER</code>) are handled when exporting client modpacks.
        </p>
        <p>
            <b>When <code>false</code> (recommended for new projects):</b>
        </p>
        <list>
            <li><b>CurseForge:</b> Server-side mods are excluded from the manifest (CurseForge doesn't support environment fields)</li>
            <li><b>Modrinth:</b> Server-side mods are included with <code>env.client = "unsupported"</code> (correctly follows side constraints using Modrinth's native env field support)</li>
        </list>
        <p>
            <b>When <code>true</code> (default for backward compatibility):</b>
        </p>
        <list>
            <li><b>CurseForge:</b> Server-side mods are included in the manifest</li>
            <li><b>Modrinth:</b> Server-side mods are included with <code>env.client = "required"</code> (treated as BOTH-side for compatibility)</li>
        </list>
        <note>
            <p>
                Existing modpacks are automatically migrated to <code>true</code> to maintain backward compatibility.
                New modpacks created with <code>pakku init</code> default to <code>false</code> for correct filtering behavior.
            </p>
        </note>
        <p>
            Type: <code>Boolean</code>
        </p>
        <p>
            Default: <code>true</code> (after migration), <code>false</code> (new projects)
        </p>
    </def>
    <def id="paths">
        <title><code>paths</code></title>
        <p>A map of project types to their respective paths.</p>
        <p>
            Type: <code>Map&lt;String, String&gt;</code>
        </p>
    </def>
    <def id="projects">
        <title><code>projects</code></title>
        <p>
            A list of project slugs, names, or IDs from the <a href="Lock-File.md">lock file</a>
            with <a href="#project_properties">properties</a> you want to change.
        </p>
        <h2 id="project_properties">Project Properties</h2>
        <deflist>
            <def id="project_type">
                <title><code>type</code></title>
                <p>
                    Change the type of the provided project.
                </p>
                <p>
                    Type: 
                    <code>
                        <a href="https://juraj-hrivnak.github.io/Pakku/api/-pakku/teksturepako.pakku.api.projects/-project-type/index.html"> 
                            <p>ProjectType</p>
                        </a>
                    </code>
                </p>
            </def>
            <def id="project_side">
                <title><code>side</code></title>
                <p>
                    Change the side of the provided project.
                </p>
                <p>
                    Type: 
                    <code>
                        <a href="https://juraj-hrivnak.github.io/Pakku/api/-pakku/teksturepako.pakku.api.projects/-project-side/index.html"> 
                            <p>ProjectSide</p>
                        </a>
                    </code>
                </p>
            </def>
            <def id="project_update_strategy">
                <title><code>update_strategy</code></title>
                <p>
                    Change the update strategy of the provided project.
                </p>
                <p>
                    Type: 
                    <code>
                        <a href="https://juraj-hrivnak.github.io/Pakku/api/-pakku/teksturepako.pakku.api.projects/-update-strategy/index.html"> 
                            <p>UpdateStrategy</p>
                        </a>
                    </code>
                </p>
            </def>
            <def id="project_redistributable">
                <title><code>redistributable</code></title>
                <p>
                    Change whether the provided project can be redistributed.
                </p>
                <p>
                    Type: <code>Boolean</code>
                </p>
            </def>
            <def id="project_subpath">
                <title><code>subpath</code></title>
                <p>
                    Change the subpath of the provided project.
                </p>
                <p>
                    Type: <code>String</code> (path)
                </p>
            </def>
            <def id="project_aliases">
                <title><code>aliases</code></title>
                <p>
                    A list of aliases for the provided project.
                </p>
                <p>
                    Type: <code>List&lt;String&gt;</code>
                </p>
            </def>
            <def id="project_export">
                <title><code>export</code></title>
                <p>
                    Change whether the provided project will be exported.
                </p>
                <p>
                    Type: <code>Boolean</code>
                </p>
            </def>
        </deflist>
    </def>
</deflist>
