# pakku cfg

Configure properties of the config file

## Usage

<snippet id="snippet-cmd">

<var name="cmd">cfg</var>
<var name="params">[&lt;options&gt;] </var>
<include from="_template_cmd.md" element-id="template-cmd"/>

</snippet>


## Subcommands

[`prj`](pakku-cfg-prj.md)
: Configure projects

## Options

<snippet id="snippet-options-all">

<snippet id="snippet-options">

`-n`, `--name`
: Change the name of the modpack

`-v`, `--version`
: Change the version of the modpack

`-d`, `--description`
: Change the description of the modpack

`-a`, `--author`
: Change the author of the modpack

`--mods-path`
: Change the path for the `MOD` project type

`--resource-packs-path`
: Change the path for the `RESOURCE_PACK` project type

`--data-packs-path`
: Change the path for the `DATA_PACK` project type

`--worlds-path`
: Change the path for the `WORLD` project type

`--shaders-path`
: Change the path for the `SHADER` project type

</snippet>

`-h`, `--help`
: Show the help message and exit

</snippet>
