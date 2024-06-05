# Command Template
{is-library="true"}

<snippet id="template-cmd">
    <tabs group="cmd">
        <var name="help"></var>
        <tab title="Default" group-key="default">
            <code-block prompt="$">%pakku% %cmd% %help%</code-block>
        </tab>
        <tab title="Using Java" group-key="java">
            <code-block prompt="$">%pakku-jvm% %cmd% %help%</code-block>
        </tab>
    </tabs>
</snippet>
