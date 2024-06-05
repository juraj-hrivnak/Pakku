# Changelog

## Unreleased

## v0.10.0

### What's new?

- Implemented new UI for most of the CLI.
  - Slugs now contain hyperlinks. The `list` command contains type and side information for projects.
- Implemented the `status` command as a better way to check for updates than the `ls -c` command.
- Improved `rm` command with better "autocomplete" for slugs.
- Improved importing - now supports importing without manually setting Minecraft
versions and loaders.
- Fixed issue with the `fetch` command not properly listing platforms.

### Docs
- Implemented auto generation for CLI Commands reference for docs.
- Added option to switch between Java and PATH examples in docs.

### API
- Refactored models
