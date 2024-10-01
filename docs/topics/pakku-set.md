# pakku set

Set various properties of your modpack or projects

## Usage

<snippet id="snippet-cmd">

<var name="cmd">set</var>
<var name="params">[&lt;options&gt;] [&lt;projects&gt;]...</var>
<include from="_template_cmd.md" element-id="template-cmd"/>

</snippet>

## Arguments

<snippet id="snippet-args">

`[<projects>]...`
: The `projects` argument. **Deprecated, use [cfg](pakku-cfg.md) command or config file instead**

</snippet>

## Options

<snippet id="snippet-options-all">

<snippet id="snippet-options">

`-s`, `--side`
: Change the side of a project. **Deprecated, use [cfg](pakku-cfg.md) command or config file instead**

`-u`, `--update-strategy`
: Change the update strategy of a project. **Deprecated, use [cfg](pakku-cfg.md) command or config file instead**

`-r`, `--redistributable`
: Change whether the project can be redistributed. **Deprecated, use [cfg](pakku-cfg.md) command or config file instead**

`-t`, `--target`
: Change the target of the pack

`-v`, `--mc-versions`
: Change the minecraft versions

`-l`, `--loaders`
: Change the mod loaders

</snippet>

`-h`, `--help`
: Show this message and exit

</snippet>
