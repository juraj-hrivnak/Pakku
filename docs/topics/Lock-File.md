# Lock File

A lock file (`pakku-lock.json`) is an automatically generated file
used by Pakku to define all [properties](#properties) of a modpack needed for its development.

It should be located in your modpack directory (`.minecraft`).

This file is not intended to be modified manually.

## Properties

<deflist>
    <def id="target">
        <title><code>target</code></title>
        <p>The targeted platform of the modpack.</p>
        <table>
            <tr>
                <td>Possible value</td>
                <td>Description</td>
            </tr>
            <tr>
                <td><code>curseforge</code></td>
                <td>
                    Pakku will use the CurseForge platform as a target of the modpack.
                </td>
            </tr>
            <tr>
                <td><code>modrinth</code></td>
                <td>
                    Pakku will use the Modrinth platform as a target of the modpack.
                </td>
            </tr>
            <tr>
                <td><code>multiplatform</code></td>
                <td>
                    Pakku will use both CurseForge and Modrinth platforms as a target of the modpack.
                </td>
            </tr>
        </table>
        <p>
            Type: <code>String</code>
        </p>
    </def>
    <def id="mc_versions">
        <title><code>mc_versions</code></title>
        <p>The Minecraft versions supported by the modpack.</p>
        <p>
            Type: <code>List&lt;String&gt;</code>
        </p>
    </def>
    <def id="loaders">
        <title><code>loaders</code></title>
        <p>A map of loader names to loader versions supported by the modpack.</p>
        <p>
            Type: <code>Map&lt;String, String&gt;</code>
        </p>
        <note>
            Provided loader names are always formated to lowercase.
        </note>
    </def>
    <def id="projects">
        <title><code>projects</code></title>
        <p>A list of projects included in the modpack.</p>
        <p>
            Type: 
            <code>
                List&lt;
                <a href="https://juraj-hrivnak.github.io/Pakku/api/-pakku/teksturepako.pakku.api.projects/-project/index.html"> 
                    <p>Project</p>
                </a>&gt;
            </code>
        </p>
    </def>
    <def id="lockfile_version">
        <title><code>lockfile_version</code></title>
        <p>The version of the lock file format. Used for migration purposes.</p>
        <table>
            <tr>
                <td>Version</td>
                <td>Description</td>
            </tr>
            <tr>
                <td><code>1</code></td>
                <td>Initial version. Projects created before version 1.3.4.</td>
            </tr>
            <tr>
                <td><code>2</code></td>
                <td>Added in version 1.3.4. Indicates that <code>export_server_side_projects_to_client</code> migration has been applied.</td>
            </tr>
        </table>
        <p>
            Type: <code>Int</code>
        </p>
        <p>
            Default: <code>1</code>
        </p>
    </def>
</deflist>