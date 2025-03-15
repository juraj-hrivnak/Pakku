# Pakku Directory

Pakku directory (`.pakku`) can be used to define [manual overrides](Pakku-Terminology.md#override)
and is also used for other Pakku functionality.

Pakku directory must be located in your modpack directory and must be named: `.pakku`.

## Subdirectories

<deflist type="medium">
    <def>
        <title><code>overrides</code></title>
        <p>Directory for adding manual overrides.</p>
    </def>
    <def>
        <title><code>server-overrides</code></title>
        <p>Directory for adding manual server overrides.</p>
    </def>
    <def>
        <title><code>client-overrides</code></title>
        <p>Directory for adding manual client overrides.</p>
    </def>
    <def>
        <title><code>remote</code></title>
        <p>Directory containing the local Git repository when a remote modpack is installed.</p>
    </def>
    <def>
        <title><code>shelf</code></title>
        <p>Directory used when running <code>pakku fetch --shelve</code>.</p>
    </def>
    <def>
        <title><code>docs</code></title>
        <p>Directory used when running  <code>pakku --generate-docs</code>.</p>
    </def>
</deflist>