# Command Template
{is-library="true"}

<snippet id="template-cmd">
    <tabs group="cmd">
        <var name="params"></var>
        <var name="arg"></var>
        <tab title="Default" group-key="default">
            <code-block prompt="$">%pakku% %cmd% %params%%arg%</code-block>
        </tab>
        <tab title="Using Java" group-key="java">
            <code-block prompt="$">%pakku-jvm% %cmd% %params%%arg%</code-block>
        </tab>
    </tabs>
</snippet>
