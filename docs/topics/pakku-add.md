# pakku add

Add projects

## Usage

<snippet id="snippet-cmd">

<var name="cmd">add</var>
<var name="params">[&lt;options&gt;] [&lt;projects&gt;]... &lt;command&gt; [&lt;args&gt;]...</var>
<include from="_template_cmd.md" element-id="template-cmd"/>

</snippet>

## Arguments

<snippet id="snippet-args">

`[<projects>]...`
: Projects to add

</snippet>

## Options

<snippet id="snippet-options-all">
<snippet id="snippet-options">
    <deflist>
    <def>
    <title><code>-D</code>, <code>--no-deps</code></title><p>Ignore resolving dependencies</p>
    </def>
    </deflist>
</snippet>
    <deflist>
    <def>
    <title><code>-h</code>, <code>--help</code></title><p>Show this message and exit</p>
    </def>
    </deflist>
</snippet>

## Subcommands

[`prj`](pakku-add-prj.md)
: Specify the project precisely
