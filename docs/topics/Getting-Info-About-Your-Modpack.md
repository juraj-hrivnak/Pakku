# Getting Info About Your Modpack

The basic operations to get info about your modpack include:

- [Checking the status](#checking-the-status) of your modpack
(Which projects have a new version available)
- [Listing projects](#listing-projects)
- [Differentiating between versions](#differentiating-between-versions) of your modpack (Useful for changelogs)

## Checking the Status

To check the status, run the [`pakku status`](pakku-status.md) command:

<include from="pakku-status.md" element-id="snippet-cmd"/>

It will tell you basic information about your modpack in a nice and readable way,
and also check for updates.

## Listing Projects

To list projects in your modpack, run the [`pakku ls`](pakku-ls.md) command:

<include from="pakku-ls.md" element-id="snippet-cmd"></include>

The output contains the following info:

<table>

<tr>
    <td>Type of info</td>
    <td>Description</td>
</tr>

<tr>
    <td>Number of dependencies</td>
    <td>Empty indicates no dependencies.</td>
</tr>

<tr>
    <td>Slugs</td>
    <td>
    Slugs are formated as <code>key=value</code> pairs,
    where the <code>key</code> is the short form of
    the platform name, and the <code>value</code> is the project's slug.
    The short forms of the platform names indicate
    that project is available on the platform.
    These short forms are also clickable
    and contain a link to the project's website.
    </td>
</tr>

<tr>
    <td>
    Update strategy (only with <code>-c</code>,
    <code>--check-updates</code> flag used)
    </td>
    <td>
    <format color="Blue">Blue colored ^</format> 
    indicates that a new version of the project is available.
    <format color="LightGreen">Green colored ^</format> 
    indicates that the project is up-to-date.
    <format color="Red">Red colored ✖^</format>
    indicates that the project will not be updated.
    </td>
</tr>

<tr>
    <td>Project name</td>
    <td>
    <format color="Red">Red color</format> indicates that it has no files.
    <format style="bold">⚠ A waring sign</format> before the project name
    indicates that the project is not redistributable.
    Use the <code>--name-max-length</code> option
    to set the max length of project names. (Default 20)
    </td>
</tr>

<tr>
    <td>Project Type</td>
    <td>Tells you whether the project is a mod, resource pack or etc.</td>
</tr>

<tr>
    <td>Project Side</td>
    <td>
    Can be SERVER, CLIENT, BOTH or empty,
    which indicates that the project side is unknown.
    </td>
</tr>

</table>

Example output (with `--check-updates` flag used):

<img src="screenshot_ls.png" alt="Listing Projects"/>

## Differentiating Between versions

To diff version of your modpack, run the [`pakku diff`](pakku-diff.md) command:

<include from="pakku-diff.md" element-id="snippet-cmd"/>

Example:

```Bash
%pakku% diff old-pakku-lock.json current-pakku-lock.json
```
{prompt="$"}

To create a diff usable for changelogs written in markdown, consider using the options:

<include from="pakku-diff.md" element-id="snippet-options"/>
