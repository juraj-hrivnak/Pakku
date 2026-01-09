# pakku export

Export modpack

## Usage

<snippet id="snippet-cmd">

<var name="cmd">export</var>
<var name="params">[&lt;options&gt;] </var>
<include from="_template_cmd.md" element-id="template-cmd"/>

</snippet>

## Options

<snippet id="snippet-options-all">

<snippet id="snippet-options">

`--show-io-errors`
: Show file IO error on exporting. (These can be ignored most of the time.)

`--no-server`
: Export modpack without server content. Modrinth: exclude server-overrides and SERVER mods; ServerPack: skip export.

</snippet>

`-h`, `--help`
: Show the help message and exit

</snippet>
