# Getting Info About Your Modpack

The basic operations to get info about your modpack include:

- [Listing projects](#listing-projects)
- [Checking the status](#checking-the-status) of your modpack
(Which projects have a new version available)
- [Differentiating between versions](#differentiating-between-versions) of your modpack (Useful for changelogs)

## Listing Projects

<note>
Note: The style and the info included while listing projects is subject to change.
</note>

To list projects in your modpack, run the [`pakku ls`](pakku-ls.md) command:

<include from="pakku-ls.md" element-id="snippet-cmd"></include>

The output contains the following info:

<table>
<tr><td>Type of info</td><td>Description</td></tr>

<tr><td>Number of dependencies</td>
<td>Empty indicates no dependencies.</td></tr>

<tr><td>Platforms</td>
<td><format color="LightGreen">Green color</format> 
indicates that project is available on the platform, 
<format color="Red">red color</format> indicates that it is not.
It is also clickable and contains a hyperlink.</td></tr>

<tr><td>Update strategy</td>
<td><format color="Blue">Blue colored ^</format> 
indicates that a new version of the project is available.
<format color="LightGreen">Green colored ^</format> 
indicates that the project is up-to-date.
<format color="Red">Red colored ✖^</format> indicates that the project will not be updated.</td></tr>

<tr><td>Project name</td>
<td><format color="Red">Red color</format> indicates that it has no files.
<format style="bold">⚠ A waring sign</format> before the project name
indicates that the project is not redistributable.</td></tr>
</table>

Example output:

```Bash
%pakku% ls

        | cf mr | ^Farmer's Delight
        | cf mr | ^Just Enough Items (JEI)
        | cf mr | ^Patchouli
 1 dep  | cf mr | ^TerraFirmaCraft

Projects total: 4

```
{prompt="$"}

## Checking the Status

Not implemented yet! Will supersede [`pakku ls`](pakku-ls.md) with its functionality.

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
