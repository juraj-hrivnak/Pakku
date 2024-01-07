
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

[![CodeQL](https://github.com/juraj-hrivnak/Pakku/actions/workflows/codeql.yml/badge.svg)](https://github.com/juraj-hrivnak/Pakku/actions/workflows/codeql.yml)

A multiplatform modpack manager for Minecraft: Java Edition.

## Usage

```
java -jar Pakku-<version>.jar [<options>] <command> [<args>]...
```

<table>
<thead>
  <tr>
    <th colspan="2">Options</th>
  </tr>
</thead>
<tbody>
  <tr>
    <td>--debug</td>
    <td>Enable additional debug logging</td>
  </tr>
  <tr>
    <td>-h, --help</td>
    <td>Show help message and exit</td>
  </tr>
</tbody>
</table>
<table>
<thead>
  <tr>
    <th colspan="2">Commands</th>
  </tr>
</thead>
<tbody>
  <tr>
    <td>set</td>
    <td>Set pack name, Minecraft versions and loaders</td>
  </tr>
  <tr>
    <td>add</td>
    <td>Add projects</td>
  </tr>
  <tr>
    <td>rm</td>
    <td>Remove projects</td>
  </tr>
  <tr>
    <td>update</td>
    <td>Update projects</td>
  </tr>
  <tr>
    <td>ls</td>
    <td>List projects</td>
  </tr>
  <tr>
    <td>fetch</td>
    <td>Fetch projects to your pack folder</td>
  </tr>
  <tr>
    <td>link</td>
    <td>Link project to another project</td>
  </tr>
</tbody>
</table>
