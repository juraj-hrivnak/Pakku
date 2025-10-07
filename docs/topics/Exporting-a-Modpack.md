# Exporting a Modpack

To export your modpack, run the [`pakku export`] command:

<include from="pakku-export.md" element-id="snippet-cmd"></include>

Pakku will export your modpack in the following formats depending on your target:

<deflist type="medium">
  <def>
    <title>CurseForge (<code>.zip</code>) format</title>
    <list>
    <li><p>if your target is <code>curseforge</code> or <code>multiplatform</code></p></li>
    </list>
    <list>
    <li><p>path: <code>./build/curseforge/&lt;output_file&gt;</code></p></li>
    </list>
    </def>
  <def>
    <title>Modrinth (<code>.mrpack</code>) format</title>
    <list>
    <li><p>if your target is <code>modrinth</code> or <code>multiplatform</code></p></li>
    </list>
    <list>
    <li><p>path: <code>./build/modrinth/&lt;output_file&gt;</code></p></li>
    </list>
  </def>
  <def>
    <title>ServerPack (<code>.zip</code>) format</title>
    <list>
    <li><p>path: <code>./build/serverpack/&lt;output_file&gt;</code></p></li>
    </list>
  </def>
</deflist>

The `<output_file>` is based on your modpack's name and version.

Example output:

<img src="screenshot_export.png" alt="export image"/>

## How It Works?

Pakku uses an export profile system under the hood.
Currently, it is only possible to export the default profiles (`curseforge`, `modrinth` and `serverpack`).
However, exposing this functionality to users using scripting is planned for the v2.0.

Benefits from export profile system:

- Export profiles consist of number of export rule which control what happens on exporting.
- Export rules are purely functional.
- Each profile is independent and results in one exported file.

### Multiplatform Modpacks

Handling of projects in a multiplatform modpack:

```mermaid
graph LR
    A[Project] --> B{Is on current platform?}
    B -- Yes --> C[Directly include in manifest]
    B -- No --> D{Is redistributable?}
    D -- Yes --> E[Include as override]
    D -- No --> F[Ignore]
```

## File Director Integration

If your modpack contains [File Director](https://github.com/TerraFirmaCraft-The-Final-Frontier/FileDirector), Pakku will automatically add missing projects to its config,
instead of packing them as [overrides](Pakku-Terminology.md#override).

[//]: # (## Technical Implementation Details)

[//]: # ()
[//]: # (### Exporting Flow)

[//]: # ()
[//]: # (```mermaid)

[//]: # (sequenceDiagram)

[//]: # (    participant CMD as Export Function)

[//]: # (    participant PRF as Export Profiles)

[//]: # (    participant RUL as Export Rules)

[//]: # (    participant PKG as Packaging Actions)

[//]: # (    participant OUT as Output)

[//]: # ()
[//]: # (    CMD->>PRF: Builds)

[//]: # (    activate PRF)

[//]: # (    PRF->>RUL: Produces)

[//]: # (    activate RUL)

[//]: # (    RUL-->>PRF: Rule Results)

[//]: # (    deactivate RUL)

[//]: # (    PRF->>PKG: Runs)

[//]: # (    activate PKG)

[//]: # (    PKG-->>PRF: Package Data)

[//]: # (    deactivate PKG)

[//]: # (    PRF->>OUT: Generate Output)

[//]: # (    activate OUT)

[//]: # (    OUT-->>CMD: Export Complete)

[//]: # (    deactivate OUT)

[//]: # (    deactivate PRF)

[//]: # (```)

[`pakku export`]: pakku-export.md