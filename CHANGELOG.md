# Changelog

## Unreleased

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
