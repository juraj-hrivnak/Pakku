# Managing Projects

On this page, you will learn how to:

- [Add projects](#adding-projects)
- [Remove projects](#removing-projects)
- [Update projects](#updating-projects)

## Adding Projects

To add a project, you can use the [`pakku add`](pakku-add.md) command:

<include from="pakku-add.md" element-id="snippet-cmd"/>

The `<projects>...` argument _accepts multiple_ `<project>` arguments which can be:
  - the CurseForge project slug or ID; 
    - optionally: `<project>:<file_id>` for specifying the file ID.
  - the Modrinth project slug or ID;
    - optionally: `<project>:<version_id>` for specifying the version ID.
  - the GitHub `<owner>/<repo>` or repository URL; 
    - optionally: `<owner>/<repo>@<tag>` for specifying the tag.

For more precision, consider using the [`pakku add prj`](pakku-add-prj.md) subcommand:

<include from="pakku-add-prj.md" element-id="snippet-cmd"/>

With its options:

<include from="pakku-add-prj.md" element-id="snippet-options"/>

### Examples

<procedure title="Using Project Slugs" type="choices">

<step>

For CurseForge or Modrinth:

<var name="params"></var>
<var name="arg">jei</var>
<include from="pakku-add.md" element-id="snippet-cmd"/>

</step>
<step>

For GitHub:

<var name="params"></var>
<var name="arg">CaffeineMC/sodium</var>
<include from="pakku-add.md" element-id="snippet-cmd"/>

</step>
<step>

For complicated slugs:

<var name="params"></var>
<var name="arg">--cf ferritecore --mr ferrite-core</var>
<include from="pakku-add-prj.md" element-id="snippet-cmd"/>

</step>
</procedure>

<procedure title="Using Project IDs" type="choices">
<step>

For CurseForge:

<var name="params"></var>
<var name="arg">238222</var>
<include from="pakku-add.md" element-id="snippet-cmd"/>

</step>
<step>

For Modrinth:

<var name="params"></var>
<var name="arg">u6dRKJwZ</var>
<include from="pakku-add.md" element-id="snippet-cmd"/>

</step>
</procedure>

<procedure title="With Project File IDs Specified" type="choices">
<step>

For CurseForge:

<var name="params"></var>
<var name="arg">jei:5101366</var>
<include from="pakku-add.md" element-id="snippet-cmd"/>

</step>
<step>

For Modrinth:

<var name="params"></var>
<var name="arg">jei:PeYsGsQy</var>
<include from="pakku-add.md" element-id="snippet-cmd"/>

</step>
<step>

For GitHub:

<var name="params"></var>
<var name="arg">CaffeineMC/sodium@mc1.20.1-0.5.11</var>
<include from="pakku-add.md" element-id="snippet-cmd"/>

</step>
</procedure>

## Removing Projects

To remove a project, run the [`pakku rm`](pakku-rm.md) command:

<include from="pakku-rm.md" element-id="snippet-cmd"/>

The `[<projects>...]` argument accepts only the project slug, name or ID
and allows multiple projects to be removed.

```
%pakku% rm jei
%pakku% rm "Just Enough Items"
%pakku% rm 238222
%pakku% rm jei terrafirmacraft
```

To remove all projects from the modpack, use the `--all` flag:

```
%pakku% rm -a
%pakku% rm --all
```

<warning>
Warning: This is a dangerous operation and therefore
you will be asked whether you really want to proceed.
</warning>

## Updating Projects

To update a project, run the [`pakku update`](pakku-update.md) command:

<include from="pakku-update.md" element-id="snippet-cmd"/>

The `[<projects>...]` argument accepts only the project slug, name or ID
and allows multiple projects to be updated.

```
%pakku% update jei
%pakku% update "Just Enough Items"
%pakku% update 238222
%pakku% update jei terrafirmacraft
```

To update all projects in the modpack, use the `--all` flag:

```
%pakku% update -a
%pakku% update --all
```

<seealso style="cards">
   <category ref="related">
       <a href="Developing-a-Modpack.md"/>
   </category>
</seealso>
