
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

### Multiplatform :dna: 

Modpack development with the split of mod hosting platforms has become significantly harder. Pakku addresses this problem and tries to be as helpful as possible when developing a multiplatform modpack.

When adding a mod, resource pack or shader (further referred to as "Project") to your modpack, Pakku has a robust way to find it across both platforms. And even If this fails, you can specify the project slug, ID, or version manually.

### Optimized :abacus: 

Pakku is multithreaded to ensure everything is done as quickly as possible.

Pakku is also efficient with its HTTP requests, reducing them to a minimum. This ensures that data is sent quicker back to you and that you do not get rate-limited by adding or updating too many projects at once.

### Configurable :nut_and_bolt: 

Pakku utilises a design used by many well-known package managers like npm or Cargo, which consists of two files, the config file and the lock file.

This allows you to fine-tune your modpack, while still being up-to-date. If any data retrieved from the platforms does not suit your requirements, you can simply override it in the config file.

### Visit the [**Documentation**](https://juraj-hrivnak.github.io/Pakku) for installation instructions and a comprehensive guide on how to use it.

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

## Quickstart

- Install Pakku using Scoop for Windows
  ```
  scoop install https://juraj-hrivnak.github.io/Pakku/install/pakku.json
  ```
  
- Install Pakku using Brew for macOS (or Linux)
  ```
  brew install juraj-hrivnak/pakku/pakku
  ```
  
### Usage

```
pakku [<options>] <command> [<args>]...
```

## Development

To build Pakku for the JVM, run the <code>gradlew jvmJar</code>. <br>

## License

Licensed under the EUPL-1.2-or-later
   
[GitHub releases]: https://github.com/juraj-hrivnak/Pakku/releases/latest
