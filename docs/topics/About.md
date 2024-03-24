# About Pakku

Pakku is a multiplatform modpack manager for Minecraft: Java Edition.

With Pakku, you can create a modpack for CurseForge, Modrinth or both simultaneously.
To create a modpack, you can either start from scratch or import an existing modpack.

## Features

Pakku includes many features to make your life easier as a modpack developer, including:

- Create a new modpack using: [`pakku init`](pakku-init.md).
- Import a modpack using: [`pakku import`](pakku-import.md).
- Choose between CurseForge, Modrinth or Multiplatform target.
- Add [projects] using: [`pakku add`](pakku-add.md).
    - On the Multiplatform target, Pakku tries to find the [project]
    on another platform using the slug or asks you to specify it manually.
    - Dependencies are handled automatically.
- Remove [projects] using: [`pakku rm`](pakku-rm.md).
- Update a [project] or all [projects] using: [`pakku update`](pakku-update.md)
or [`pakku update --all`](pakku-update.md#options).
- Prepare your development environment using: [`pakku fetch`](pakku-fetch.md).
- List all [projects] in your modpack using: [`pakku ls`](pakku-ls.md).
- Export the modpack using [`pakku export`](pakku-export.md).

## Optimized

Pakku is multithreaded to ensure everything is done as quickly as possible.

Pakku also uses batched HTTP requests. This ensures that data is sent quicker back to you
and that you do not get rate-limited by adding or updating too many [projects] at once.

## Configurable

Pakku allows you to fine-tune your configuration using the [config file](Config-File.md).

Pakku does not limit you to how many mod loaders or Minecraft versions you can use.
Any combination is possible, and you are only limited to the individual [project] requirements.

## Multiplatform

Pakku tries to be as helpful as possible when developing a multiplatform modpack.
It tries to find the [project] on another platform using its slug or asks you to specify it manually.

[project]: Pakku-Terminology.md#project
[projects]: Pakku-Terminology.md#project
