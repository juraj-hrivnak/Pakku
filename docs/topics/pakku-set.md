# pakku set

Set properties of the lock file

## Usage

<snippet id="snippet-cmd">

<var name="cmd">set</var>
<var name="params">[&lt;options&gt;] [&lt;projects&gt;]...</var>
<include from="_template_cmd.md" element-id="template-cmd"/>

</snippet>

## Arguments

<snippet id="snippet-args">

`[<projects>]...`
: Use the config file (pakku.json) or 'cfg' command instead.

</snippet>

## Options

<snippet id="snippet-options-all">

<snippet id="snippet-options">

`-s`, `--side`
: Change the side of a project

`-u`, `--update-strategy`
: Change the update strategy of a project

`-r`, `--redistributable`
: Change whether the project can be redistributed

`-t`, `--target`
: Change the target of the pack

`-v`, `--mc-versions`
: Change the minecraft versions

`-l`, `--loaders`
: Change the mod loaders

</snippet>

`-h`, `--help`
: Show the help message and exit

</snippet>
