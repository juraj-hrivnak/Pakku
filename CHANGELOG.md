# Changelog

## Unreleased

## v0.14.2

- Fixed typo in the `export` command output path.

## v0.14.1

- Implemented auto generation of command parameters and arguments for docs.
- Updated the docs with better info on importing and command references.
- Made CLI message prefix color inverted (like it was before).

## v0.14.0

- Fixed import not recognizing files outside modpack folder.
- Removed `korlibs.io` dependency and added `kotlinx.atomicfu`.
- Unzipping files now uses `okio` under the hood.

## v0.13.1

- Removed unwanted `File not found: '.\.pakku\cli-config.json'` print 
when CLI Config does not exist.
- Set default CLI theme to Pakku's default theme instead of the _default one_.

## v0.13.0

- Implemented the CLI Config (`cli-config.json`), an optional file
which can be used to modify the UI aspects of the Pakku CLI.
  - It must be located in the [Pakku directory (`.pakku`)](https://juraj-hrivnak.github.io/Pakku/pakku-directory.html).
  - The relative path from the `.minecraft` directory should be: `.pakku/cli-config.json`.
  - Properties:
    - `theme`: can be `default` or `ascii`. (Defaults to `default` 😜.)
    - `ansi_level`: can be `none`, `ansi16`, `ansi256` or `truecolor`.
    (By default it is automatically detected based on your terminal/CMD.)
- `NoSuchFileException` is now wrapped as `FileNotFound` action error.
- Errors when creating the modpack zip file now return early.

## v0.12.1

- Fixed exceptions being uncaught when writing output files on export.
- Pakku now detects terminal interactivity and ansiLevel by default.

## v0.12.0

- Rewritten exporting and implemented _export rules_ and _export profiles_.
  - Exporting is now cached and asynchronous.
  - Internally, exporting a modpack is now controlled by a list of rules which control
    what should happen with the content you want to export/package.
    - _Export rules_ must be part of an _export profile_ to be executed.
    - Each _export profile_ is independent of each other and will result
      in one exported file.
    - This functionality can be currently only controlled using the API.
      For more information see the pull request [#18](https://github.com/juraj-hrivnak/Pakku/pull/18).
- Improved ZIP creation and removed zip4j.
- Dependencies are now not resolved on import by default.
  \- by @Wxrlds in pull request [#16](https://github.com/juraj-hrivnak/Pakku/pull/16).
  - There now is a `-D`, `--deps` flag, which resolves dependencies like before.
- Added "Updated" section to diff when using the `diff` command.
  \- by @Wxrlds in pull request [#17](https://github.com/juraj-hrivnak/Pakku/pull/17).
  - There now is a `-v`, `--verbose` flag which displays detailed information
    about which version of a project was updated to which version.
- Implemented modpack `@var@` replacements on export. `@name@`, `@version@`, `@description@` and `@author@`
  will be respectively replaced with the correct values from the _config file_ in the exported modpack.
- Improved some error messages and added hyperlinks to the exported modpack file path.
- Refactored project override syncing (with the modpack directory).
- Added initial `lockfile_version: 1` property to _lock file_.
  _Lock file_ without this property is still treated as v1.

## v0.11.3

- Improved resolving old files on `fetch`.

## v0.11.2

- Fixed error with `fetch` command.
- Added better path info for files on `fetch`.

## v0.11.1

### What's new?

- Fixed error with null icon in CurseForge projects. 
- Fixed the `fetch` command not resolving files other than from CurseForge.

## v0.11.0

- Refactored the `fetch` command to use the new fetch action API call.
  - Downloading project files and writing to disk are now in separate
  threads, making it way faster.
  - Hashes are now the preferred way to check for old project files
  instead of file names.
- Increased timeouts for HTTP requests to avoid issues with big modpacks.

## v0.10.0

### What's new?

- Implemented new UI for most of the CLI.
  - Slugs now contain hyperlinks. The `ls` command is now better formatted
  and contains type and side information for projects.
- Implemented the `status` command as a better way to check for updates
than the `ls -c` command.
- Improved `rm` command with better "autocomplete" for slugs.
- Improved importing - now supports importing without manually setting Minecraft
versions and loaders.
- Fixed issue with the `fetch` command not properly listing platforms.

### Docs
- Implemented auto generation for CLI Commands reference for docs.
- Added option to switch between Java and PATH examples in docs.

### API
- Refactored models
