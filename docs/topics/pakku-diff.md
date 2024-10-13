# pakku diff

Diff projects in modpack

## Usage

<snippet id="snippet-cmd">

<var name="cmd">diff</var>
<var name="params">[&lt;options&gt;] &lt;old-lock-file&gt; &lt;current-lock-file&gt;</var>
<include from="_template_cmd.md" element-id="template-cmd"/>

</snippet>

## Arguments

<snippet id="snippet-args">

`<old-lock-file>`
: The `old-lock-file` argument

`<current-lock-file>`
: The `current-lock-file` argument

</snippet>

## Options

<snippet id="snippet-options-all">

<snippet id="snippet-options">

`--markdown-diff`
: Export a `.md` file formatted as a diff code block

`--markdown`
: Export a `.md` file formatted as regular markdown

`-v`, `--verbose`
: Gives detailed information on which mods were updated

</snippet>

`-h`, `--help`
: Show the help message and exit

</snippet>
