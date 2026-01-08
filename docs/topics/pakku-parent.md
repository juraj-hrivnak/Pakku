# pakku parent

Manage parent modpack configuration for fork management

## Usage

<snippet id="snippet-cmd">

<var name="cmd">parent</var>
<var name="params">[&lt;options&gt;] </var>
<include from="_template_cmd.md" element-id="template-cmd"/>

</snippet>

## Subcommands

`set`
: Set the parent modpack

`show`
: Show the current parent modpack configuration

`unset`
: Remove the parent modpack configuration

## Options

<snippet id="snippet-options-all">

<snippet id="snippet-options">

`-k`, `--keep-projects`
: Keep all projects when unsetting parent (converts upstream projects to local)

</snippet>

`-h`, `--help`
: Show the help message and exit

</snippet>

## Examples

### Setting a Parent Modpack

To set a parent modpack from Modrinth:

<var name="params">set </var>
<var name="arg">modrinth:my-base-modpack</var>
<include from="pakku-parent.md" element-id="snippet-cmd"/>

To set a parent from CurseForge:

<var name="params">set </var>
<var name="arg">curseforge:12345</var>
<include from="pakku-parent.md" element-id="snippet-cmd"/>

### Showing Parent Configuration

<var name="params">show</var>
<var name="arg"></var>
<include from="pakku-parent.md" element-id="snippet-cmd"/>

This displays:
- Parent modpack type and ID
- Version tracking status (latest or pinned)
- Number of upstream projects
- Number of local-only projects

### Unsetting Parent

To remove parent configuration and delete all upstream projects:

<var name="params">unset</var>
<var name="arg"></var>
<include from="pakku-parent.md" element-id="snippet-cmd"/>

To remove parent but keep all projects as local:

<var name="params">unset --keep-projects</var>
<var name="arg"></var>
<include from="pakku-parent.md" element-id="snippet-cmd"/>
