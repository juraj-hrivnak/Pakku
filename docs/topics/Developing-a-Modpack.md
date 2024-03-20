# Developing a Modpack

To be able to properly run and start working on your modpack, you need to have
[project files](Pakku-Terminology.md#project-file) fetched to your modpack folder.

To do that, run the [`pakku fetch`](pakku-fetch.md) command:

<include from="pakku-fetch.md" element-id="snippet-cmd"></include>

This will: download [project files](Pakku-Terminology.md#project-file),
remove old [project files](Pakku-Terminology.md#project-file) and
copy [project overrides](Pakku-Terminology.md#project-override)
from the [Pakku directory](Pakku-Directory.md) to your modpack folder.