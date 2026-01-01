# Managing Forks

Pakku supports fork management, allowing you to maintain a custom variant of a
base modpack while tracking upstream changes. This is useful when you want to
add local-only mods without the overhead of git rebasing.

## Setting Up a Fork

To create a fork of an existing modpack, use the
[`pakku parent`](pakku-parent.md) command:

<var name="params">set </var>
<var name="arg">modrinth:base-modpack</var>
<include from="pakku-parent.md" element-id="snippet-cmd"/>

This configures your modpack as a fork that tracks the specified parent modpack.

## Version Tracking

By default, your fork tracks the **latest version** of the parent modpack. When
you sync from parent, you'll automatically get the newest upstream changes.

To pin to a specific version, edit the `pakku.json` config file and set the
`version` field:

```json
{
  "parent": {
    "type": "modrinth",
    "id": "base-modpack",
    "version": "1.2.3"
  }
}
```

Set `version: null` to resume tracking the latest version.

## Adding Local-Only Projects

To add mods that are unique to your fork (not from the parent):

<var name="params">--local-only </var>
<var name="arg">my-custom-mod</var>
<include from="pakku-add.md" element-id="snippet-cmd"/>

Local-only projects are never modified or removed when syncing from the parent.

## Syncing From Parent

To pull upstream changes from the parent modpack:

<var name="params">--from-parent</var>
<var name="arg"></var>
<include from="pakku-sync.md" element-id="snippet-cmd"/>

This will:

- Add new projects from the parent (marked as `UPSTREAM` origin)
- Update existing upstream projects to match parent versions
- Remove projects that were deleted from parent
- **Preserve** all local-only projects

### Handling Conflicts

If the parent adds a project that you already have as local-only, Pakku will
detect the conflict and prompt you to choose:

1. **Keep local version** - Marks the project as local-only and ignores parent
   version
2. **Use upstream version** - Replaces local with parent version and tracks it
   as upstream
3. **Skip** - Leaves project unchanged for this sync

For automation, use flags:

- `--prefer-local` - Automatically keep local versions
- `--prefer-upstream` - Automatically use parent versions

## Viewing Fork Status

To see your fork configuration and project breakdown:

<var name="params">show</var>
<var name="arg"></var>
<include from="pakku-parent.md" element-id="snippet-cmd"/>

Or check overall status:

<var name="params"></var>
<var name="arg"></var>
<include from="pakku-status.md" element-id="snippet-cmd"/>

This shows:

- Parent modpack information
- Number of upstream projects (from parent)
- Number of local-only projects (your additions)
- Total project count

## Managing Local-Only Status

To mark an existing project as local-only:

<var name="params">--local-only </var>
<var name="arg">project-slug</var>
<include from="pakku-set.md" element-id="snippet-cmd"/>

To remove local-only marking:

<var name="params">--no-local-only </var>
<var name="arg">project-slug</var>
<include from="pakku-set.md" element-id="snippet-cmd"/>

## Filtering Projects

List only local-only projects:

<var name="params">--local-only</var>
<var name="arg"></var>
<include from="pakku-ls.md" element-id="snippet-cmd"/>

Filter by origin:

<var name="params">--origin upstream</var>
<var name="arg"></var>
<include from="pakku-ls.md" element-id="snippet-cmd"/>

Valid origins: `upstream`, `local`, `external`

## Removing Fork Configuration

To stop tracking the parent modpack:

<var name="params">unset</var>
<var name="arg"></var>
<include from="pakku-parent.md" element-id="snippet-cmd"/>

This **removes all upstream projects** by default. To keep them as local
projects:

<var name="params">unset --keep-projects</var>
<var name="arg"></var>
<include from="pakku-parent.md" element-id="snippet-cmd"/>

## Practical Example: Managing Your Personal Modpack

Here's a complete workflow for managing a personal modpack that extends a base
modpack with your own mods and resourcepacks.

### Initial Setup

Imagine you want to create "My Awesome Modpack" based on "Valhelsia Enhanced"
but with your own additions:

```bash
# Set up the fork relationship
pakku parent set modrinth:valhelsia-enhanced

# Add your extra mods (these are YOUR additions)
pakku add sodium --local-only
pakku add create --local-only
pakku add iris-shaders --local-only

# Add a resourcepack
pakku add faithful-32x --local-only

# Check your setup
pakku status
# Output:
# Parent: modrinth:valhelsia-enhanced (latest: 3.1.0)
# Upstream projects: 47
# Local-only projects: 4
# Total: 51
```

### Daily Workflow

When the parent modpack updates:

```bash
# Check for updates
pakku parent show

# Sync upstream changes
pakku sync --from-parent
# If upstream adds a mod you already have locally, you'll be prompted:
# - Keep your local version (recommended for your custom mods)
# - Use upstream version (if you prefer parent's choice)
# - Skip (if you want to decide later)

# Review what changed
pakku ls --local-only
# Shows your personal additions: sodium, create, iris-shaders, faithful-32x
```

### Managing Your Local-Only Mods

If you decide one of your local mods is no longer needed:

```bash
# Remove from your modpack entirely
pakku rm sodium

# Or remove the local-only marking (treated as upstream going forward)
pakku set sodium --no-local-only
```

### Publishing Your Fork

When you want to share your modpack with friends:

```bash
# Export as a standalone modpack (includes both upstream + your mods)
pakku export --name "My Awesome Modpack"

# The resulting zip file is complete - friends don't need the parent
```

### Switching to a Different Base

If you want to change to a different parent modpack:

```bash
# Option 1: Start fresh with new parent
pakku parent unset
pakku parent set modrinth:new-base-modpack

# Option 2: Keep all current projects when changing parent
pakku parent unset --keep-projects
pakku parent set modrinth:new-base-modpack
```

## Exporting Forks

When you export your fork, **all projects are included** (both upstream and
local-only). The exported modpack is a complete, standalone modpack.

<var name="params"></var>
<var name="arg"></var>
<include from="pakku-export.md" element-id="snippet-cmd"/>

The fork relationship is only relevant for development and is not preserved in
exports.
