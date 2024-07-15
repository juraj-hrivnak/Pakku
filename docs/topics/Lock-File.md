# Lock File

A lock file (`pakku-lock.json`) is an automatically generated file
used by Pakku to define all [properties](#properties) of a modpack needed for its development.

This file is not intended to be modified manually.

## Properties

`target`
: The targeted platform of the modpack.

`mc_versions`
: The Minecraft versions supported by the modpack.

`loaders`
: The mod loaders supported by the modpack.

`projects`
: A list of projects included in the modpack.

`lockfile_version`
: The version of the LockFile.
