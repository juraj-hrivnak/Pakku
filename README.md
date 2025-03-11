
<p align="center">
  <a href="https://github.com/juraj-hrivnak/pakku">
    <img
      src="https://github.com/juraj-hrivnak/Pakku/assets/71150936/818cb871-15eb-4052-9577-dc8ba75e0855"
      alt="Logo"
      width="200"
    />
  </a>
  <h1 align="center">Pakku</h1>
</p>

<p align="center">
  A multiplatform modpack manager for Minecraft: Java Edition.
</p>

<p align="center">
  <a href="https://github.com/juraj-hrivnak/Pakku/actions/workflows/Build.yml">
    <img
      src="https://github.com/juraj-hrivnak/Pakku/actions/workflows/Build.yml/badge.svg"
      alt="Build"
    />
  </a>
  <a href="https://discord.com/invite/dtAyqdzTMj">
    <img
      alt="Discord"
      src="https://img.shields.io/discord/1207079018193616986?label=Discord"
    />
  </a>
  <a href="https://github.com/juraj-hrivnak/Pakku/actions/workflows/Build.yml">
    <img
      src="https://img.shields.io/github/downloads/juraj-hrivnak/Pakku/total?color=light&label=Downloads"
      alt="Downloads"
    />
  </a>
  <a href="https://www.codefactor.io/repository/github/juraj-hrivnak/pakku">
    <img
      src="https://www.codefactor.io/repository/github/juraj-hrivnak/pakku/badge"
      alt="CodeFactor"
    />
  </a>
</p>

## About

With **Pakku**, you can create modpacks for **CurseForge**, **Modrinth** or **both simultaneously**.

It's a package manager that significantly simplifies Minecraft modpack development, inspired by package managers like npm and Cargo.
In addition to package management itself, it enables support for version control, simplifies collaboration options, and adds support for CI/CD.

## Features

**A comprehensive toolkit for modpack development:**

- Project operations
  - `pakku add` - Add new projects with automatic dependency resolution
  - `pakku rm` - Remove projects safely with dependency checking
  - `pakku update` - Update projects individually or in bulk
  - `pakku ls` - List and inspect project details
  - `pakku diff` - Compare different versions of your modpack

- Development environment
  - `pakku init` - Create a new modpack
  - `pakku import` - Import existing modpacks
  - `pakku fetch` - Set up or update your development environment
  - `pakku sync` - Sync your local project files with the modpack

- Modpack distribution
  - `pakku export` - Create distribution-ready packages
  - `pakku remote` - Install a modpack from a Git URL (great for server owners)

## Documentation 📚

Visit the [**Documentation**](https://juraj-hrivnak.github.io/Pakku) for installation instructions and a comprehensive guide on how to use it.

## Images

<p align="center">
  <a href="https://github.com/juraj-hrivnak/pakku">
    <img
      src="docs/images/screenshot_export.png"
      alt="Exporting a Modpack"
    />
  </a>
  <a href="https://github.com/juraj-hrivnak/pakku">
    <img
      src="docs/images/screenshot_ls.png"
      alt="Listing Projects"
      width="800"
    />
  </a>
</p>

## Development

For a complete development and setup guide, check out our [Contributing Guide](CONTRIBUTING.md).

Contributions are very welcomed, from code to documentation improvements!

_Join our [Discord](https://discord.com/invite/dtAyqdzTMj) if you have any questions._

## License

Licensed under the EUPL-1.2-or-later
   
[GitHub releases]: https://github.com/juraj-hrivnak/Pakku/releases/latest
