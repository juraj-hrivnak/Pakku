# CLI Config File

The CLI config file (`cli-config.json`) can be used to modify the UI aspects of the Pakku CLI.

The `cli-config.json` must be located in the [Pakku directory (`.pakku`)](Pakku-Directory.md).
<br>The relative path from your modpack directory must be: `.pakku/cli-config.json`.

## Properties

<deflist type="narrow">
    <def>
        <title><code>theme</code></title>
        <p>
            (By default it is automatically detected based on your terminal/CMD.)
        </p>
        <table>
            <tr>
                <td>Possible value</td>
                <td>Description</td>
            </tr>
            <tr>
                <td><code>default</code></td>
                <td>
                    Uses <a href="https://en.wikipedia.org/wiki/UTF-8">UTF-8</a>
                    characters for a better experience in the CLI. 
                    <note>
                        <a href="https://en.wikipedia.org/wiki/UTF-8">UTF-8</a>
                        characters may not render properly in some terminals.
                    </note>
                </td>
            </tr>
            <tr>
                <td><code>ascii</code></td>
                <td>
                    Limits characters to <a href="https://en.wikipedia.org/wiki/ASCII">ASCII</a> only.
                </td>
            </tr>
        </table>
    </def>
    <def>
        <title><code>ansi_level</code></title>
        <p>
            (By default it is automatically detected based on your terminal/CMD.)
        </p>
        <table>
            <tr>
                <td>Possible values</td>
            </tr>
            <tr>
                <td><code>none</code>, <code>ansi16</code>, <code>ansi256</code> or <code>truecolor</code></td>
            </tr>
        </table>
    </def>
</deflist>