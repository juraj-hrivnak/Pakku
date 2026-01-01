# pakku sync

Sync your modpack with local project files

## Usage

<snippet id="snippet-cmd">

<var name="cmd">sync</var>
<var name="params">[&lt;options&gt;] </var>
<include from="_template_cmd.md" element-id="template-cmd"/>

</snippet>

## Options

<snippet id="snippet-options-all">

<snippet id="snippet-options">

`-A`, `--additions`
: Sync additions only

`-R`, `--removals`
: Sync removals only

`-U`, `--updates`
: Sync updates only

`-f`, `--from-parent`
: Sync from parent modpack instead of local files

`--prefer-upstream`
: Automatically prefer upstream version in conflicts (use with `--from-parent`)

`--prefer-local`
: Automatically prefer local version in conflicts (use with `--from-parent`)

</snippet>

`-h`, `--help`
: Show the help message and exit

</snippet>
