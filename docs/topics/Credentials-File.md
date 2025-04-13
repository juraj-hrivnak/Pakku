# Credentials File

Pakku stores sensitive credential information that you specify with [`pakku credentials`](pakku-credentials.md),
[`pakku init`](pakku-init.md) or when requested, in a local file named `credentials`,
in a directory named `.pakku` in your [home directory](https://en.wikipedia.org/wiki/Home_directory).

To delete all your credentials, run the [`pakku credentials`](pakku-credentials.md) command
with the `--delete` flag:

<var name="params">--delete </var>
<include from="pakku-credentials.md" element-id="snippet-cmd"/>
