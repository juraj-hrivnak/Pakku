# Forking a Modpack

Pakku can fork a modpack from a Git repository.

A fork keeps the parent repository in `.pakku/parent`. The parent checkout is
ignored by Git and is treated as read-only modpack input. Your local
`pakku.json` and `pakku-lock.json` remain the fork layer where you add projects,
override project properties, or exclude parent projects.

## Initialize a Fork

To fork from a Git URL:

```shell
pakku fork init --git-url https://example.com/owner/modpack.git --ref-name main
```

To fork from an existing local clone:

```shell
pakku fork init --from-path ../modpack --ref-name main
```

When using `--from-path`, Pakku requires the source repository to be clean. The
parent is cloned from the local path, but `.pakku/parent` keeps the upstream
remote URL from the source repository.

## Sync the Parent

```shell
pakku fork sync
```

Sync updates `.pakku/parent` to the configured ref, stores the latest parent
commit in `pakku.json`, and records SHA-256 hashes for the parent config and
lock file.

## Export a Fork

Use the normal export command:

```shell
pakku export
```

During export, Pakku starts with the parent lock file and then applies the local
fork layer:

- Local projects with the same slug replace parent projects.
- Local project config entries can override parent project properties.
- Projects listed in `excludes` are omitted from the exported pack.
- Additional local projects are appended to the merged lock file.

If the parent lock file hash differs from the hash stored during the last sync,
Pakku warns before exporting. Run `pakku fork sync` to verify the parent again.

## Managing Parent Projects

Promote a parent project into the local lock file so it becomes locally managed:

```shell
pakku fork promote sodium
```

Exclude parent projects from exports:

```shell
pakku fork exclude sodium lithium
```

Re-include excluded parent projects:

```shell
pakku fork include sodium
```

Show the current fork configuration:

```shell
pakku fork show
```

Remove the fork metadata and parent checkout:

```shell
pakku fork unset
```
