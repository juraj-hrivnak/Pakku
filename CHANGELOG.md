
# Changelog

## Unreleased

## v1.2.0

- Implemented working path search up. You can now run Pakku anywhere in your modpack's subdirectories.
- Fixed `pakku init` command not asking for loaders.
- When configuring the CurseForge API key, Pakku will now test it to see if it works and return an error if it is not valid.
- When using the `pakku cfg prj` command, pakku will now remove empty project entries. 
For example, if this command: `pakku cfg prj -t mod example-mod` results in this JSON: `"example-mod": {}`, `"example-mod": {}` will be removed from `pakku.json`.

## v1.1.1

- Fixed encoding of spaces in URLs in the `modrinth.index.json` file when exporting.
- Fixed typo in Pattern Format section in docs.

## v1.1.0

- Fixed excluding files from subdirectories matched with glob pattern not working.
- Added proper documentation for the glob pattern format.
- Fixed broken debug prints when exporting with the `--debug` flag. 

## v1.0.0

> [!IMPORTANT]  
> Pakku now requires users to provide their own CurseForge API key when accessing CurseForge.  
> The embedded API key used in older versions of Pakku will be revoked in a month.
> 
> The transition is simple and can be done in two minutes. Please, [see the docs](https://juraj-hrivnak.github.io/Pakku/setting-up-a-modpack.html#configuring-curseforge-access) for more info.

> [!WARNING]  
> Deprecated argument and options from the `pakku set` command (namely: `[<projects>]...`, `-s`, `-u` and `-r`) have been removed.
> Please, use the `pakku cfg prj` command as the alternative.
### Highlights

- Pakku now allows you to install Pakku modpacks from **secure remote Git repositories** using the `pakku remote` command.
  - Example usage for *"autoupdating"* installation: `pakku remote https://github.com/juraj-hrivnak/CaveGameUltimate.git update`
  - More information can be found below.
- Implemented more robust error handling for HTTP requests.
- Pakku now requires users to provide a CurseForge API key when accessing CurseForge.
  - `pakku credentials` command has been added. Which can be used for your credentials' management.
- On Windows, Pakku now tries to configure your console output to use the UTF-8 encoding (code page 65001). If this fails, Pakku will use the `ASCII` CLI theme.
- Fixed Pakku incorrectly using backslash path separators in exported ZIP files when exporting on Windows.
- Pakku now allows adding files directly to the `.pakku/overrides`, `.pakku/server-overrides` or `.pakku/client-overrides` directories. This can be used, for example, to bundle your server installer with the server pack.
  - Due to this change, "Project Overrides" have been renamed to "Manual Overrides".
- Added the `export` property for projects in the config file, which can be used to exclude projects from being exported.
  - Example usage: 
    ```json5
    {
        "projects": {
            "spark": {
                "export": false // If you don't want Spark to be in the exported modpack 😉
            }
        }
    }
    ```
- Deprecated argument and options from the `pakku set` command (namely: `[<projects>]...`, `-s`, `-u` and `-r`) have been removed.
- Fixed errors with various representations of hash algorithm names. For example, "SHA_1", "SHA-1" and "SHA1" will all be recognised as "SHA-1".
- Improved docs and the readme. Mainly the "Configuring Pakku" chapter. Added a new "Server Management" chapter and a better explanation of types in the reference section.

### `pakku remote` Explanation

The `pakku remote` command can be used to install Pakku modpacks from secure remote Git repositories.

It is based on Git, but using this command doesn't require Git to be installed on your computer. This is because Pakku uses the Java implementation of Git named [JGit](https://git-scm.com/book/en/v2/Appendix-B:-Embedding-Git-in-your-Applications-JGit).

A remote modpack can only be installed in a directory with a non-initialized Pakku dev environment, meaning the directory must be empty or must contain empty directories only.

To install a remote modpack, use the `pakku remote` command with the `<url>` argument provided.  
Example usage: `pakku remote https://github.com/juraj-hrivnak/CaveGameUltimate.git`

`pakku remote` without any arguments will show you the status of the remote or show the help message when no remote is installed.  

To update your modpack from the remote modpack repository, run the `pakku remote update` subcommand.  

`pakku remote <url>` and the `update` subcommand can be used together for an autoupdating modpack installation.  
Example usage: `pakku remote https://github.com/juraj-hrivnak/CaveGameUltimate.git update` - This will install or update the modpack as needed.

Server installation can be achieved by using the `-S` or `--server-pack` flag when running the `pakku remote` command.

### Other

- The prefix "`!`" in glob pattern matching, which negates the pattern, will now exclude any matching file included by a **previous pattern**, and not a matching file included by any pattern, as it worked before.
- Refactored the `LockFile` to use the `kotlin-result` monad.
- Refactored action error messages to use the new `message()` function.
- Modrinth `MultipleProjectFiles` requests are now chunked every 300 IDs instead of 1000.
- Improved error handling for the export action.
- Improved and unified error handling when writing the lock file and the config file to disk.
- Implemented `OptionalExportRule`.
- Fixed a rare crash when converting `ProjectFile` to `CfModData` on exporting.

## v0.26.0

### Highlights

- Added mod loader and MC version changes to the diff, by @Wxrlds in [#48](https://github.com/juraj-hrivnak/Pakku/pull/48).
  - Added sections `# Minecraft` and `# Loaders` to the diff output.
  - Added `header-size` option to specify the size of the headers in the markdown.
- Improved already added & replacing project messages.
- Fixed support for CurseForge projects with 7-digit IDs; Fixes [#75](https://github.com/juraj-hrivnak/Pakku/issues/75).
- Fixed the building of Export Profiles when exporting multiple times.
- Created the [`CONTRIBUTING.md`](https://github.com/juraj-hrivnak/Pakku/blob/main/CONTRIBUTING.md).

### API

- Implemented `PakkuApi` entry point for initializing the Pakku API.
  - Example usage:
    ```kt
    pakku {
        curseForge(apiKey = "<your_custom_api_key>")
        withUserAgent("<your_custom_user_agent>")
    }
    ```
- Renamed `ExportingScope` to `ExportRuleScope` for better clarity.
- Implemented the `getProjectConfig` function. (Get "projects" configuration from the config file.)

### Tests

- Migrated to Strikt assertion library for more readable tests.

## v0.25.0

### Highlights

- Implemented the `pakku fetch --shelve` flag.
- Removed the `pakku sync` command's subpath auto-detection. (It did not work properly, and it's not worth rewriting it.)
- Fixed error when requesting GitHub projects generated from a template repository.

### `pakku fetch --shelve` Explanation

This feature was implemented to help revive old modpacks that use mods outside any platform.

When `pakku fetch --shelve` is used, Pakku will move unknown project files to a shelf instead of deleting them.
The shelf directory is located at `.pakku/shelf/`.

This feature can be used together with the `pakku sync` command in the following way:

- Run the `pakku -y sync` command to detect and add your local project files.
- Run the `pakku fetch --shelve` command to move any other project files outside any platform to the shelf.
- Decide what you want to do with each project file on the shelf.
  - You can move the project to the project overrides directory `.pakku/overrides/mods/`.
  - You can try to search for the project online. (For example, there is a chance that some of these files will be on GitHub.)

## v0.24.1

- Refactored `sync` command's subpath search algorithm to async.
- Fixed export profiles' `orElse` function not working as intended. (Optional integration with FileDirector did not work.)

## v0.24.0

### Highlights

- Fixed a major bug where projects were not being exported to modpack manifests.
- Implemented the `sync` command.
  - This command can be used to sync your modpack with local project files.
- Limited max number of retries for `fetch` command to 3 instead of 10.

### Other

- Made most of the commands print help on empty args and added a spinner progress bar to some of them.
- Projects will now be compared with lowercase and white space filtered project names and their project file hashes.
- Trailing commas are now allowed in the config file and lock file.
- Added a better error message to `ProjNotFound` errors.
- `.jar.meta` is now allowed extension when removing old files on `fetch`.
- Created tests for CurseForge and Modrinth modpack models.

## v0.23.0

- Fixed that updating GitHub projects was not working properly.

### Technical Notes

- Implemented DSL builder for export profiles.
- Implemented error severity and made the `AlreadyAdded` error only a notice.

## v0.22.0

### Highlights

- When removing a project, Pakku will always ask whether you want to remove each of its dependencies.
- Added glob pattern support for specifying overrides, server_overrides and client_overrides, in [#70](https://github.com/juraj-hrivnak/Pakku/pull/70).
  - This new implementation is optimized to work with large modpacks.
  - For more info see [v0.20.0](https://github.com/juraj-hrivnak/Pakku/releases/tag/v0.20.0).

### Fixes

- Removed hyperlink from exported modpack file path messages. They didn't work properly on all platforms.

### API

- Breaking change: Refactored action errors to separate data classes.
- Created a mini framework for simplifying tests.

## v0.21.0

- Improved projects updating, by @SettingDust in [#49](https://github.com/juraj-hrivnak/Pakku/pull/49).
  - Projects now prefer their current loaders of project files when updating.
- Added option the specify the project type when adding projects, by @SettingDust in [#54](https://github.com/juraj-hrivnak/Pakku/pull/54).
  - Fixed duplicated project files when updating.
- Updated _Exporting a Modpack_ docs page and improved the _Managing Projects_ docs page.

## v0.20.2

- Fixed an issue that caused project files from CurseForge to be outdated due to incorrect release date parsing.

## v0.20.1

- Reverted [`glob`](https://en.wikipedia.org/wiki/Glob_(programming)#Syntax) pattern matching support for overrides, due to performance limitations with large modpacks.
  - It will be reintroduced in the next releases.

## v0.20.0

### What's new?

- Implemented [`glob`](https://en.wikipedia.org/wiki/Glob_(programming)#Syntax) pattern support for specifying overrides. by @SettingDust in [#43](https://github.com/juraj-hrivnak/Pakku/pull/43), and @juraj-hrivnak.
  - An asterisk "`*`" matches anything except a slash. The character "`?`" matches any one character except "`/`". The range notation, e.g. `[a-zA-Z]`, can be used to match one of the characters in a range.
  - Two consecutive asterisks ("`**`") means match in all directories; `<directory>/**` means match everything inside the directory.
  - An optional prefix "`!`" negates the pattern; any matching file included by a previous pattern will become excluded again.
- Implemented retry logic for the `fetch` command.
  - Use `pakku fetch --retry` to retry downloading when it fails, with an optional number of times to retry. (Defaults to 2.)
- Added support for specifying a different version of an already added project using the `add` command.
- Added simple colored diff for showing changes to file names when using the `status` command.
- The newest file from all providers is now preferred when downloading, by @SettingDust in [#41](https://github.com/juraj-hrivnak/Pakku/pull/41).

## v0.19.0

### Highlights

- Added the `cfg` command for configuring properties of the config file, by @SettingDust in [#29](https://github.com/juraj-hrivnak/Pakku/pull/29).
  - This change deprecated the `pakku set [<projects>]` argument. Use the config file (`pakku.json`) or the `cfg prj` subcommand instead.
- Implemented configurable subpaths for projects and configurable paths for project types.
- Implemented configurable project aliases, by @SettingDust in [#34](https://github.com/juraj-hrivnak/Pakku/pull/34).
- Added support for data packs.
- Added better support for multi-loader modpacks, by @SettingDust in [#36](https://github.com/juraj-hrivnak/Pakku/pull/36) and [#37](https://github.com/juraj-hrivnak/Pakku/pull/37). (Sinytra Connector support.)
  - Project files are now also sorted in order of loaders in the lock file.
- Rewritten `fetch` command's deleting algorithm to account for subpaths.
  - Saves & screenshots are always excluded.
- Improved the UI of the `status` command.
  - Now it's much nicer and shows you changes to project files.

### Fixes

- Fixed ConcurrentModificationException on running `fetch` command in some cases.
- Fixed setting loaders using the `set` command when projects have unusual loaders.

#### Configurable Subpaths

There is now a new `subpath` property for projects that you configure in the [config file] or using the `cfg prj` subcommand.

Config file - syntax:

```json
{
  "projects": {
    "<project>": {
      "subpath": "<path>"
    }
  }
}
```

Config file - example usage:

```json
{
  "projects": {
    "sodium-extras": {
      "subpath": "optimize.client"
    },
    "iris": {
      "subpath": "decoration.client"
    }
  }
}
```

#### Configurable Paths for Project Types

There is now a variable for each project type that you configure in the [config file] or using the `cfg` command.

Config file - syntax:

```json
{
  "paths": {
    "mods": "<path>",
    "resource_packs": "<path>",
    "data_packs": "<path>",
    "worlds": "<path>",
    "shaders": "<path>"
  }
}
```

#### Project Aliases

There is now a new `aliases` property of type array for projects that you configure in the [config file] or using the `cfg prj` subcommand.

Config file - syntax:

```json
{
  "projects": {
    "<project>": {
      "aliases": ["<alias>"]
    }
  }
}
```

Config file - example usage:

```json
{
  "projects": {
    "forgified-fabric-api": {
      "aliases": ["fabric-api"]
    }
  }
}
```

### API

- Updated Clikt to `5.0.0` & Mordant to `3.0.0`.
- Improved docs generation with better subcommand support.

## v0.18.2

- Fixed error when GitHub license URL is `null`.

## v0.18.1

- Fixed tag parsing in GitHub project arguments.

## v0.18.0

### Highlights

Pakku now fully supports GitHub.

#### Adding GitHub projects

GitHub repositories with releases can be added as projects.
To add a GitHub project, run `pakku add {owner}/{repo}` or `pakku add https://github.com/{owner}/{repo}`.

You can also use the `prj` subcommand: `pakku add prj --gh {owner}/{repo}` or `pakku add prj --gh https://github.com/{owner}/{repo}`

Combining projects is also possible: `pakku add prj --mr greenery --gh juraj-hrivnak/Greenery`

To add a specific version (tag) of a GitHub project, run `pakku add {owner}/{repo}@{tag}`, `pakku add https://github.com/{owner}/{repo}/releases/tag/{tag}` or `pakku add https://github.com/{owner}/{repo}/tree/{tag}`.

#### Updating

GitHub projects can be updated using the `pakku update` command.
They are also recognized in other commands as expected.

#### Exporting

For CurseForge, GitHub projects are added to the `overrides`.
For Modrinth, GitHub projects are added to the `modrinth.index.json` also with generated `sha512` and `sha1` hashes.

The project side of GitHub projects defaults to `BOTH.` If you need to change this, do so in `pakku.json`.
Pakku determines whether the GitHub project is redistributable based on its licence's spdx code. No licence means `ARR`.

#### UI

GitHub projects are displayed in the format: `gh={owner}/{repo}`

![gh_support](https://github.com/user-attachments/assets/3d3e81c0-f764-4679-bdf8-71cc4107a643)

### Other Changes

- Project types can now be overridden in the config file (`pakku.json`).
- Deprecated `set` command's `-s`,`--side` and `-r`, `--redistributable` options. Use the config file (`pakku.json`) instead.

### Technical Notes

- Many functions now support `IProjectProvider`s instead of `Platform`s only.
- The lock file is now sorted by project names instead of slugs.
- Implemented the `ProjectArg` monad to better handle project additions.
  - Its `fold()` function can be used to map the possible arg types:

    ```kt
    arg.fold(
        commonArg = { },
        gitHubArg = { }
    )
    ```

- Integrity checking will now allow projects without hashes (GitHub) and will warn the user instead.

## v0.17.1

- Fixed `add` command not being invoked without a subcommand. ([#22](https://github.com/juraj-hrivnak/Pakku/issues/22))
- Updated to the latest snapshot of the Clikt library.

## v0.17.0

- Added the `prj` subcommand for the `add` command.
  - With this subcommand you can specify the project
  you want to add precisely, using the `--cf` and/or `--mr` options.
  - This subcommand can be used multiple times.
  - Example usage: `pakku add prj --mr iris --cf irisshaders`
  - Note that the `add` command functionality remains the same.
- Added descriptions to command arguments.

## v0.16.0

- When adding projects, slugs are now more robustly resolved.
  - Project additions are now stricter. Pakku now doesn't allow you to add projects without any files.
  - Fixed [#15](https://github.com/juraj-hrivnak/Pakku/issues/15).
- Added backup handling when writing to files.
  When an error occurs during writing to a file a backup is restored.
- Pakku now shows you the project type alongside the slugs in all messages.

## v0.15.1

- Fixed modpack `@var@` replacements not working.
- URLs in FileDirector's config are now encoded. This is a workaround for
[FileDirector#19](https://github.com/TerraFirmaCraft-The-Final-Frontier/FileDirector/issues/19).

## v0.15.0

- Refactored deleting old files in `fetch` command. (Improved performance.)
- The `status` command now also shows you the Minecraft version, loaders and author.
- Removed `cache4k` dependency.

## v0.14.3

- Fixed that resource and shader packs were exported to the server pack when their project side was missing.

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

[config file]: https://juraj-hrivnak.github.io/Pakku/
