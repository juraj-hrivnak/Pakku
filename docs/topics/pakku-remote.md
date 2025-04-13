# pakku remote

Install modpack from a Git URL

## Usage

<snippet id="snippet-cmd">

<var name="cmd">remote</var>
<var name="params">[&lt;options&gt;] [&lt;url&gt;]</var>
<include from="_template_cmd.md" element-id="template-cmd"/>

</snippet>


## Subcommands

[`update`](pakku-remote-update.md)
: Update the modpack from its remote

## Arguments

<snippet id="snippet-args">

`[<url>]`
: URL of the remote Git repository to install

</snippet>

## Options

<snippet id="snippet-options-all">

<snippet id="snippet-options">

`-b`, `--branch`
: Checkout <branch> instead of the remote's HEAD

`-r`, `--retry`
: Retries downloading when it fails, with optional number of times to retry (Defaults to 2)

`-S`, `--server-pack`
: Install the server pack

`--rm`, `--remove`
: Remove the remote from this modpack

</snippet>

`-h`, `--help`
: Show the help message and exit

</snippet>
