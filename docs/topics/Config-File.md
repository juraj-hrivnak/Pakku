# Config File

A config file (`pakku.json`) is a file used by the user to configure properties needed for
modpack export.

## Properties

`name`
: The name of the modpack.

`version`
: The version of the modpack.

`description`
: Description of the modpack.

`author`
: The author of the modpack.

`overrides`
: A list of [overrides](Pakku-Terminology.md#override) packed up with the modpack.

`server_overrides`
: A list of [server overrides](Pakku-Terminology.md#override) packed up with the modpack.

`client_overrides`
: A list of [client overrides](Pakku-Terminology.md#override) packed up with the modpack.

`projects`
: A list of project slugs, names, IDs or filenames from the [lock file](Lock-File.md)
with properties you want to change.
