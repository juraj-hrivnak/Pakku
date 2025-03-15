# Changing Project&#39;s Update Strategy (Version Pinning)

You can stop the project from updating by changing the _update strategy_ to `NONE`.

To do that, you can use the [`pakku cfg prj`](pakku-cfg-prj.md) command:

<var name="params">&lt;projects&gt;... -u none</var>
<include from="pakku-cfg-prj.md" element-id="snippet-cmd"/>

Or you can change it manually using the [config file](Config-File.md):

```json
{
    "projects": {
        "<project>": {
            "update_strategy": "NONE"
        }
    }
}
```