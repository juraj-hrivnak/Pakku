# CLI Config

CLI Config (`cli-config.json`) is an optional file
which can be used to modify the UI aspects of the Pakku CLI.

The `cli-config.json` must be located in the [Pakku directory (`.pakku`)](Pakku-Directory.md).
The relative path from the `.minecraft` directory should be: `.pakku/cli-config.json`.

## Properties

`theme`
: Can be `default` or `ascii`. (Defaults to `default` 😜.)
: - The `default` theme uses [UTF-8](https://en.wikipedia.org/wiki/UTF-8) symbols
which may not be properly rendered in some terminals.
: - The `ascii` theme limits the characters to [ASCII](https://en.wikipedia.org/wiki/ASCII) only.

`ansi_level`
: Can be `none`, `ansi16`, `ansi256` or `truecolor`.
(By default it is automatically detected based on your terminal/CMD.)
