# About Pakku

With **Pakku**, you can create modpacks for **CurseForge**, **Modrinth** or **both simultaneously**.

## Multiplatform :dna:

Modpack development with the split of mod hosting platforms has become significantly harder. Pakku addresses this problem and tries to be as helpful as possible when developing a multiplatform modpack.

When adding a mod, resource pack or shader (further referred to as "Project") to your modpack, Pakku has a robust way to find it across both platforms. And even If this fails, you have the option to specify the project slug, ID, or version manually.

## Optimized :abacus:

Pakku is multithreaded to ensure everything is done as quickly as possible.

Pakku is also efficient with its HTTP requests, reducing them to a minimum. This ensures that data is sent quicker back to you and that you do not get rate-limited by adding or updating too many projects at once.

## Configurable :nut_and_bolt:

Pakku utilises a design used by many well-known package managers like npm or Cargo, which consists of two files, the config file and the lock file.

This allows you to fine-tune your modpack, while still being up-to-date. If any data retrieved from the platforms does not suit your requirements, you can simply override it in the config file.

Pakku also does not limit you to how many mod loaders or Minecraft versions you can use. Any combination is possible, and you are only limited to the individual project requirements.

## Prepared for the Future

Pakku is designed to account for the possibility of new platforms to arise if that ever happens.

## Simple Workflow

Pakku includes many features to make your life easier as a modpack developer, including:

- Create a new modpack using: [`pakku init`](pakku-init.md).
- Import a modpack using: [`pakku import`](pakku-import.md).
- Choose between CurseForge, Modrinth or Multiplatform target.
- Add [projects] using: [`pakku add`](pakku-add.md).
    - Dependencies are handled automatically.
    - Pakku also tries to detect the project side and redistribution permission options.
- Remove [projects] using: [`pakku rm`](pakku-rm.md).
- Update a [project] or all [projects] using: [`pakku update`](pakku-update.md)
or [`pakku update --all`](pakku-update.md#options).
    - Use [`pakku set <project> -u`](pakku-set.md) to change the update strategy.
- Prepare your development environment using: [`pakku fetch`](pakku-fetch.md).
- List all [projects] in your modpack using: [`pakku ls`](pakku-ls.md).
- Export the modpack using [`pakku export`](pakku-export.md).

[project]: Pakku-Terminology.md#project
[projects]: Pakku-Terminology.md#project
