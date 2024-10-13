# Config File

A config file (`pakku.json`) is a file used by the user to configure properties needed for
modpack export.

## Properties

<deflist>
    <def>
        <title><code>name</code></title>
        <p>The name of the modpack.</p>
    </def>
    <def>
        <title><code>version</code></title>
        <p>The version of the modpack.</p>
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
            A list of project slugs, names, IDs from the <a href="Lock-File.md">lock file</a>
            with properties you want to change.
            You can use <a href="pakku-cfg.md">cfg</a> command to change the properties as well.
        </p>U
    </def>
</deflist>