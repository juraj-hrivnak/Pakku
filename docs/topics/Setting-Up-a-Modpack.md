# Setting Up a Modpack

To set up a modpack, you can:

- Start form scratch and [create a new modpack](#creating_a_new_modpack).
- [Import an existing modpack](#importing_an_existing_modpack).

<procedure title="Creating a new modpack" id="creating_a_new_modpack" type="choices">
<step>

To create a new modpack, run the [`pakku init`](pakku-init.md) command:

```
./pakku init
```
{prompt="$"}

</step>
<step>

This will ask you to enter:

1. The name of the modpack.
2. The Minecraft versions supported by the modpack.
3. The mod loaders and their versions supported by the modpack.
4. The targeted platform of the modpack.

I will also set the version of the modpack to `0.0.1` and add `config` folder to the overrides.

</step>
<step>

If you made a mistake, you can change the properties of the modpack
using the [`pakku set`](pakku-set.md) command.

</step>
</procedure>

<procedure title="Importing an existing modpack" id="importing_an_existing_modpack">
<step>

To import an existing modpack, run the [`pakku import`](pakku-import.md) command with the `<path>` argument:

```
./pakku import <path>
```
{prompt="$"}

</step>
</procedure>
