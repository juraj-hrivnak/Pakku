# pakku remote

Create and install modpacks from remote

## Usage

<snippet id="snippet-cmd">

<var name="cmd">remote</var>
<var name="params">[&lt;options&gt;] [&lt;url&gt;]</var>
<include from="_template_cmd.md" element-id="template-cmd"/>

</snippet>


## Subcommands

[`update`](pakku-remote-update.md)
: update the modpack from its remote

[`rm`](pakku-remote-rm.md)
: remove the remote from this modpack

## Arguments

<snippet id="snippet-args">

`[<url>]`
: URL of the remote package or Git repository

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

</snippet>

`-h`, `--help`
: Show the help message and exit

</snippet>
