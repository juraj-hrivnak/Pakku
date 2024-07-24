# Setting Up a Modpack

To set up a modpack, you can:

- Start form scratch and [create a new modpack](#creating-a-new-modpack).
- [Import an existing modpack](#importing-an-existing-modpack).
- [Migrate existing modpack from one platform to another or import modpack as multiplatform modpack](#migrating-modpack-s-platform-or-importing-as-multiplatform-modpack).

## Creating a New Modpack

To create a new modpack, run the [`pakku init`](pakku-init.md) command:

<include from="pakku-init.md" element-id="snippet-cmd"></include>

This will ask you to enter:

1. The name of the modpack.
2. The Minecraft versions supported by the modpack.
3. The mod loaders and their versions supported by the modpack.
4. The targeted platform of the modpack.

I will also set the version of the modpack to `0.0.1` and add `config` folder to the overrides.

If you made a mistake, you can change the properties of the modpack
using the [`pakku set`](pakku-set.md) command.

## Importing an Existing Modpack

To import an existing modpack, run the [`pakku import`](pakku-import.md) command with the `<path>` argument:

<include from="pakku-import.md" element-id="snippet-cmd"></include>

The `<path>` argument will only accept:
- CurseForge's modpack with the `.zip` extension or the `manifest.json`.
- Modrinth's modpack with the `.mrpack` extension or the `modrinth.index.json`.

Pakku automatically sets the targeted platform of your modpack based on the file you import,
and imports [projects](Pakku-Terminology.md#project).

Pakku does not import [overrides](Pakku-Terminology.md#override).

## Migrating Modpack's Platform or Importing as Multiplatform Modpack


You can import a CurseForge modpack as a Modrinth modpack or vice versa by doing the following:

<procedure>
<step>

[Create a new modpack](#creating-a-new-modpack) using the [`pakku init`](pakku-init.md) command.

</step>

<step>

Set the target to the platform you want to import your modpack to.

</step>

<step>

[Import the modpack](#importing-an-existing-modpack) using the [`pakku import`](pakku-import.md) command.

</step>
</procedure>

[//]: # (--)

You can import an existing CurseForge or Modrinth modpack as a multiplatform modpack by doing the following:

<procedure>
<step>

[Create a new modpack](#creating-a-new-modpack) using the [`pakku init`](pakku-init.md) command.

</step>

<step>

Set the target to the `multiplatform` target.

</step>

<step>

[Import the modpack](#importing-an-existing-modpack) using the [`pakku import`](pakku-import.md) command.

</step>
</procedure>

<seealso style="cards">
   <category ref="related">
       <a href="Config-File.md"/>
       <a href="Lock-File.md"/>
   </category>
</seealso>

