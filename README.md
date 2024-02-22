
<p align="center">  
  <a href="https://github.com/juraj-hrivnak/pakku">
    <img
      src="https://github.com/juraj-hrivnak/Pakku/assets/71150936/818cb871-15eb-4052-9577-dc8ba75e0855"
      alt="Logo"
      width="200"
    >
  </a>
  <h1 align="center">Pakku</h1>
</p>


<p align="center">
  <a href="https://github.com/juraj-hrivnak/Pakku/actions/workflows/Build.yml">
    <img
      src="https://github.com/juraj-hrivnak/Pakku/actions/workflows/Build.yml/badge.svg"
      alt="Build"
    >
  </a>
  <a href="https://github.com/juraj-hrivnak/Pakku/actions/workflows/Build.yml">
    <img
      src="https://img.shields.io/github/downloads/juraj-hrivnak/Pakku/total?color=light&label=Downloads"
      alt="Downloads"
    >
  </a>
</p>

<p align="center">
  A multiplatform modpack manager for Minecraft: Java Edition.
</p>

<p align="center">
  With Pakku you can create a modpack for CurseForge, Modrinth or both simultaneously. <br>
  To create a modpack you can either start from scratch or import an existing modpack.
</p>

<p align="center">
  Pakku includes many features to make your life easier as a modpack developer. 
</p>

<h2 align="center">Usage</h2>

```
// Java:
java -jar pakku.jar [<options>] <command> [<args>]...

// Native - Linux, macOS, Windows:
./pakku [<options>] <command> [<args>]...
./pakku-macos [<options>] <command> [<args>]...
.\pakku.exe [<options>] <command> [<args>]...
```

<h3 align="center">System Requirements</h3>

<p align="center">
  macOS <b>11 or higher</b>
</p>

<h2 align="center">Documentation</h2>

<p align="center">
  See <a href="https://juraj-hrivnak.github.io/Pakku-Docs/home.html">juraj-hrivnak.github.io/Pakku-Docs/</a>
</p>

<h2 align="center">Development</h2>

<p align="center">
  To build Pakku for the JVM, run the <code>gradlew jvmJar</code>. <br>
  To build Pakku using Kotlin/Native, run the <code>gradlew linkDebugExecutableNative</code> or <code>gradlew linkReleaseExecutableNative</code>.
</p>

