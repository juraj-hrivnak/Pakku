# Developing a Modpack

With Pakku there is more than one modpack development strategy.
<br>There are at least three development strategies which you can implement:

1. Adding (or managing) projects (using the Pakku CLI), and then downloading project files
to your local modpack directory using the [`pakku fetch`](pakku-fetch.md) command.
2. Downloading the project files and adding them to your local modpack directory manually, 
and then synchronizing them using the [`pakku sync`](pakku-sync.md) command.
3. Using a combination of the two.

## First Strategy

To download project files to your local modpack directory, run the [`pakku fetch`](pakku-fetch.md) command:

<var name="params"> </var>
<include from="pakku-fetch.md" element-id="snippet-cmd"></include>

This will: download [project files](Pakku-Terminology.md#project-file),
remove old [project files](Pakku-Terminology.md#project-file) and
copy manual overrides
from the [Pakku directory](Pakku-Directory.md) to your modpack folder.
